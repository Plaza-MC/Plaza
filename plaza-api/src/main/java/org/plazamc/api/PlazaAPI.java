package org.plazamc.api;

import net.kyori.adventure.util.Services;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.plazamc.api.events.PlazaWorldLoadEvent;
import org.plazamc.api.exceptions.CorruptedWorldException;
import org.plazamc.api.exceptions.UnknownWorldException;
import org.plazamc.api.exceptions.WorldAlreadyExistsException;
import org.plazamc.api.exceptions.WorldLoadedException;
import org.plazamc.api.world.PlazaWorld;
import org.plazamc.api.world.PlazaWorldInstance;
import org.plazamc.api.world.PlazaWorldLoader;
import org.plazamc.api.world.PlazaWorldPropertyMap;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Main entry point for the Plaza API.
 *
 * <p>This API is format-agnostic: it can load and manage Slime worlds today
 * and will support other optimized formats (Linear, Polar, etc.) in the future.</p>
 */
public interface PlazaAPI {

    /**
     * Reads a world from a data source without loading it as a live level.
     *
     * @param loader     The data source loader.
     * @param worldName  The world name.
     * @param readOnly   Whether the world should be read-only.
     * @param properties Properties to apply or override.
     * @return The in-memory world.
     */
    @NotNull
    PlazaWorld readWorld(@NotNull PlazaWorldLoader loader, @NotNull String worldName, boolean readOnly,
                         @NotNull PlazaWorldPropertyMap properties) throws UnknownWorldException, IOException, CorruptedWorldException;

    /**
     * Returns a loaded Plaza world instance by name, or {@code null} if not loaded.
     *
     * @param worldName The world name.
     * @return The loaded instance, or {@code null}.
     */
    @Nullable
    PlazaWorldInstance getLoadedWorld(@NotNull String worldName);

    /**
     * Returns a list of all currently loaded Plaza world instances.
     *
     * @return The loaded instances.
     */
    @NotNull
    List<PlazaWorldInstance> getLoadedWorlds();

    /**
     * Loads a Plaza world as a live server level.
     *
     * <p><b>This method must be called synchronously on the server thread.</b></p>
     *
     * @param world            The world to load.
     * @param callWorldLoadEvent Whether to call {@link org.bukkit.event.world.WorldLoadEvent}.
     * @return The live world instance.
     */
    @NotNull
    PlazaWorldInstance loadWorld(@NotNull PlazaWorld world, boolean callWorldLoadEvent);

    /**
     * Checks whether a Plaza world is currently loaded.
     *
     * @param world The world to check.
     * @return {@code true} if loaded.
     */
    boolean worldLoaded(@NotNull PlazaWorld world);

    /**
     * Saves a world to its configured loader.
     *
     * @param world The world to save.
     * @throws IOException if the world could not be saved.
     */
    void saveWorld(@NotNull PlazaWorld world) throws IOException;

    /**
     * Migrates a world from one data source to another.
     *
     * @param worldName     The world name.
     * @param currentLoader The current loader.
     * @param newLoader     The target loader.
     * @throws IOException             if the migration fails.
     * @throws WorldAlreadyExistsException if the target already contains the world.
     * @throws UnknownWorldException     if the world is missing from the current loader.
     */
    void migrateWorld(@NotNull String worldName, @NotNull PlazaWorldLoader currentLoader,
                      @NotNull PlazaWorldLoader newLoader) throws IOException, WorldAlreadyExistsException, UnknownWorldException;

    /**
     * Creates an empty world without loading or saving it.
     *
     * @param worldName  The world name.
     * @param readOnly   Whether the world should be read-only.
     * @param properties The initial properties.
     * @param loader     The loader to use for persistence, or {@code null} for a temporary world.
     * @return The in-memory world.
     */
    @NotNull
    PlazaWorld createEmptyWorld(@NotNull String worldName, boolean readOnly,
                                @NotNull PlazaWorldPropertyMap properties, @Nullable PlazaWorldLoader loader);

    /**
     * Imports a vanilla Anvil world into a Plaza data source.
     *
     * @param worldDir  The vanilla world folder.
     * @param worldName The name to use in Plaza.
     * @param loader    The target loader.
     * @return The imported world.
     * @throws IOException             if the import fails.
     * @throws WorldAlreadyExistsException if the target already exists.
     * @throws WorldLoadedException      if the vanilla world is currently loaded.
     */
    @NotNull
    PlazaWorld importVanillaWorld(@NotNull File worldDir, @NotNull String worldName,
                                  @NotNull PlazaWorldLoader loader) throws IOException, WorldAlreadyExistsException, WorldLoadedException;

    /**
     * Exports a Plaza world to a vanilla Anvil folder.
     *
     * @param world   The world to export.
     * @param worldDir The target folder.
     * @throws IOException if the export fails.
     */
    void exportWorld(@NotNull PlazaWorld world, @NotNull File worldDir) throws IOException;

    /**
     * Creates or loads a world through the Bukkit {@link WorldCreator} API while
     * respecting Plaza's default format and void generator settings. This is a
     * convenience method for plugins that still use the Bukkit world API.
     *
     * @param creator The Bukkit world creator.
     * @return The Bukkit world.
     */
    @NotNull
    World createBukkitWorld(@NotNull WorldCreator creator);

    /**
     * Returns the API instance.
     *
     * @return The Plaza API instance.
     */
    static PlazaAPI instance() {
        return Holder.INSTANCE;
    }

    @ApiStatus.Internal
    class Holder {
        private static final PlazaAPI INSTANCE = Services.service(PlazaAPI.class)
                .orElseThrow(() -> new IllegalStateException("PlazaAPI service is not available"));
    }
}
