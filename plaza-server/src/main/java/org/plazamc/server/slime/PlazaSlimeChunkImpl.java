package org.plazamc.server.slime;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

public record PlazaSlimeChunkImpl(int x,
                                  int z,
                                  PlazaSlimeChunkSection[] sections,
                                  @Nullable CompoundBinaryTag heightMaps,
                                  List<CompoundBinaryTag> tileEntities,
                                  List<CompoundBinaryTag> entities,
                                  Map<String, BinaryTag> extraData,
                                  @Nullable CompoundBinaryTag upgradeData,
                                  @Nullable CompoundBinaryTag poiChunkSections,
                                  @Nullable ListBinaryTag blockTicks,
                                  @Nullable ListBinaryTag fluidTicks) implements PlazaSlimeChunk {

    @Override
    public int getX() {
        return this.x;
    }

    @Override
    public int getZ() {
        return this.z;
    }

    @Override
    public PlazaSlimeChunkSection[] getSections() {
        return this.sections;
    }

    @Override
    public @Nullable CompoundBinaryTag getHeightMaps() {
        return this.heightMaps;
    }

    @Override
    public List<CompoundBinaryTag> getTileEntities() {
        return this.tileEntities;
    }

    @Override
    public List<CompoundBinaryTag> getEntities() {
        return this.entities;
    }

    @Override
    public Map<String, BinaryTag> getExtraData() {
        return this.extraData;
    }

    @Override
    public @Nullable CompoundBinaryTag getUpgradeData() {
        return this.upgradeData;
    }

    @Override
    public @Nullable ListBinaryTag getBlockTicks() {
        return this.blockTicks;
    }

    @Override
    public @Nullable ListBinaryTag getFluidTicks() {
        return this.fluidTicks;
    }

    @Override
    public @Nullable CompoundBinaryTag getPoiChunkSections() {
        return this.poiChunkSections;
    }
}
