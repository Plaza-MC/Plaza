package org.plazamc.server.slime.nms.chunk;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.jspecify.annotations.Nullable;
import org.plazamc.server.slime.PlazaSlimeChunk;
import org.plazamc.server.slime.PlazaSlimeChunkSection;

import java.util.List;
import java.util.Map;

/**
 * Wraps an NMS-backed Slime chunk together with its original Slime skeleton.
 * When the live chunk is not fully loaded yet, reads fall back to the skeleton
 * to avoid serializing partially initialised data.
 */
public final class PlazaSafeNmsChunkWrapper implements PlazaSlimeChunk {

    private final PlazaNMSSlimeChunk wrapper;
    private final PlazaSlimeChunk safety;

    public PlazaSafeNmsChunkWrapper(PlazaNMSSlimeChunk wrapper, PlazaSlimeChunk safety) {
        this.wrapper = wrapper;
        this.safety = safety;
    }

    @Override
    public int getX() {
        return this.wrapper.getX();
    }

    @Override
    public int getZ() {
        return this.wrapper.getZ();
    }

    @Override
    public PlazaSlimeChunkSection[] getSections() {
        return shouldDefaultBackToSlimeChunk() ? this.safety.getSections() : this.wrapper.getSections();
    }

    @Override
    public CompoundBinaryTag getHeightMaps() {
        return shouldDefaultBackToSlimeChunk() ? this.safety.getHeightMaps() : this.wrapper.getHeightMaps();
    }

    @Override
    public List<CompoundBinaryTag> getTileEntities() {
        return shouldDefaultBackToSlimeChunk() ? this.safety.getTileEntities() : this.wrapper.getTileEntities();
    }

    @Override
    public List<CompoundBinaryTag> getEntities() {
        return shouldDefaultBackToSlimeChunk() ? this.safety.getEntities() : this.wrapper.getEntities();
    }

    @Override
    public Map<String, BinaryTag> getExtraData() {
        return shouldDefaultBackToSlimeChunk() ? this.safety.getExtraData() : this.wrapper.getExtraData();
    }

    @Override
    public @Nullable CompoundBinaryTag getUpgradeData() {
        return shouldDefaultBackToSlimeChunk() ? this.safety.getUpgradeData() : this.wrapper.getUpgradeData();
    }

    @Override
    public @Nullable ListBinaryTag getBlockTicks() {
        return shouldDefaultBackToSlimeChunk() ? this.safety.getBlockTicks() : this.wrapper.getBlockTicks();
    }

    @Override
    public @Nullable ListBinaryTag getFluidTicks() {
        return shouldDefaultBackToSlimeChunk() ? this.safety.getFluidTicks() : this.wrapper.getFluidTicks();
    }

    @Override
    public @Nullable CompoundBinaryTag getPoiChunkSections() {
        return shouldDefaultBackToSlimeChunk() ? this.safety.getPoiChunkSections() : this.wrapper.getPoiChunkSections();
    }

    /**
     * Slime chunks can still be requested but not actually loaded. This caused
     * some things to not properly save because they are not "loaded" into the chunk.
     * See ChunkMap#protoChunkToFullChunk: anything in the if statement will not be
     * loaded and is stuck inside the runnable. To avoid corrupting state, simply
     * refer back to the Slime saved object.
     */
    public boolean shouldDefaultBackToSlimeChunk() {
        return this.safety != null && !this.wrapper.getChunk().loaded;
    }

    public PlazaNMSSlimeChunk getWrapper() {
        return wrapper;
    }

    public PlazaSlimeChunk getSafety() {
        return safety;
    }
}
