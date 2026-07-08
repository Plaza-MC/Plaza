package org.plazamc.server.slime;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * In-memory representation of a Slime chunk.
 */
public interface PlazaSlimeChunk {

    /**
     * Returns the X coordinate of the chunk.
     *
     * @return X coordinate of the chunk.
     */
    int getX();

    /**
     * Returns the Z coordinate of the chunk.
     *
     * @return Z coordinate of the chunk.
     */
    int getZ();

    /**
     * Returns all the sections of the chunk.
     *
     * @return A {@link PlazaSlimeChunkSection} array.
     */
    PlazaSlimeChunkSection[] getSections();

    /**
     * Returns the height maps of the chunk.
     *
     * @return A {@link CompoundBinaryTag} containing all the height maps of the chunk.
     */
    @Nullable
    CompoundBinaryTag getHeightMaps();

    /**
     * Returns all the tile entities of the chunk.
     *
     * @return A list containing all the tile entities of the chunk.
     */
    List<CompoundBinaryTag> getTileEntities();

    /**
     * Returns all the entities of the chunk.
     *
     * @return A list containing all the entities of the chunk.
     */
    List<CompoundBinaryTag> getEntities();

    /**
     * Returns the extra data of the chunk.
     *
     * @return A map containing the extra data of the chunk as NBT tags.
     */
    Map<String, BinaryTag> getExtraData();

    /**
     * Upgrade data used to fix the chunks. Not intended to be serialized.
     *
     * @return A {@link CompoundBinaryTag} containing the upgrade data of the chunk.
     */
    @Nullable
    CompoundBinaryTag getUpgradeData();

    /**
     * Returns all block ticks of the chunk, if present.
     *
     * @return A {@link ListBinaryTag} containing all the block ticks of the chunk, if present.
     */
    @Nullable
    ListBinaryTag getBlockTicks();

    /**
     * Returns all fluid ticks of the chunk, if present.
     *
     * @return A {@link ListBinaryTag} containing all the fluid ticks of the chunk, if present.
     */
    @Nullable
    ListBinaryTag getFluidTicks();

    /**
     * Returns the POI sections of the chunk, if present.
     *
     * @return A {@link CompoundBinaryTag} containing the POI sections of the chunk, if present.
     */
    @Nullable
    CompoundBinaryTag getPoiChunkSections();
}
