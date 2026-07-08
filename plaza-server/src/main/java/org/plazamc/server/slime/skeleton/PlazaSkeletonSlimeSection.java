package org.plazamc.server.slime.skeleton;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jspecify.annotations.Nullable;
import org.plazamc.server.slime.PlazaSlimeChunkSection;
import org.plazamc.server.slime.util.PlazaNibbleArray;

public record PlazaSkeletonSlimeSection(CompoundBinaryTag blockStates,
                                        CompoundBinaryTag biome,
                                        @Nullable PlazaNibbleArray blockLight,
                                        @Nullable PlazaNibbleArray skyLight) implements PlazaSlimeChunkSection {

    @Override
    public CompoundBinaryTag getBlockStatesTag() {
        return this.blockStates;
    }

    @Override
    public CompoundBinaryTag getBiomeTag() {
        return this.biome;
    }

    @Override
    public @Nullable PlazaNibbleArray getBlockLight() {
        return this.blockLight;
    }

    @Override
    public @Nullable PlazaNibbleArray getSkyLight() {
        return this.skyLight;
    }
}
