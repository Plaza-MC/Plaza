package org.plazamc.server.linear;

import ca.spottedleaf.moonrise.patches.chunk_system.storage.ChunkSystemRegionFile;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;

/**
 * Common abstraction over the vanilla MCA region file and the Linear region
 * format, so the chunk system can address either implementation.
 *
 * Ported from Luminol/Leaf (abomination/IRegionFile.java).
 * Original license: GPL-3.0-only
 * Original projects:
 *   https://github.com/LuminolMC/Luminol
 *   https://github.com/xymb-endcrystalme/Abomination
 */
public interface IRegionFile extends ChunkSystemRegionFile, AutoCloseable {

    Path getPath();

    DataInputStream getChunkDataInputStream(ChunkPos pos) throws IOException;

    boolean doesChunkExist(ChunkPos pos);

    DataOutputStream getChunkDataOutputStream(ChunkPos pos) throws IOException;

    void flush() throws IOException;

    void clear(ChunkPos pos) throws IOException;

    boolean hasChunk(ChunkPos pos);

    @Override
    void close() throws IOException;

    void write(ChunkPos pos, ByteBuffer buf) throws IOException;

    CompoundTag getOversizedData(int x, int z) throws IOException;

    boolean isOversized(int x, int z);

    boolean recalculateHeader() throws IOException;

    void setOversized(int x, int z, boolean oversized) throws IOException;

    default int getRecalculateCount() {
        return 0;
    }
}
