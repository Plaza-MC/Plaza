package org.plazamc.server.world;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.plazamc.api.events.PlazaWorldLoadEvent;
import org.plazamc.api.events.PlazaWorldSaveEvent;
import org.plazamc.api.events.PlazaWorldUnloadEvent;
import org.plazamc.api.exceptions.InvalidWorldException;
import org.plazamc.api.exceptions.UnknownWorldException;
import org.plazamc.api.exceptions.WorldAlreadyExistsException;
import org.plazamc.api.exceptions.WorldLoadedException;
import org.plazamc.api.world.PlazaWorld;
import org.plazamc.api.world.PlazaWorldFormat;
import org.plazamc.api.world.PlazaWorldInstance;
import org.plazamc.api.world.PlazaWorldLoader;
import org.plazamc.api.world.PlazaWorldPropertyMap;
import org.plazamc.api.world.PlazaWorldPropertyMapImpl;
import org.plazamc.server.PlazaConfig;
import org.plazamc.server.slime.PlazaSlimeWorld;
import org.plazamc.server.slime.format.PlazaSlimeSerializer;
import org.plazamc.server.slime.nms.PlazaSlimeNMSBridge;
import org.plazamc.server.world.loader.PlazaWorldSourceRegistry;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central manager for Plaza worlds. Bridges the generic {@link org.plazamc.api.PlazaAPI}
 * with the Slime (and future format) implementations.
 */
public final class PlazaWorldManager {

    private static final Logger LOGGER = Logger.getLogger("Plaza");
    private static final Map<String, PlazaWorldInstance> LOADED_WORLDS = new ConcurrentHashMap<>();
    private static final java.util.List<String> DEFERRED_FOLDER_WORLDS = new java.util.ArrayList<>();

    private PlazaWorldManager() {
    }

    public static void init() {
        init(null);
    }

    public static void init(String defaultWorldNameToSkip) {
        PlazaWorldSourceRegistry.load();
        loadConfiguredWorlds(defaultWorldNameToSkip);
    }

    public static void reload() {
        PlazaWorldSourceRegistry.reload();
    }

    public static void loadConfiguredWorlds() {
        loadConfiguredWorlds(null);
    }

    public static void loadConfiguredWorlds(String defaultWorldNameToSkip) {
        Section worlds = PlazaConfig.plazaWorldsWorlds();
        for (String worldName : worlds.getRoutesAsStrings(false)) {
            if (defaultWorldNameToSkip != null && defaultWorldNameToSkip.equals(worldName)) {
                // The default world is bootstrapped separately by PlazaSlimeWorldBootstrap.
                continue;
            }

            if (!PlazaConfig.plazaWorldsWorldLoadOnStartup(worldName)) {
                continue;
            }

            String format = PlazaConfig.plazaWorldsWorldFormat(worldName);
            LOGGER.info("Configured world '" + worldName + "' has format: " + format);
            if ("ANVIL".equalsIgnoreCase(format) || "LINEAR".equalsIgnoreCase(format)) {
                // Folder worlds rely on Bukkit.createWorld(), which needs the overworld ready.
                // During early bootstrap it is not available yet, so defer their loading.
                LOGGER.info("Deferring " + format.toUpperCase() + " world '" + worldName + "' until after bootstrap.");
                DEFERRED_FOLDER_WORLDS.add(worldName);
                continue;
            }

            String sourceName = PlazaConfig.plazaWorldsWorldSource(worldName);
            PlazaWorldLoader loader = PlazaWorldSourceRegistry.getLoader(sourceName);
            if (loader == null) {
                LOGGER.warning("Cannot load world '" + worldName + "': source '" + sourceName + "' not found");
                continue;
            }

            try {
                PlazaWorld world = loader.readWorld(worldName, PlazaConfig.plazaWorldsWorldReadOnly(worldName), new org.plazamc.api.world.PlazaWorldPropertyMapImpl());
                loadWorld(world, true);
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Could not load Plaza world " + worldName, ex);
            }
        }
    }

