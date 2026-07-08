package org.plazamc.server.slime;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jspecify.annotations.Nullable;
import org.plazamc.server.slime.util.PlazaNibbleArray;

public record PlazaSlimeChunkSectionImpl(CompoundBinaryTag blockStatesTag,
                                         CompoundBinaryTag biomeTag,
                                         @Nullable PlazaNibbleArray blockLight,
                                         @Nullable PlazaNibbleArray skyLight) implements PlazaSlimeChunkSection {

    @Override
    public CompoundBinaryTag getBlockStatesTag() {
        return this.blockStatesTag;
    }

    @Override
    public CompoundBinaryTag getBiomeTag() {
        return this.biomeTag;
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
