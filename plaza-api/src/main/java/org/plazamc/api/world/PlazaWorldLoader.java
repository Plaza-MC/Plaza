package org.plazamc.api.world;

import org.jetbrains.annotations.NotNull;
import org.plazamc.api.exceptions.UnknownWorldException;
import org.plazamc.api.exceptions.WorldAlreadyExistsException;

import java.io.IOException;
import java.util.List;

/**
 * Loads, saves and deletes Plaza worlds from a data source.
 *
 * <p>Implementations are format-specific: a loader knows how to read and write
 * the world format it was built for (Slime, Linear, Polar, etc.).</p>
 */
public interface PlazaWorldLoader {

    /**
     * Reads a world from the data source.
     *
     * @param worldName  Name of the world.
     * @param readOnly   Whether the world should be read-only.
     * @param properties Properties to apply or override after loading.
     * @return The in-memory world.
     * @throws UnknownWorldException if the world cannot be found.
     * @throws IOException           if the world could not be read.
     */
    @NotNull
    PlazaWorld readWorld(@NotNull String worldName, boolean readOnly, @NotNull PlazaWorldPropertyMap properties) throws UnknownWorldException, IOException;

    /**
     * Checks whether a world exists in the data source.
     *
     * @param worldName Name of the world.
     * @return {@code true} if the world exists.
     * @throws IOException if the check fails.
     */
    boolean worldExists(@NotNull String worldName) throws IOException;

    /**
     * Lists the worlds stored in this data source.
     *
     * @return A list of world names.
     * @throws IOException if the list could not be obtained.
     */
    @NotNull
    List<String> listWorlds() throws IOException;

    /**
     * Saves a world to the data source.
     *
     * @param world The world to save.
     * @throws IOException if the world could not be saved.
     */
    void saveWorld(@NotNull PlazaWorld world) throws IOException;

    /**
     * Deletes a world from the data source.
     *
     * @param worldName Name of the world.
     * @throws UnknownWorldException if the world cannot be found.
     * @throws IOException           if the world could not be deleted.
     */
    void deleteWorld(@NotNull String worldName) throws UnknownWorldException, IOException;

    /**
     * Locks a world so other servers cannot load it at the same time.
     *
     * @param worldName Name of the world.
     * @throws IOException if the lock could not be acquired.
     */
    void lockWorld(@NotNull String worldName) throws IOException;

    /**
     * Releases a previously acquired lock.
     *
     * @param worldName Name of the world.
     * @throws IOException if the lock could not be released.
     */
    void unlockWorld(@NotNull String worldName) throws IOException;

    /**
     * Returns whether a world is currently locked.
     *
     * @param worldName Name of the world.
     * @return {@code true} if the world is locked.
     * @throws IOException if the check fails.
     */
    boolean isWorldLocked(@NotNull String worldName) throws IOException;

    /**
     * Returns the data source name (e.g. {@code "file"}, {@code "mysql"}).
     *
     * @return The source name.
     */
    @NotNull
    String getName();
}
