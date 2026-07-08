package org.plazamc.server.slime.loader;

import java.io.IOException;
import java.util.List;

/**
 * SlimeLoaders are in charge of loading worlds from a data source, and also deleting them.
 */
public interface PlazaSlimeLoader {

    /**
     * Read a world's data file.
     *
     * @param worldName The name of the world.
     * @return The world's data file, contained inside a byte array.
     * @throws PlazaUnknownWorldException if the world cannot be found.
     * @throws IOException if the world could not be obtained.
     */
    byte[] readWorld(String worldName) throws PlazaUnknownWorldException, IOException;

    /**
     * Checks whether a world exists inside the data source.
     *
     * @param worldName The name of the world.
     * @return {@code true} if the world exists inside the data source, {@code false} otherwise.
     * @throws IOException if the world could not be obtained.
     */
    boolean worldExists(String worldName) throws IOException;

    /**
     * Returns the current saved world names.
     *
     * @return A list containing all the world names.
     * @throws IOException if the list could not be obtained.
     */
    List<String> listWorlds() throws IOException;

    /**
     * Saves the world's data file.
     *
     * @param worldName The name of the world.
     * @param serializedWorld The world's data file, contained inside a byte array.
     * @throws IOException if the world could not be saved.
     */
    void saveWorld(String worldName, byte[] serializedWorld) throws IOException;

    /**
     * Deletes a world from the data source.
     *
     * @param worldName name of the world.
     * @throws PlazaUnknownWorldException if the world could not be found.
     * @throws IOException if the world could not be deleted.
     */
    void deleteWorld(String worldName) throws PlazaUnknownWorldException, IOException;
}
