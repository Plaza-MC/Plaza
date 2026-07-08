package org.plazamc.server.slime.format.reader.v13;

import com.github.luben.zstd.Zstd;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.jspecify.annotations.Nullable;
import org.plazamc.server.slime.PlazaSlimeChunk;
import org.plazamc.server.slime.PlazaSlimeChunkSection;
import org.plazamc.server.slime.PlazaSlimeWorld;
import org.plazamc.server.slime.format.PlazaSlimeSerializer;
import org.plazamc.server.slime.format.reader.PlazaSlimeWorldReader;
import org.plazamc.server.slime.loader.PlazaSlimeLoader;
import org.plazamc.server.slime.properties.PlazaSlimePropertyMap;
import org.plazamc.server.slime.skeleton.PlazaSkeletonSlimeChunk;
import org.plazamc.server.slime.skeleton.PlazaSkeletonSlimeSection;
import org.plazamc.server.slime.skeleton.PlazaSkeletonSlimeWorld;
import org.plazamc.server.slime.util.PlazaNibbleArray;
import org.plazamc.server.slime.util.PlazaSlimeUtil;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlazaV13SlimeWorldDeserializer implements PlazaSlimeWorldReader {

    private static final Logger LOGGER = Logger.getLogger("Plaza-Slime");
    public static final int ARRAY_SIZE = 16 * 16 * 16 / (8 / 4);

    @Override
    public PlazaSlimeWorld deserializeWorld(
            byte version,
            @Nullable PlazaSlimeLoader loader,
            String worldName,
            DataInputStream dataStream,
            PlazaSlimePropertyMap propertyMap,
            boolean readOnly
    ) throws IOException {
        int worldVersion = dataStream.readInt();
        byte additionalWorldData = dataStream.readByte();

        byte[] chunkBytes = readCompressed(dataStream);
        Long2ObjectMap<PlazaSlimeChunk> chunks = readChunks(additionalWorldData, chunkBytes);

        byte[] extraTagBytes = readCompressed(dataStream);
        CompoundBinaryTag extraTag = readCompound(extraTagBytes);

        ConcurrentMap<String, BinaryTag> extraData = new ConcurrentHashMap<>();
        extraTag.forEach(entry -> extraData.put(entry.getKey(), entry.getValue()));

        PlazaSlimePropertyMap worldPropertyMap = propertyMap;
        if (extraData.containsKey("properties")) {
            CompoundBinaryTag serializedSlimeProperties = (CompoundBinaryTag) extraData.get("properties");
            worldPropertyMap = PlazaSlimePropertyMap.fromCompound(serializedSlimeProperties);
            worldPropertyMap.merge(propertyMap);
        }

        return new PlazaSkeletonSlimeWorld(worldName, loader, readOnly, chunks, extraData, worldPropertyMap, worldVersion);
    }

    private static Long2ObjectMap<PlazaSlimeChunk> readChunks(byte additionalWorldData, byte[] chunkBytes) throws IOException {
        Long2ObjectMap<PlazaSlimeChunk> chunkMap = new Long2ObjectOpenHashMap<>();
        DataInputStream chunkData = new DataInputStream(new ByteArrayInputStream(chunkBytes));

        int chunks = chunkData.readInt();
        for (int i = 0; i < chunks; i++) {
            int x = chunkData.readInt();
            int z = chunkData.readInt();

            int sectionCount = chunkData.readInt();
            PlazaSlimeChunkSection[] chunkSections = new PlazaSlimeChunkSection[sectionCount];
            for (int sectionId = 0; sectionId < sectionCount; sectionId++) {
                byte sectionFlags = chunkData.readByte();

                PlazaNibbleArray blockLightArray;
                if ((sectionFlags & 1) == 1) {
                    byte[] blockLightByteArray = new byte[ARRAY_SIZE];
                    chunkData.read(blockLightByteArray);
                    blockLightArray = new PlazaNibbleArray(blockLightByteArray);
                } else {
                    blockLightArray = null;
                }

                PlazaNibbleArray skyLightArray;
                if (((sectionFlags >> 1) & 1) == 1) {
                    byte[] skyLightByteArray = new byte[ARRAY_SIZE];
                    chunkData.read(skyLightByteArray);
                    skyLightArray = new PlazaNibbleArray(skyLightByteArray);
                } else {
                    skyLightArray = null;
                }

                byte[] blockStateData = new byte[chunkData.readInt()];
                chunkData.read(blockStateData);
                CompoundBinaryTag blockStateTag = readCompound(blockStateData);

                byte[] biomeData = new byte[chunkData.readInt()];
                chunkData.read(biomeData);
                CompoundBinaryTag biomeTag = readCompound(biomeData);

                chunkSections[sectionId] = new PlazaSkeletonSlimeSection(blockStateTag, biomeTag, blockLightArray, skyLightArray);
            }

            byte[] heightMapData = new byte[chunkData.readInt()];
            chunkData.read(heightMapData);
            CompoundBinaryTag heightMaps = readCompound(heightMapData);

            CompoundBinaryTag poiChunk = null;
            if (PlazaV13AdditionalWorldData.POI_CHUNKS.isSet(additionalWorldData)) {
                byte[] poiData = new byte[chunkData.readInt()];
                chunkData.read(poiData);
                poiChunk = readCompound(poiData);
            }

            ListBinaryTag blockTicks = null;
            if (PlazaV13AdditionalWorldData.BLOCK_TICKS.isSet(additionalWorldData)) {
                byte[] blockTickData = new byte[chunkData.readInt()];
                chunkData.read(blockTickData);
                CompoundBinaryTag tag = readCompound(blockTickData);
                blockTicks = tag.getList("block_ticks", BinaryTagTypes.COMPOUND);
            }

            ListBinaryTag fluidTicks = null;
            if (PlazaV13AdditionalWorldData.FLUID_TICKS.isSet(additionalWorldData)) {
                byte[] fluidTickData = new byte[chunkData.readInt()];
                chunkData.read(fluidTickData);
                CompoundBinaryTag tag = readCompound(fluidTickData);
                fluidTicks = tag.getList("fluid_ticks", BinaryTagTypes.COMPOUND);
            }

            int countOfUnsupportedData = PlazaV13AdditionalWorldData.countUnsupportedFlags(additionalWorldData);
            for (int i1 = 0; i1 < countOfUnsupportedData; i1++) {
                byte[] randomData = new byte[chunkData.readInt()];
                chunkData.read(randomData);
            }
            if (countOfUnsupportedData > 0) {
                LOGGER.log(Level.WARNING, "Unsupported additional world data found in chunk {0}, {1}. This data will be lost on save.", new Object[] { x, z });
            }

            byte[] tileEntitiesRaw = read(chunkData);
            List<CompoundBinaryTag> tileEntities;
            CompoundBinaryTag tileEntitiesCompound = readCompound(tileEntitiesRaw);
            if (tileEntitiesCompound.isEmpty()) {
                tileEntities = Collections.emptyList();
            } else {
                tileEntities = tileEntitiesCompound.getList("tileEntities", BinaryTagTypes.COMPOUND).stream()
                        .map(tag -> (CompoundBinaryTag) tag)
                        .toList();
            }

            byte[] entitiesRaw = read(chunkData);
            List<CompoundBinaryTag> entities;
            CompoundBinaryTag entitiesCompound = readCompound(entitiesRaw);
            if (entitiesCompound.isEmpty()) {
                entities = Collections.emptyList();
            } else {
                entities = entitiesCompound.getList("entities", BinaryTagTypes.COMPOUND).stream()
                        .map(tag -> (CompoundBinaryTag) tag)
                        .toList();
            }

            byte[] rawExtra = read(chunkData);
            CompoundBinaryTag extra = readCompound(rawExtra);

            Map<String, BinaryTag> extraData = new HashMap<>();
            extra.forEach(entry -> extraData.put(entry.getKey(), entry.getValue()));

            chunkMap.put(PlazaSlimeUtil.chunkPosition(x, z), new PlazaSkeletonSlimeChunk(x, z, chunkSections, heightMaps, tileEntities, entities, extraData, null, poiChunk, blockTicks, fluidTicks));
        }
        return chunkMap;
    }

    private static byte[] readCompressed(DataInputStream stream) throws IOException {
        int compressedLength = stream.readInt();
        int decompressedLength = stream.readInt();
        byte[] compressedData = new byte[compressedLength];
        byte[] decompressedData = new byte[decompressedLength];
        stream.read(compressedData);
        Zstd.decompress(decompressedData, compressedData);
        return decompressedData;
    }

    private static byte[] read(DataInputStream stream) throws IOException {
        int length = stream.readInt();
        byte[] data = new byte[length];
        stream.read(data);
        return data;
    }

    private static CompoundBinaryTag readCompound(byte[] tagBytes) throws IOException {
        if (tagBytes.length == 0) return CompoundBinaryTag.empty();
        return BinaryTagIO.unlimitedReader().read(new ByteArrayInputStream(tagBytes));
    }
}
