package org.plazamc.server.slime.nms.chunk;

import ca.spottedleaf.moonrise.patches.chunk_system.level.entity.ChunkEntitySlices;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.plazamc.server.slime.PlazaSlimeWorld;
import org.plazamc.server.slime.nms.PlazaNmsUtil;
import org.plazamc.server.slime.properties.PlazaSlimeProperties;
import org.plazamc.server.slime.properties.PlazaSlimePropertyMap;

public final class PlazaFastChunkPruner {

    private PlazaFastChunkPruner() {
        throw new AssertionError();
    }

    public static boolean canBePruned(PlazaSlimeWorld world, LevelChunk chunk) {
        return canBePruned(world, chunk, null);
    }

    public static boolean canBePruned(PlazaSlimeWorld world, LevelChunk chunk, ChunkEntitySlices slices) {
        NewChunkHolder chunkHolder = PlazaNmsUtil.getChunkHolder(chunk);

        // It's not safe to assume that the chunk can be pruned
        // if there isn't a loaded chunk there.
        if (chunkHolder == null) {
            return false;
        }

        PlazaSlimePropertyMap propertyMap = world.getPropertyMap();
        if (propertyMap.getValue(PlazaSlimeProperties.SHOULD_LIMIT_SAVE)) {
            int minX = propertyMap.getValue(PlazaSlimeProperties.SAVE_MIN_X);
            int maxX = propertyMap.getValue(PlazaSlimeProperties.SAVE_MAX_X);
            int minZ = propertyMap.getValue(PlazaSlimeProperties.SAVE_MIN_Z);
            int maxZ = propertyMap.getValue(PlazaSlimeProperties.SAVE_MAX_Z);

            int chunkX = chunk.locX;
            int chunkZ = chunk.locZ;

            if (chunkX < minX || chunkX > maxX) {
                return true;
            }
            if (chunkZ < minZ || chunkZ > maxZ) {
                return true;
            }
        }

        String pruningSetting = propertyMap.getValue(PlazaSlimeProperties.CHUNK_PRUNING);
        if ("aggressive".equals(pruningSetting)) {
            if (slices == null) {
                // In case no slices were provided, try getting them from the chunk holder.
                slices = chunkHolder.getEntityChunk();
            }

            return chunk.blockEntities.isEmpty() && (slices == null || slices.isEmpty()) && areSectionsEmpty(chunk);
        }

        return false;
    }

    private static boolean areSectionsEmpty(LevelChunk chunk) {
        for (LevelChunkSection section : chunk.getSections()) {
            if (!section.hasOnlyAir()) {
                return false;
            }
        }
        return true;
    }
}
