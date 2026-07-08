package org.plazamc.server.slime.nms.moonrise;

import ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import org.plazamc.server.slime.PlazaSlimeChunk;
import org.plazamc.server.slime.nms.PlazaSlimeLevelInstance;
import org.plazamc.server.slime.nms.chunk.PlazaSlimeChunkConverter;

import java.io.IOException;

public final class PlazaSlimePoiDataLoader extends MoonriseRegionFileIO.RegionDataController {

    private final PlazaSlimeLevelInstance instance;

    public PlazaSlimePoiDataLoader(PlazaSlimeLevelInstance instance, ChunkTaskScheduler taskScheduler) {
        super(MoonriseRegionFileIO.RegionFileType.POI_DATA, taskScheduler.ioExecutor, taskScheduler.compressionExecutor);
        this.instance = instance;
    }

    @Override
    public RegionFileStorage getCache() {
        return null;
    }

    @Override
    public WriteData startWrite(int chunkX, int chunkZ, CompoundTag compound) throws IOException {
        // Plaza uses its own fast saving path instead of Moonrise's per-chunk POI writing.
        throw new UnsupportedOperationException("Slime worlds must use Plaza's custom save path");
    }

    @Override
    public void finishWrite(int chunkX, int chunkZ, WriteData writeData) throws IOException {
        // No-op: writes are handled by Plaza's custom save path.
    }

    @Override
    public ReadData readData(int chunkX, int chunkZ) throws IOException {
        PlazaSlimeChunk chunk = instance.slimeInstance.getChunk(chunkX, chunkZ);

        if (chunk == null || chunk.getPoiChunkSections() == null) {
            return new ReadData(ReadData.ReadResult.NO_DATA, null, null, 0);
        }

        CompoundTag tag = PlazaSlimeChunkConverter.createPoiChunk(chunk);
        return new ReadData(ReadData.ReadResult.SYNC_READ, null, tag, 0);
    }

    @Override
    public CompoundTag finishRead(int chunkX, int chunkZ, ReadData readData) throws IOException {
        return readData.syncRead();
    }
}
