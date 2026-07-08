package org.plazamc.server.world.importexport;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.plazamc.api.world.PlazaWorld;
import org.plazamc.server.slime.PlazaSlimeChunk;
import org.plazamc.server.slime.PlazaSlimeChunkSection;
import org.plazamc.server.slime.PlazaSlimeWorld;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * Exports Plaza Slime worlds to vanilla Anvil region files.
 *
 * <p>This exports every piece of information stored inside the Slime world:
 * chunks, entities, POI, block ticks and fluid ticks. It writes a minimal but
 * valid {@code level.dat}. It does not export player data or vanilla
 * structure files because those are not part of the Slime format.</p>
 */
public final class PlazaAnvilExporter {

    private static final int SECTOR_SIZE = 4096;
    private static final Logger LOGGER = Logger.getLogger("Plaza");

    private PlazaAnvilExporter() {
        throw new AssertionError();
    }

    public static void exportWorld(PlazaWorld world, File worldDir) throws IOException {
        if (!(world instanceof PlazaSlimeWorld slimeWorld)) {
            throw new IllegalArgumentException("Only Slime worlds can be exported to Anvil");
        }

        if (!worldDir.exists() && !worldDir.mkdirs()) {
            throw new IOException("Could not create world directory " + worldDir);
        }

        File regionDir = new File(worldDir, "region");
        File entityDir = new File(worldDir, "entities");
        File poiDir = new File(worldDir, "poi");

        for (File dir : List.of(regionDir, entityDir, poiDir)) {
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("Could not create directory " + dir);
            }
        }

        writeLevelDat(slimeWorld, new File(worldDir, "level.dat"));

        // Group chunks by region
        java.util.Map<Long, List<PlazaSlimeChunk>> regionChunks = new java.util.HashMap<>();
        for (PlazaSlimeChunk chunk : slimeWorld.getChunkStorage()) {
            int regionX = chunk.getX() >> 5;
            int regionZ = chunk.getZ() >> 5;
            regionChunks.computeIfAbsent((((long) regionX) << 32) | (regionZ & 0xFFFFFFFFL), k -> new ArrayList<>()).add(chunk);
        }