    /**
     * Loads any configured folder worlds (ANVIL or LINEAR) that were deferred
     * during early bootstrap. This must be called once the overworld is fully
     * initialised.
     */
    public static void loadDeferredFolderWorlds() {
        LOGGER.info("Loading deferred folder worlds: " + DEFERRED_FOLDER_WORLDS);
        if (DEFERRED_FOLDER_WORLDS.isEmpty()) {
            return;
        }

        for (final String worldName : DEFERRED_FOLDER_WORLDS) {
            try {
                LOGGER.info("Loading deferred folder world '" + worldName + "'.");
                loadFolderWorld(worldName);
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Could not load deferred folder world " + worldName, ex);
            }
        }
        DEFERRED_FOLDER_WORLDS.clear();
    }

    public static void loadFolderWorld(final String worldName) {
        if (Bukkit.getWorld(worldName) != null) {
            return;
        }

        final PlazaWorldFormat format = PlazaWorldFormat.fromId(PlazaConfig.plazaWorldsWorldFormat(worldName));
        final WorldCreator creator = new WorldCreator(worldName)
            .environment(World.Environment.NORMAL)
            .generator(new org.plazamc.server.generator.PlazaVoidChunkGenerator());
        final World bukkitWorld = creator.createWorld();
        if (bukkitWorld == null) {
            LOGGER.warning("Could not load " + format.getId() + " world '" + worldName + "'");
            return;
        }

        registerLoadedWorld(new PlazaFolderWorld(worldName, false, format), bukkitWorld);
    }

    @NotNull
    public static PlazaWorld readWorld(@NotNull PlazaWorldLoader loader, @NotNull String worldName,
                                       boolean readOnly, @NotNull PlazaWorldPropertyMap properties) throws UnknownWorldException, IOException {
        return loader.readWorld(worldName, readOnly, properties);
    }

    @Nullable
    public static PlazaWorldInstance getLoadedWorld(@NotNull String worldName) {
        return LOADED_WORLDS.get(worldName);
    }

    @NotNull
    public static List<PlazaWorldInstance> getLoadedWorlds() {
        return List.copyOf(LOADED_WORLDS.values());
    }

    @NotNull
    public static PlazaWorldInstance loadWorld(@NotNull PlazaWorld world, boolean callWorldLoadEvent) {
        String worldName = world.getName();
        PlazaWorldInstance existing = LOADED_WORLDS.get(worldName);
        if (existing != null) {
            return existing;
        }

        if (!(world instanceof PlazaSlimeWorld slimeWorld)) {
            throw new UnsupportedOperationException("Only Slime worlds are supported right now");
        }

        PlazaWorldInstance instance = new PlazaSlimeWorldInstance(slimeWorld, callWorldLoadEvent);
        LOADED_WORLDS.put(worldName, instance);

        Bukkit.getPluginManager().callEvent(new PlazaWorldLoadEvent(instance));
        return instance;
    }

    /**
     * Registers a world that has already been loaded into NMS/Bukkit (for example
     * the default overworld or a world created through the Bukkit API) so that
     * Plaza's world manager can track it.
     */
    @NotNull
    public static PlazaWorldInstance registerLoadedWorld(@NotNull PlazaWorld worldData, @NotNull World bukkitWorld) {
        String worldName = worldData.getName();
        PlazaWorldInstance existing = LOADED_WORLDS.get(worldName);
        if (existing != null) {
            return existing;
        }

        PlazaWorldInstance instance;
        if (worldData instanceof PlazaSlimeWorld slimeWorld) {
            instance = new PlazaSlimeWorldInstance(slimeWorld, bukkitWorld);
        } else {
            instance = new PlazaBukkitWorldInstance(worldData, bukkitWorld);
        }
        LOADED_WORLDS.put(worldName, instance);
        Bukkit.getPluginManager().callEvent(new PlazaWorldLoadEvent(instance));
        return instance;
    }

    public static boolean worldLoaded(@NotNull PlazaWorld world) {
        return LOADED_WORLDS.containsKey(world.getName());
    }

