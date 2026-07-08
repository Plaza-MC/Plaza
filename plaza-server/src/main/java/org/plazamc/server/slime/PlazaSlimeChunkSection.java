package org.plazamc.server.slime;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jspecify.annotations.Nullable;
import org.plazamc.server.slime.util.PlazaNibbleArray;

/**
 * In-memory representation of a Slime chunk section.
 */
public interface PlazaSlimeChunkSection {

    /**
     * Returns the block states tag.
     *
     * @return A {@link CompoundBinaryTag} containing the block states.
     */
    CompoundBinaryTag getBlockStatesTag();

    /**
     * Returns the biome tag.
     *
     * @return A {@link CompoundBinaryTag} containing the biomes.
     */
    CompoundBinaryTag getBiomeTag();

    /**
     * Returns the block light data.
     *
     * @return A {@link PlazaNibbleArray} with the block light data.
     */
    @Nullable
    PlazaNibbleArray getBlockLight();

    /**
     * Returns the sky light data.
     *
     * @return A {@link PlazaNibbleArray} containing the sky light data.
     */
    @Nullable
    PlazaNibbleArray getSkyLight();
}