        for (java.util.Map.Entry<Long, List<PlazaSlimeChunk>> entry : regionChunks.entrySet()) {
            long key = entry.getKey();
            int regionX = (int) (key >> 32);
            int regionZ = (int) key;
            List<PlazaSlimeChunk> chunks = entry.getValue();

            File regionFile = new File(regionDir, "r." + regionX + "." + regionZ + ".mca");
            writeRegionFile(chunks, regionFile, slimeWorld.getDataVersion());

            List<PlazaSlimeChunk> chunksWithEntities = chunks.stream()
                    .filter(chunk -> chunk.getEntities() != null && !chunk.getEntities().isEmpty())
                    .toList();
            if (!chunksWithEntities.isEmpty()) {
                File entityFile = new File(entityDir, "r." + regionX + "." + regionZ + ".mca");
                writeEntityRegionFile(chunksWithEntities, entityFile, slimeWorld.getDataVersion());
            }

            List<PlazaSlimeChunk> chunksWithPoi = chunks.stream()
                    .filter(chunk -> chunk.getPoiChunkSections() != null && !chunk.getPoiChunkSections().isEmpty())
                    .toList();
            if (!chunksWithPoi.isEmpty()) {
                File poiFile = new File(poiDir, "r." + regionX + "." + regionZ + ".mca");
                writePoiRegionFile(chunksWithPoi, poiFile, slimeWorld.getDataVersion());
            }
        }
    }

    private static void writeLevelDat(PlazaSlimeWorld world, File levelDat) throws IOException {
        CompoundBinaryTag data = CompoundBinaryTag.builder()
                .putInt("DataVersion", world.getDataVersion())
                .putInt("SpawnX", world.getPropertyMap().getInt("spawnX", 0))
                .putInt("SpawnY", world.getPropertyMap().getInt("spawnY", 64))
                .putInt("SpawnZ", world.getPropertyMap().getInt("spawnZ", 0))
                .putString("LevelName", world.getName())
                .build();
        CompoundBinaryTag root = CompoundBinaryTag.builder().put("Data", data).build();

        try (FileOutputStream fos = new FileOutputStream(levelDat)) {
            BinaryTagIO.writer().write(root, fos, BinaryTagIO.Compression.GZIP);
        }
    }

    private static void writeRegionFile(List<PlazaSlimeChunk> chunks, File regionFile, int dataVersion) throws IOException {
        writeRegionFileGeneric(chunks, regionFile, dataVersion, false);
    }

    private static void writeEntityRegionFile(List<PlazaSlimeChunk> chunks, File regionFile, int dataVersion) throws IOException {
        writeRegionFileGeneric(chunks, regionFile, dataVersion, true);
    }

    private static void writePoiRegionFile(List<PlazaSlimeChunk> chunks, File regionFile, int dataVersion) throws IOException {
        writeRegionFileGeneric(chunks, regionFile, dataVersion, false, true);
    }

    private static void writeRegionFileGeneric(List<PlazaSlimeChunk> chunks, File regionFile, int dataVersion, boolean entityFile) throws IOException {
        writeRegionFileGeneric(chunks, regionFile, dataVersion, entityFile, false);
    }

    private static void writeRegionFileGeneric(List<PlazaSlimeChunk> chunks, File regionFile, int dataVersion, boolean entityFile, boolean poiFile) throws IOException {
        int[] offsets = new int[1024];
        int[] timestamps = new int[1024];

        try (ByteArrayOutputStream chunkData = new ByteArrayOutputStream();
             DataOutputStream chunkOut = new DataOutputStream(chunkData)) {

            for (PlazaSlimeChunk chunk : chunks) {
                int localX = chunk.getX() & 31;
                int localZ = chunk.getZ() & 31;
                int index = localX + localZ * 32;

                byte[] serialized;
                if (poiFile) {
                    serialized = serializePoiChunk(chunk, dataVersion);
                } else if (entityFile) {
                    serialized = serializeEntityChunk(chunk, dataVersion);
                } else {
                    serialized = serializeChunk(chunk, dataVersion);
                }

                ByteArrayOutputStream compressed = new ByteArrayOutputStream();
                try (DeflaterOutputStream deflater = new DeflaterOutputStream(compressed, new Deflater())) {
                    deflater.write(serialized);
                }
                byte[] compressedBytes = compressed.toByteArray();

                int sectorPosition = (chunkData.size() / SECTOR_SIZE) + 2; // +2 for header
                offsets[index] = (sectorPosition << 8) | (Math.max(1, (compressedBytes.length + 5 + SECTOR_SIZE - 1) / SECTOR_SIZE) & 0xFF);
                timestamps[index] = (int) (System.currentTimeMillis() / 1000L);

                chunkOut.writeInt(compressedBytes.length + 1);
                chunkOut.writeByte(2); // zlib
                chunkOut.write(compressedBytes);

                // Pad to sector boundary
                int padding = SECTOR_SIZE - ((chunkData.size() + 2 * SECTOR_SIZE) % SECTOR_SIZE);
                if (padding != SECTOR_SIZE) {
                    chunkOut.write(new byte[padding]);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(regionFile);
                 DataOutputStream out = new DataOutputStream(fos)) {
                for (int offset : offsets) {
                    out.writeInt(offset);
                }
                for (int timestamp : timestamps) {
                    out.writeInt(timestamp);
                }
                chunkData.writeTo(out);
            }
        }
    }

    private static byte[] serializeEntityChunk(PlazaSlimeChunk chunk, int dataVersion) throws IOException {
        CompoundBinaryTag tag = CompoundBinaryTag.builder()
                .putIntArray("Position", new int[]{chunk.getX(), chunk.getZ()})
                .putInt("DataVersion", dataVersion)
                .put("Entities", ListBinaryTag.listBinaryTag(BinaryTagTypes.COMPOUND, yayGenerics(chunk.getEntities())))
                .build();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinaryTagIO.writer().write(tag, out);
        return out.toByteArray();
    }

    private static byte[] serializePoiChunk(PlazaSlimeChunk chunk, int dataVersion) throws IOException {
        CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder()
                .putIntArray("Position", new int[]{chunk.getX(), chunk.getZ()})
                .putInt("DataVersion", dataVersion);

        CompoundBinaryTag poi = chunk.getPoiChunkSections();
        if (poi != null) {
            for (java.util.Map.Entry<String, ? extends BinaryTag> entry : poi) {
                builder.put(entry.getKey(), entry.getValue());
            }
        }

        CompoundBinaryTag tag = builder.build();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinaryTagIO.writer().write(tag, out);
        return out.toByteArray();
    }

    private static byte[] serializeChunk(PlazaSlimeChunk chunk, int dataVersion) throws IOException {
        List<BinaryTag> sectionTags = new ArrayList<>();
        int minSectionY = -4; // overworld default
        for (int i = 0; i < chunk.getSections().length; i++) {
            PlazaSlimeChunkSection section = chunk.getSections()[i];
            if (section == null) {
                continue;
            }

            CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder()
                    .putByte("Y", (byte) (i + minSectionY))
                    .put("block_states", section.getBlockStatesTag())
                    .put("biomes", section.getBiomeTag());

            if (section.getBlockLight() != null) {
                builder.putByteArray("BlockLight", section.getBlockLight().getBacking());
            }
            if (section.getSkyLight() != null) {
                builder.putByteArray("SkyLight", section.getSkyLight().getBacking());
            }

            sectionTags.add(builder.build());
        }

        CompoundBinaryTag.Builder chunkBuilder = CompoundBinaryTag.builder()
                .putInt("xPos", chunk.getX())
                .putInt("zPos", chunk.getZ())
                .putInt("DataVersion", dataVersion)
                .putString("Status", "minecraft:full")
                .put("Heightmaps", chunk.getHeightMaps() == null ? CompoundBinaryTag.empty() : chunk.getHeightMaps())
                .put("sections", ListBinaryTag.listBinaryTag(BinaryTagTypes.COMPOUND, yayGenerics(sectionTags)))
                .put("block_entities", ListBinaryTag.listBinaryTag(BinaryTagTypes.COMPOUND, yayGenerics(chunk.getTileEntities())));

        if (chunk.getBlockTicks() != null && !chunk.getBlockTicks().isEmpty()) {
            chunkBuilder.put("block_ticks", chunk.getBlockTicks());
        }
        if (chunk.getFluidTicks() != null && !chunk.getFluidTicks().isEmpty()) {
            chunkBuilder.put("fluid_ticks", chunk.getFluidTicks());
        }

        for (java.util.Map.Entry<String, BinaryTag> extra : chunk.getExtraData().entrySet()) {
            chunkBuilder.put(extra.getKey(), extra.getValue());
        }

        CompoundBinaryTag tag = chunkBuilder.build();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinaryTagIO.writer().write(tag, out);
        return out.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private static List<BinaryTag> yayGenerics(final List<? extends BinaryTag> tags) {
        return (List<BinaryTag>) tags;
    }
}
