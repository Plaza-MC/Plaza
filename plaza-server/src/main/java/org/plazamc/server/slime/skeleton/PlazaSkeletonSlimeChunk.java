package org.plazamc.server.slime.skeleton;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.jspecify.annotations.Nullable;
import org.plazamc.server.slime.PlazaSlimeChunk;
import org.plazamc.server.slime.PlazaSlimeChunkSection;

import java.util.List;
import java.util.Map;

public record PlazaSkeletonSlimeChunk(int x,
                                      int z,
                                      PlazaSlimeChunkSection[] sections,
                                      @Nullable CompoundBinaryTag heightMap,
                                      List<CompoundBinaryTag> blockEntities,
                                      List<CompoundBinaryTag> entities,
                                      Map<String, BinaryTag> extra,
                                      @Nullable CompoundBinaryTag upgradeData,
                                      @Nullable CompoundBinaryTag poiChunk,
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
        return this.heightMap;
    }

    @Override
    public List<CompoundBinaryTag> getTileEntities() {
        return this.blockEntities;
    }

    @Override
    public List<CompoundBinaryTag> getEntities() {
        return this.entities;
    }

    @Override
    public Map<String, BinaryTag> getExtraData() {
        return this.extra;
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
        return this.poiChunk;
    }
}
