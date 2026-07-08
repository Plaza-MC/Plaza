package org.plazamc.server.slime;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import org.plazamc.server.PlazaConfig;
import org.plazamc.server.slime.loader.PlazaSlimeLoader;
import org.plazamc.server.slime.loader.PlazaUnknownWorldException;
import org.plazamc.server.slime.loader.file.PlazaFileSlimeLoader;
import org.plazamc.server.slime.nms.PlazaSlimeNMSBridge;
import org.plazamc.server.slime.properties.PlazaSlimeProperties;
import org.plazamc.server.slime.properties.PlazaSlimePropertyMap;
import org.plazamc.server.slime.skeleton.PlazaSkeletonSlimeWorld;

/**
 * Bootstraps default Slime worlds for Plaza's plugin-driven profile.
 */
public final class PlazaSlimeWorldBootstrap {

    private PlazaSlimeWorldBootstrap() {
    }

    public static void bootstrapDefaultWorlds(final MinecraftServer server) {
        if (!PlazaConfig.slimeWorldsEnabled()) {
            return;
        }

        final String levelName = ((DedicatedServer) server).getProperties().levelName;
        final PlazaSlimeLoader loader = createLoader();

        final PlazaSlimeWorld overworld = loadOrCreateDefaultWorld(loader, levelName, org.bukkit.World.Environment.NORMAL);
        final PlazaSlimeWorld nether = PlazaConfig.disableDefaultNether()
            ? null
            : loadOrCreateDefaultWorld(loader, levelName + "_nether", org.bukkit.World.Environment.NETHER);
        final PlazaSlimeWorld end = PlazaConfig.disableDefaultEnd()
            ? null
            : loadOrCreateDefaultWorld(loader, levelName + "_the_end", org.bukkit.World.Environment.THE_END);

        PlazaSlimeNMSBridge.instance().setDefaultWorlds(overworld, nether, end);
    }

    private static PlazaSlimeLoader createLoader() {
        final String storage = PlazaConfig.slimeWorldsStorage();
        if (!"file".equalsIgnoreCase(storage)) {
            throw new UnsupportedOperationException("Slime storage backend '" + storage + "' is not supported yet.");
        }
        return new PlazaFileSlimeLoader(PlazaConfig.slimeWorldsDirectory());
    }

    private static PlazaSlimeWorld loadOrCreateDefaultWorld(final PlazaSlimeLoader loader, final String name,
                                                            final org.bukkit.World.Environment environment) {
        try {
            final byte[] data = loader.readWorld(name);
            return org.plazamc.server.slime.format.reader.PlazaSlimeWorldReaderRegistry.readWorld(loader, name, data, new PlazaSlimePropertyMap(), false);
        } catch (final PlazaUnknownWorldException ex) {
            return createDefaultWorld(loader, name, environment);
        } catch (final IOException ex) {
            throw new RuntimeException("Could not read Slime world " + name, ex);
        }
    }

    private static PlazaSlimeWorld createDefaultWorld(final PlazaSlimeLoader loader, final String name,
                                                      final org.bukkit.World.Environment environment) {
        final PlazaSlimePropertyMap properties = new PlazaSlimePropertyMap();
        properties.setValue(PlazaSlimeProperties.ENVIRONMENT, environment.name().toLowerCase());
        properties.setValue(PlazaSlimeProperties.DIFFICULTY, "peaceful");
        properties.setValue(PlazaSlimeProperties.ALLOW_MONSTERS, false);
        properties.setValue(PlazaSlimeProperties.ALLOW_ANIMALS, false);
        properties.setValue(PlazaSlimeProperties.PVP, false);
        properties.setValue(PlazaSlimeProperties.DEFAULT_BIOME, PlazaConfig.slimeDefaultBiome());
        properties.setValue(PlazaSlimeProperties.SAVE_POI, PlazaConfig.slimeSavePoi());
        properties.setValue(PlazaSlimeProperties.SAVE_BLOCK_TICKS, PlazaConfig.slimeSaveBlockTicks());
        properties.setValue(PlazaSlimeProperties.SAVE_FLUID_TICKS, PlazaConfig.slimeSaveFluidTicks());
        properties.setValue(PlazaSlimeProperties.SPAWN_X, 0);
        properties.setValue(PlazaSlimeProperties.SPAWN_Y, 64);
        properties.setValue(PlazaSlimeProperties.SPAWN_Z, 0);

        final PlazaSkeletonSlimeWorld world = new PlazaSkeletonSlimeWorld(
            name,
            loader,
            PlazaConfig.slimeReadOnly(),
            new Long2ObjectOpenHashMap<>(),
            new ConcurrentHashMap<>(),
            properties,
            SharedConstants.getCurrentVersion().dataVersion().version()
        );

        try {
            loader.saveWorld(name, org.plazamc.server.slime.format.PlazaSlimeSerializer.serialize(world));
        } catch (final IOException ex) {
            throw new RuntimeException("Could not save new Slime world " + name, ex);
        }

        return world;
    }
}