    public static void saveWorld(@NotNull PlazaWorld world) throws IOException {
        if (world.isReadOnly()) {
            return;
        }

        if (worldLoaded(world)) {
            World bukkitWorld = Bukkit.getWorld(world.getName());
            if (bukkitWorld != null) {
                bukkitWorld.save();
            }
        }

        PlazaWorldLoader loader = world.getLoader();
        if (loader != null) {
            loader.saveWorld(world);
            Bukkit.getPluginManager().callEvent(new PlazaWorldSaveEvent(world));
        }
    }

    public static boolean unloadWorld(@NotNull String worldName, boolean save) {
        PlazaWorldInstance instance = LOADED_WORLDS.get(worldName);
        if (instance == null) {
            World bukkitWorld = Bukkit.getWorld(worldName);
            if (bukkitWorld == null) {
                return false;
            }
            Bukkit.getPluginManager().callEvent(new PlazaWorldUnloadEvent(null)); // TODO: resolve world data
            return Bukkit.unloadWorld(bukkitWorld, save);
        }

        Bukkit.getPluginManager().callEvent(new PlazaWorldUnloadEvent(instance.getWorldData()));

        if (save) {
            try {
                saveWorld(instance.getWorldData());
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Could not save world " + worldName + " before unloading", ex);
            }
        }

        World bukkitWorld = Bukkit.getWorld(worldName);
        boolean unloaded = bukkitWorld != null && Bukkit.unloadWorld(bukkitWorld, false);
        if (unloaded) {
            LOADED_WORLDS.remove(worldName);
        }
        return unloaded;
    }

    public static void deleteWorld(@NotNull String worldName) throws UnknownWorldException, IOException {
        PlazaWorldInstance instance = LOADED_WORLDS.get(worldName);
        if (instance != null) {
            unloadWorld(worldName, false);
        }

        final String plazaWorldFormat = PlazaConfig.plazaWorldsWorldFormat(worldName);
        if ("ANVIL".equalsIgnoreCase(plazaWorldFormat) || "LINEAR".equalsIgnoreCase(plazaWorldFormat)) {
            // Folder worlds own their vanilla world folder; delete it directly.
            final java.io.File folder = new java.io.File(Bukkit.getWorldContainer(), worldName);
            if (folder.exists()) {
                deleteRecursively(folder);
            }
            PlazaConfig.removePlazaWorld(worldName);
            return;
        }

        // Determine the source from configuration or default to the default source.
        String sourceName = PlazaConfig.plazaWorldsWorldSource(worldName);
        PlazaWorldLoader loader = PlazaWorldSourceRegistry.getLoader(sourceName);
        if (loader == null) {
            throw new IllegalStateException("No loader found for source '" + sourceName + "'");
        }

        loader.deleteWorld(worldName);
        PlazaWorldShadow.delete(worldName);
    }

    private static void deleteRecursively(final java.io.File file) {
        final java.io.File[] children = file.listFiles();
        if (children != null) {
            for (final java.io.File child : children) {
                deleteRecursively(child);
            }
        }
        if (!file.delete()) {
            LOGGER.warning("Could not delete path: " + file);
        }
    }

    @NotNull
    public static PlazaWorld createEmptyWorld(@NotNull String worldName, boolean readOnly,
                                              @NotNull PlazaWorldPropertyMap properties, @Nullable PlazaWorldLoader loader) {
        Objects.requireNonNull(worldName, "World name cannot be null");
        Objects.requireNonNull(properties, "Properties cannot be null");

        org.plazamc.server.slime.loader.PlazaSlimeLoader slimeLoader;
        if (loader instanceof org.plazamc.server.slime.loader.PlazaSlimeLoader sl) {
            slimeLoader = sl;
        } else if (loader == null) {
            slimeLoader = null;
        } else {
            throw new UnsupportedOperationException("Only Slime loaders are supported right now");
        }

        org.plazamc.server.slime.properties.PlazaSlimePropertyMap slimeProperties = properties instanceof org.plazamc.server.slime.properties.PlazaSlimePropertyMap slime
                ? slime
                : new org.plazamc.server.slime.properties.PlazaSlimePropertyMap();
        if (!(properties instanceof org.plazamc.server.slime.properties.PlazaSlimePropertyMap)) {
            slimeProperties.merge(properties);
        }

        return new org.plazamc.server.slime.skeleton.PlazaSkeletonSlimeWorld(
                worldName,
                slimeLoader,
                readOnly,
                new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>(),
                new java.util.concurrent.ConcurrentHashMap<>(),
                slimeProperties,
                net.minecraft.SharedConstants.getCurrentVersion().dataVersion().version()
        );
    }

