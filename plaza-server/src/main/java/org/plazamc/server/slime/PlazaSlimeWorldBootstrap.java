package org.plazamc.server.slime;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import org.bukkit.World;
import org.plazamc.server.PlazaConfig;
import org.plazamc.server.world.PlazaWorldShadow;
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

        if (!"SLIME".equalsIgnoreCase(PlazaConfig.worldDefaultFormat())) {
            Logger.getLogger("Plaza").warning("Default world format is set to '" + PlazaConfig.worldDefaultFormat()
                + "'; Slime world bootstrap skipped. Only SLIME is supported as default format right now.");
            return;
        }

        final String levelName = ((DedicatedServer) server).getProperties().levelName;
        final PlazaSlimeLoader loader = createLoader();

        // Default overworld: shadow folder is created internally by MinecraftServer,
        // so Plaza does not need to manage it explicitly here.
        final PlazaSlimeWorld overworld = loadOrCreateWorld(loader, levelName, World.Environment.NORMAL, false);
        // Plaza design decision: default Nether and End are always disabled.
        PlazaSlimeNMSBridge.instance().setDefaultWorlds(overworld, null, null);
    }

    /**
     * Creates or loads a Slime world with the given name and environment, then
     * returns its Bukkit {@link World}. This is used for worlds created through
     * the Bukkit API when {@code world.default-format} is {@code SLIME}, and
     * therefore manages the legacy shadow folder.
     */
    public static World createOrLoadWorld(final String name, final World.Environment environment) {
        if (!PlazaConfig.slimeWorldsEnabled()) {
            throw new IllegalStateException("Slime worlds are disabled in plaza.yml");
        }

        final PlazaSlimeLoader loader = createLoader();
        final PlazaSlimeWorld world = loadOrCreateWorld(loader, name, environment, true);
        return PlazaSlimeNMSBridge.instance().loadInstance(world).getInstance().getWorld();
    }

    private static PlazaSlimeLoader createLoader() {
        final String storage = PlazaConfig.slimeWorldsStorage();
        if (!"file".equalsIgnoreCase(storage)) {
            throw new UnsupportedOperationException("Slime storage backend '" + storage + "' is not supported yet.");
        }
        return new PlazaFileSlimeLoader(PlazaConfig.slimeWorldsDirectory());
    }

    public static PlazaSlimeWorld loadOrCreateWorld(final PlazaSlimeLoader loader, final String name,
                                                    final World.Environment environment,
                                                    final boolean createShadow) {
        final boolean shadowExists = PlazaWorldShadow.exists(name);

        try {
            final byte[] data = loader.readWorld(name);

            if (createShadow && !shadowExists) {
                // The shadow folder was deleted by a legacy plugin; treat that as
                // a request to delete the Slime world as well.
                try {
                    loader.deleteWorld(name);
                } catch (final IOException deleteEx) {
                    Logger.getLogger("Plaza").warning("Could not delete Slime world " + name
                        + " after its shadow folder was removed: " + deleteEx.getMessage());
                }
                throw new RuntimeException("Slime world " + name + " has been deleted because its shadow folder "
                    + "is missing. This usually means a legacy plugin removed the world folder.");
            }

            final PlazaSlimeWorld world = org.plazamc.server.slime.format.reader.PlazaSlimeWorldReaderRegistry.readWorld(
                loader, name, data, new PlazaSlimePropertyMap(), false);
            if (createShadow) {
                PlazaWorldShadow.create(name);
            }
            return world;
        } catch (final PlazaUnknownWorldException ex) {
            if (createShadow && shadowExists) {
                // The Slime file is gone but the shadow folder remains; clean up.
                PlazaWorldShadow.delete(name);
            }
            return createDefaultWorld(loader, name, environment, createShadow);
        } catch (final IOException ex) {
            throw new RuntimeException("Could not read Slime world " + name, ex);
        }
    }

    private static PlazaSlimeWorld createDefaultWorld(final PlazaSlimeLoader loader, final String name,
                                                      final World.Environment environment,
                                                      final boolean createShadow) {
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

        if (createShadow) {
            PlazaWorldShadow.create(name);
        }
        return world;
    }
}
