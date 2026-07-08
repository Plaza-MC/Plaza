package org.plazamc.server.slime.loader;

import org.jetbrains.annotations.NotNull;
import org.plazamc.api.exceptions.UnknownWorldException;
import org.plazamc.api.world.PlazaWorld;
import org.plazamc.api.world.PlazaWorldLoader;
import org.plazamc.api.world.PlazaWorldPropertyMap;
import org.plazamc.server.slime.PlazaSlimeWorld;
import org.plazamc.server.slime.format.PlazaSlimeSerializer;
import org.plazamc.server.slime.format.reader.PlazaSlimeWorldReaderRegistry;

import java.io.IOException;

/**
 * Slime-specific loader. It extends {@link PlazaWorldLoader} so it can be used
 * through the generic Plaza API while keeping raw byte access available for
 * Slime serialization.
 */
public interface PlazaSlimeLoader extends PlazaWorldLoader {

    /**
     * Read a world's raw serialized data.
     *
     * @param worldName The name of the world.
     * @return The world's data file, contained inside a byte array.
     * @throws UnknownWorldException if the world cannot be found.
     * @throws IOException           if the world could not be obtained.
     */
    byte[] readWorldBytes(String worldName) throws UnknownWorldException, IOException;

    @Override
    @NotNull
    default PlazaWorld readWorld(@NotNull String worldName, boolean readOnly, @NotNull PlazaWorldPropertyMap properties) throws UnknownWorldException, IOException {
        byte[] data = readWorldBytes(worldName);
        PlazaSlimeWorld world = PlazaSlimeWorldReaderRegistry.readWorld(this, worldName, data, (org.plazamc.server.slime.properties.PlazaSlimePropertyMap) properties, readOnly);
        return world;
    }

    @Override
    default void saveWorld(@NotNull PlazaWorld world) throws IOException {
        if (!(world instanceof PlazaSlimeWorld slimeWorld)) {
            throw new IllegalArgumentException("Cannot save a non-Slime world with a Slime loader: " + world.getName());
        }
        saveWorld(slimeWorld.getName(), PlazaSlimeSerializer.serialize(slimeWorld));
    }

    /**
     * Saves the world's raw serialized data.
     *
     * @param worldName       The name of the world.
     * @param serializedWorld The world's data file, contained inside a byte array.
     * @throws IOException if the world could not be saved.
     */
    void saveWorld(String worldName, byte[] serializedWorld) throws IOException;

    @Override
    @NotNull
    default String getName() {
        return "slime";
    }
}
