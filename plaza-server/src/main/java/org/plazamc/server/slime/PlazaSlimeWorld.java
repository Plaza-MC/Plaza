package org.plazamc.server.slime;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.plazamc.api.exceptions.WorldAlreadyExistsException;
import org.plazamc.api.world.PlazaWorld;
import org.plazamc.api.world.PlazaWorldFormat;
import org.plazamc.api.world.PlazaWorldLoader;
import org.plazamc.server.slime.loader.PlazaSlimeLoader;
import org.plazamc.server.slime.properties.PlazaSlimePropertyMap;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory representation of a Slime world.
 */
public interface PlazaSlimeWorld extends PlazaWorld {

    @Override
    default PlazaWorldFormat getFormat() {
        return PlazaWorldFormat.SLIME;
    }

    /**
     * Returns the {@link PlazaSlimeLoader} used to load and store the world.
     *
     * @return The loader used to load and store the world, or {@code null}.
     */
    @Override
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
    @Override
    PlazaSlimePropertyMap getPropertyMap();

    @Override
    PlazaSlimeWorld clone(String worldName);

    @Override
    PlazaSlimeWorld clone(String worldName, PlazaWorldLoader loader) throws WorldAlreadyExistsException, IOException;
}
