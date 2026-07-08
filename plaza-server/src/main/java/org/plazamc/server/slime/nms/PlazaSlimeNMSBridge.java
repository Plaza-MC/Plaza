package org.plazamc.server.slime.nms;

import com.mojang.serialization.Lifecycle;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.CommandStorage;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;
import org.plazamc.server.slime.PlazaSlimeWorld;
import org.plazamc.server.slime.properties.PlazaSlimeProperties;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Locale;

/**
 * Singleton bridge that loads Slime worlds into live NMS ServerLevels.
 */
public final class PlazaSlimeNMSBridge {

    private static final PlazaSlimeNMSBridge INSTANCE = new PlazaSlimeNMSBridge();

    private PlazaSlimeWorld defaultWorld;
    private PlazaSlimeWorld defaultNetherWorld;
    private PlazaSlimeWorld defaultEndWorld;

    private PlazaSlimeNMSBridge() {
    }

    public static PlazaSlimeNMSBridge instance() {
        return INSTANCE;
    }

    public boolean loadOverworldOverride() {
        if (defaultWorld == null) {
            return false;
        }

        PlazaSlimeLevelInstance instance = this.loadInstance(defaultWorld, Level.OVERWORLD).getInstance();
        DimensionDataStorage worldPersistentData = instance.getDataStorage();
        instance.getCraftServer().scoreboardManager = new org.bukkit.craftbukkit.scoreboard.CraftScoreboardManager(instance.getServer(), instance.getScoreboard());
        try {
            Field commandStorageField = MinecraftServer.class.getDeclaredField("commandStorage");
            commandStorageField.setAccessible(true);
            commandStorageField.set(instance.getServer(), new CommandStorage(worldPersistentData));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Could not install command storage for Slime overworld", e);
        }

        return true;
    }

    public boolean loadNetherOverride() {
        if (defaultNetherWorld == null) {
            return false;
        }

        this.loadInstance(defaultNetherWorld, Level.NETHER);
        return true;
    }

    public boolean loadEndOverride() {
        if (defaultEndWorld == null) {
            return false;
        }

        this.loadInstance(defaultEndWorld, Level.END);
        return true;
    }

    /**
     * Sets the default worlds for the server.
     * <b>NOTE: These worlds should be unloaded!</b>
     */
    public void setDefaultWorlds(PlazaSlimeWorld normalWorld, PlazaSlimeWorld netherWorld, PlazaSlimeWorld endWorld) {
        if (normalWorld != null) {
            normalWorld.getPropertyMap().setValue(PlazaSlimeProperties.ENVIRONMENT, World.Environment.NORMAL.toString().toLowerCase());
            defaultWorld = normalWorld;
        }

        if (netherWorld != null) {
            netherWorld.getPropertyMap().setValue(PlazaSlimeProperties.ENVIRONMENT, World.Environment.NETHER.toString().toLowerCase());
            defaultNetherWorld = netherWorld;
        }

        if (endWorld != null) {
            endWorld.getPropertyMap().setValue(PlazaSlimeProperties.ENVIRONMENT, World.Environment.THE_END.toString().toLowerCase());
            defaultEndWorld = endWorld;
        }
    }

    public PlazaSlimeInMemoryWorld loadInstance(PlazaSlimeWorld slimeWorld) {
        return this.loadInstance(slimeWorld, null);
    }

    public PlazaSlimeInMemoryWorld loadInstance(PlazaSlimeWorld slimeWorld, @Nullable ResourceKey<Level> dimensionOverride) {
        String worldName = slimeWorld.getName();

        if (Bukkit.getWorld(worldName) != null) {
            throw new IllegalArgumentException("World " + worldName + " already exists! Maybe it's an outdated SlimeWorld object?");
        }

        PlazaSlimeLevelInstance server = createCustomWorld(slimeWorld, dimensionOverride);
        registerWorld(server);
        return server.slimeInstance;
    }

    public int getCurrentVersion() {
        return SharedConstants.getCurrentVersion().dataVersion().version();
    }

    public void registerWorld(PlazaSlimeLevelInstance server) {
        MinecraftServer mcServer = MinecraftServer.getServer();
        mcServer.initWorld(server, server.serverLevelData, server.serverLevelData.worldGenOptions());
        mcServer.addLevel(server);
    }

    private PlazaSlimeLevelInstance createCustomWorld(PlazaSlimeWorld world, @Nullable ResourceKey<Level> dimensionOverride) {
        PlazaSlimeBootstrap bootstrap = new PlazaSlimeBootstrap(world);
        String worldName = world.getName();

        PrimaryLevelData worldDataServer = createWorldData(world);
        World.Environment environment = getEnvironment(world);
        ResourceKey<LevelStem> dimension = switch (environment) {
            case NORMAL -> LevelStem.OVERWORLD;
            case NETHER -> LevelStem.NETHER;
            case THE_END -> LevelStem.END;
            default -> throw new IllegalArgumentException("Unknown dimension supplied");
        };

        ResourceKey<Level> worldKey = dimensionOverride == null
                ? ResourceKey.create(Registries.DIMENSION, Identifier.parse(worldName.toLowerCase(Locale.ENGLISH)))
                : dimensionOverride;
        LevelStem stem = MinecraftServer.getServer().registries().compositeAccess().lookupOrThrow(Registries.LEVEL_STEM).get(dimension).orElseThrow().value();

        try {
            return new PlazaSlimeLevelInstance(bootstrap, worldDataServer, worldKey, dimension, stem, environment);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private World.Environment getEnvironment(PlazaSlimeWorld world) {
        return World.Environment.valueOf(world.getPropertyMap().getValue(PlazaSlimeProperties.ENVIRONMENT).toUpperCase());
    }

    public PrimaryLevelData createWorldData(PlazaSlimeWorld world) {
        MinecraftServer mcServer = MinecraftServer.getServer();
        DedicatedServerProperties serverProps = ((DedicatedServer) mcServer).getProperties();
        String worldName = world.getName();
        WorldLoader.DataLoadContext context = mcServer.worldLoaderContext;

        LevelSettings worldSettings = new LevelSettings(
                worldName,
                serverProps.gameMode.get(),
                false,
                serverProps.difficulty.get(),
                true,
                new GameRules(context.dataConfiguration().enabledFeatures(), GameRuleMap.of()),
                context.dataConfiguration()
        );

        WorldOptions worldOptions = new WorldOptions(0, false, false);

        PrimaryLevelData data = new PrimaryLevelData(worldSettings, worldOptions, PrimaryLevelData.SpecialWorldProperty.FLAT, Lifecycle.stable());
        data.checkName(worldName);
        data.setModdedInfo(mcServer.getServerModName(), mcServer.getModdedStatus().shouldReportAsModified());
        data.setInitialized(true);

        return data;
    }
}
