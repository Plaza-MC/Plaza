package org.plazamc.server.slime;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.persistence.PersistentDataHolder;
import org.plazamc.server.slime.loader.PlazaSlimeLoader;
import org.plazamc.server.slime.properties.PlazaSlimePropertyMap;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory representation of a Slime world.
 */
public interface PlazaSlimeWorld extends PersistentDataHolder {

    /**
     * Returns the name of the world.
     *
     * @return The name of the world.
     */
    String getName();

    /**
     * Returns the {@link PlazaSlimeLoader} used to load and store the world.
     *
     * @return The loader used to load and store the world, or {@code null}.
     */
    PlazaSlimeLoader getLoader();

    /**
     * Returns the chunk that belongs to the coordinates specified.
     *
     * @param x X coordinate.
     * @param z Z coordinate.
     * @return The {@link PlazaSlimeChunk} that belongs to those coordinates.
     */
    PlazaSlimeChunk getChunk(int x, int z);

    /**
     * Returns every loaded chunk.
     *
     * @return A collection with every loaded chunk.
     */
    Collection<PlazaSlimeChunk> getChunkStorage();

    /**
     * Extra data to be stored alongside the world.
     *
     * @return A map containing the extra data of the world.
     */
    ConcurrentMap<String, BinaryTag> getExtraData();

    /**
     * Returns a collection with every world map, serialized in a {@link CompoundBinaryTag} object.
     *
     * @return A collection containing every world map.
     */
    Collection<CompoundBinaryTag> getWorldMaps();

    /**
     * Returns the property map.
     *
     * @return A {@link PlazaSlimePropertyMap} object containing all the properties of the world.
     */
    PlazaSlimePropertyMap getPropertyMap();

    /**
     * Returns whether read-only is enabled.
     *
     * @return {@code true} if read-only is enabled, {@code false} otherwise.
     */
    boolean isReadOnly();

    /**
     * Returns a clone of the world with the given name. This world will never be stored, as the
     * {@code readOnly} property will be set to true.
     *
     * @param worldName The name of the cloned world.
     * @return The clone of the world.
     * @throws IllegalArgumentException if the name of the world is the same as the current one or is {@code null}.
     */
    PlazaSlimeWorld clone(String worldName);

    /**
     * Returns a clone of the world with the given name. The world will be automatically stored inside
     * the provided data source.
     *
     * @param worldName The name of the cloned world.
     * @param loader The {@link PlazaSlimeLoader} used to store the world or {@code null} if the world is temporary.
     * @return The clone of the world.
     * @throws IllegalArgumentException if the name of the world is the same as the current one or is {@code null}.
     * @throws org.plazamc.server.slime.loader.PlazaWorldAlreadyExistsException if there's already a world with the same name inside the provided data source.
     * @throws IOException if the world could not be stored.
     */
    PlazaSlimeWorld clone(String worldName, PlazaSlimeLoader loader) throws org.plazamc.server.slime.loader.PlazaWorldAlreadyExistsException, IOException;

    /**
     * Returns the data version of the world.
     *
     * @return The data version.
     */
    int getDataVersion();
}
