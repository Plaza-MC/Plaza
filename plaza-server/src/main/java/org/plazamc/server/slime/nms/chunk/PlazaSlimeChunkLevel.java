package org.plazamc.server.slime.nms.chunk;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelChunkTicks;
import org.jspecify.annotations.Nullable;
import org.plazamc.server.slime.PlazaSlimeChunk;
import org.plazamc.server.slime.nms.PlazaSlimeInMemoryWorld;
import org.plazamc.server.slime.nms.PlazaSlimeLevelInstance;

public final class PlazaSlimeChunkLevel extends LevelChunk {

    private final PlazaSlimeInMemoryWorld inMemoryWorld;
    private final PlazaNMSSlimeChunk nmsSlimeChunk;
    private final @Nullable PlazaSlimeChunk slimeReference;

    public PlazaSlimeChunkLevel(
            PlazaSlimeLevelInstance world,
            @Nullable PlazaSlimeChunk reference,
            ChunkPos pos,
            UpgradeData upgradeData,
            LevelChunkTicks<Block> blockTickScheduler,
            LevelChunkTicks<Fluid> fluidTickScheduler,
            long inhabitedTime,
            @Nullable LevelChunkSection[] sectionArrayInitializer,
            LevelChunk.PostLoadProcessor entityLoader,
            BlendingData blendingData
    ) {
        super(world, pos, upgradeData, blockTickScheduler, fluidTickScheduler, inhabitedTime, sectionArrayInitializer, entityLoader, blendingData);
        this.inMemoryWorld = world.slimeInstance;
        this.nmsSlimeChunk = new PlazaNMSSlimeChunk(this, reference);
        this.slimeReference = reference;
    }

    @Override
    public void loadCallback() {
        // Not the earliest point where we can promote the chunk in storage, but it's the easiest
        // without any further patches, and without causing a potential memory leak. It's also
        // where Bukkit calls its chunk load event, so we should be fine.
        this.inMemoryWorld.promoteInChunkStorage(this);
        super.loadCallback();
    }

    public PlazaSlimeChunk getSafeSlimeReference() {
        if (this.slimeReference == null) {
            return this.nmsSlimeChunk;
        }
        return new PlazaSafeNmsChunkWrapper(this.nmsSlimeChunk, this.slimeReference);
    }

    public PlazaNMSSlimeChunk getNmsSlimeChunk() {
        return nmsSlimeChunk;
    }
}