    public static void migrateWorld(@NotNull String worldName, @NotNull PlazaWorldLoader currentLoader,
                                    @NotNull PlazaWorldLoader newLoader) throws IOException, WorldAlreadyExistsException, UnknownWorldException {
        if (newLoader.worldExists(worldName)) {
            throw new WorldAlreadyExistsException(worldName);
        }

        PlazaWorld world = currentLoader.readWorld(worldName, false, new PlazaWorldPropertyMapImpl());
        newLoader.saveWorld(world);
        currentLoader.deleteWorld(worldName);
    }

    @NotNull
    public static PlazaWorld cloneWorld(@NotNull PlazaWorld source, @NotNull String targetName,
                                        @Nullable PlazaWorldLoader targetLoader) throws WorldAlreadyExistsException, IOException {
        return source.clone(targetName, targetLoader);
    }

    @NotNull
    public static PlazaWorld importVanillaWorld(@NotNull File worldDir, @NotNull String worldName,
                                                @NotNull PlazaWorldLoader loader) throws IOException, WorldAlreadyExistsException, WorldLoadedException, InvalidWorldException {
        if (Bukkit.getWorld(worldName) != null) {
            throw new WorldLoadedException(worldName);
        }
        return org.plazamc.server.world.importexport.PlazaAnvilImporter.importWorld(worldDir, worldName, loader);
    }

    public static void exportWorld(@NotNull PlazaWorld world, @NotNull File worldDir) throws IOException {
        org.plazamc.server.world.importexport.PlazaAnvilExporter.exportWorld(world, worldDir);
    }

    @NotNull
    public static World createBukkitWorld(@NotNull org.bukkit.WorldCreator creator) {
        return org.plazamc.server.slime.PlazaSlimeWorldBootstrap.createOrLoadWorld(creator.name(), creator.environment());
    }

    public static void onWorldUnload(@NotNull String name) {
        LOADED_WORLDS.remove(name);
    }

    private static final class PlazaSlimeWorldInstance implements PlazaWorldInstance {

        private final PlazaSlimeWorld worldData;
        private final World bukkitWorld;

        PlazaSlimeWorldInstance(PlazaSlimeWorld worldData, boolean callWorldLoadEvent) {
            this.worldData = worldData;
            org.plazamc.server.slime.nms.PlazaSlimeInMemoryWorld inMemory = PlazaSlimeNMSBridge.instance().loadInstance(worldData);
            this.bukkitWorld = inMemory.getInstance().getWorld();

            if (callWorldLoadEvent) {
                Bukkit.getPluginManager().callEvent(new org.bukkit.event.world.WorldLoadEvent(this.bukkitWorld));
            }
        }

        PlazaSlimeWorldInstance(PlazaSlimeWorld worldData, World bukkitWorld) {
            this.worldData = worldData;
            this.bukkitWorld = bukkitWorld;
        }

        @Override
        @NotNull
        public PlazaWorld getWorldData() {
            return this.worldData;
        }

        @Override
        @NotNull
        public World getBukkitWorld() {
            return this.bukkitWorld;
        }

        @Override
        public boolean isLoaded() {
            return LOADED_WORLDS.containsKey(this.worldData.getName());
        }
    }

    private static final class PlazaBukkitWorldInstance implements PlazaWorldInstance {

        private final PlazaWorld worldData;
        private final World bukkitWorld;

        PlazaBukkitWorldInstance(PlazaWorld worldData, World bukkitWorld) {
            this.worldData = worldData;
            this.bukkitWorld = bukkitWorld;
        }

        @Override
        @NotNull
        public PlazaWorld getWorldData() {
            return this.worldData;
        }

        @Override
        @NotNull
        public World getBukkitWorld() {
            return this.bukkitWorld;
        }

        @Override
        public boolean isLoaded() {
            return LOADED_WORLDS.containsKey(this.worldData.getName());
        }
    }
}
