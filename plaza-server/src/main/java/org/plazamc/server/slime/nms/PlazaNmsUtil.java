package org.plazamc.server.slime.nms;

import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Minimal NMS helpers used by the live Slime chunk implementation.
 */
public final class PlazaNmsUtil {

    private PlazaNmsUtil() {
        throw new AssertionError();
    }

    public static long asLong(int chunkX, int chunkZ) {
        return (((long) chunkZ) * Integer.MAX_VALUE + ((long) chunkX));
    }

    public static NewChunkHolder getChunkHolder(LevelChunk chunk) {
        return chunk.level.moonrise$getChunkTaskScheduler().chunkHolderManager.getChunkHolder(chunk.getPos().x, chunk.getPos().z);
    }
}
