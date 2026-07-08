package org.plazamc.server.slime.format;

import com.github.luben.zstd.Zstd;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.plazamc.server.slime.PlazaSlimeChunk;
import org.plazamc.server.slime.PlazaSlimeChunkSection;
import org.plazamc.server.slime.PlazaSlimeWorld;
import org.plazamc.server.slime.format.reader.v13.PlazaV13AdditionalWorldData;
import org.plazamc.server.slime.properties.PlazaSlimeProperties;
import org.plazamc.server.slime.properties.PlazaSlimePropertyMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public class PlazaSlimeSerializer {

    private PlazaSlimeSerializer() {
        throw new AssertionError();
    }

    public static byte[] serialize(PlazaSlimeWorld world) {
        Map<String, BinaryTag> extraData = world.getExtraData();
        PlazaSlimePropertyMap propertyMap = world.getPropertyMap();

        if (!extraData.containsKey("properties")) {
            extraData.putIfAbsent("properties", propertyMap.toCompound());
        } else {
            extraData.replace("properties", propertyMap.toCompound());
        }

        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
        DataOutputStream outStream = new DataOutputStream(outByteStream);

        try {
            outStream.write(PlazaSlimeFormat.SLIME_HEADER);
            outStream.writeByte(PlazaSlimeFormat.SLIME_VERSION);
            outStream.writeInt(world.getDataVersion());

            EnumSet<PlazaV13AdditionalWorldData> additionalWorldData = EnumSet.noneOf(PlazaV13AdditionalWorldData.class);
            if (world.getPropertyMap().getValue(PlazaSlimeProperties.SAVE_POI)) {
                additionalWorldData.add(PlazaV13AdditionalWorldData.POI_CHUNKS);
            }
            if (world.getPropertyMap().getValue(PlazaSlimeProperties.SAVE_BLOCK_TICKS)) {
                additionalWorldData.add(PlazaV13AdditionalWorldData.BLOCK_TICKS);
            }
            if (world.getPropertyMap().getValue(PlazaSlimeProperties.SAVE_FLUID_TICKS)) {
                additionalWorldData.add(PlazaV13AdditionalWorldData.FLUID_TICKS);
            }
            outStream.writeByte(PlazaV13AdditionalWorldData.fromSet(additionalWorldData));

            byte[] chunkData = serializeChunks(world, world.getChunkStorage(), additionalWorldData);
            byte[] compressedChunkData = Zstd.compress(chunkData);

            outStream.writeInt(compressedChunkData.length);
            outStream.writeInt(chunkData.length);
            outStream.write(compressedChunkData);

            byte[] extra = serializeCompoundTag(CompoundBinaryTag.builder().put(extraData).build());
            byte[] compressedExtra = Zstd.compress(extra);

            outStream.writeInt(compressedExtra.length);
            outStream.writeInt(extra.length);
            outStream.write(compressedExtra);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return outByteStream.toByteArray();
    }

    static byte[] serializeChunks(PlazaSlimeWorld world, Collection<PlazaSlimeChunk> chunks, EnumSet<PlazaV13AdditionalWorldData> data) throws IOException {
        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream(16384);
        DataOutputStream outStream = new DataOutputStream(outByteStream);

        List<PlazaSlimeChunk> chunksToSave = chunks.stream()
                .filter(chunk -> !PlazaChunkPruner.canBePruned(world, chunk))
                .toList();

        outStream.writeInt(chunksToSave.size());
        for (PlazaSlimeChunk chunk : chunksToSave) {
            outStream.writeInt(chunk.getX());
            outStream.writeInt(chunk.getZ());

            PlazaSlimeChunkSection[] sections = chunk.getSections();

            outStream.writeInt(sections.length);
            for (PlazaSlimeChunkSection slimeChunkSection : sections) {
                byte sectionFlags = 0;

                boolean hasBlockLight = slimeChunkSection.getBlockLight() != null;
                boolean hasSkyLight = slimeChunkSection.getSkyLight() != null;

                if (hasBlockLight) {
                    sectionFlags = (byte) (sectionFlags | 1);
                }
                if (hasSkyLight) {
                    sectionFlags = (byte) (sectionFlags | (1 << 1));
                }
                outStream.write(sectionFlags);

                if (hasBlockLight) {
                    outStream.write(slimeChunkSection.getBlockLight().getBacking());
                }

                if (hasSkyLight) {
                    outStream.write(slimeChunkSection.getSkyLight().getBacking());
                }

                byte[] serializedBlockStates = serializeCompoundTag(slimeChunkSection.getBlockStatesTag());
                outStream.writeInt(serializedBlockStates.length);
                outStream.write(serializedBlockStates);

                byte[] serializedBiomes = serializeCompoundTag(slimeChunkSection.getBiomeTag());
                outStream.writeInt(serializedBiomes.length);
                outStream.write(serializedBiomes);
            }

            byte[] heightMaps = serializeCompoundTag(chunk.getHeightMaps());
            outStream.writeInt(heightMaps.length);
            outStream.write(heightMaps);

            if (data.contains(PlazaV13AdditionalWorldData.POI_CHUNKS)) {
                byte[] poiData = serializeCompoundTag(chunk.getPoiChunkSections());
                outStream.writeInt(poiData.length);
                outStream.write(poiData);
            }

            if (data.contains(PlazaV13AdditionalWorldData.BLOCK_TICKS)) {
                byte[] blockTicksData = serializeCompoundTag(wrap("block_ticks", chunk.getBlockTicks()));
                outStream.writeInt(blockTicksData.length);
                outStream.write(blockTicksData);
            }

            if (data.contains(PlazaV13AdditionalWorldData.FLUID_TICKS)) {
                byte[] fluidTicksData = serializeCompoundTag(wrap("fluid_ticks", chunk.getFluidTicks()));
                outStream.writeInt(fluidTicksData.length);
                outStream.write(fluidTicksData);
            }

            ListBinaryTag tileEntitiesNbtList = ListBinaryTag.listBinaryTag(BinaryTagTypes.COMPOUND, yayGenerics(chunk.getTileEntities()));
            CompoundBinaryTag tileEntitiesCompound = CompoundBinaryTag.builder().put("tileEntities", tileEntitiesNbtList).build();
            byte[] tileEntitiesData = serializeCompoundTag(tileEntitiesCompound);

            outStream.writeInt(tileEntitiesData.length);
            outStream.write(tileEntitiesData);

            ListBinaryTag entitiesNbtList = ListBinaryTag.listBinaryTag(BinaryTagTypes.COMPOUND, yayGenerics(chunk.getEntities()));
            CompoundBinaryTag entitiesCompound = CompoundBinaryTag.builder().put("entities", entitiesNbtList).build();
            byte[] entitiesData = serializeCompoundTag(entitiesCompound);

            outStream.writeInt(entitiesData.length);
            outStream.write(entitiesData);

            byte[] extra = serializeCompoundTag(CompoundBinaryTag.from(chunk.getExtraData()));
            outStream.writeInt(extra.length);
            outStream.write(extra);
        }

        return outByteStream.toByteArray();
    }

    private static CompoundBinaryTag wrap(String key, ListBinaryTag list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return CompoundBinaryTag.builder().put(key, list).build();
    }

    protected static byte[] serializeCompoundTag(CompoundBinaryTag tag) throws IOException {
        if (tag == null || tag.isEmpty()) return new byte[0];

        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
        BinaryTagIO.writer().write(tag, outByteStream);

        return outByteStream.toByteArray();
    }

    public static CompoundBinaryTag deserializeCompoundTag(byte[] data) throws IOException {
        if (data == null || data.length == 0) return CompoundBinaryTag.empty();
        return BinaryTagIO.unlimitedReader().read(new ByteArrayInputStream(data));
    }

    @SuppressWarnings("unchecked")
    private static List<BinaryTag> yayGenerics(final List<? extends BinaryTag> tags) {
        return (List<BinaryTag>) tags;
    }
}
