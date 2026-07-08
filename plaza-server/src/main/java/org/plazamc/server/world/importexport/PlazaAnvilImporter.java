package org.plazamc.server.world.importexport;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.jetbrains.annotations.Nullable;
import org.plazamc.api.exceptions.InvalidWorldException;
import org.plazamc.api.exceptions.WorldAlreadyExistsException;
import org.plazamc.api.exceptions.WorldLoadedException;
import org.plazamc.api.world.PlazaWorldLoader;
import org.plazamc.server.slime.PlazaSlimeChunk;
import org.plazamc.server.slime.PlazaSlimeChunkSection;
import org.plazamc.server.slime.PlazaSlimeWorld;
import org.plazamc.server.slime.loader.PlazaSlimeLoader;
import org.plazamc.server.slime.properties.PlazaSlimeProperties;
import org.plazamc.server.slime.properties.PlazaSlimePropertyMap;
import org.plazamc.server.slime.skeleton.PlazaSkeletonSlimeChunk;
import org.plazamc.server.slime.skeleton.PlazaSkeletonSlimeSection;
import org.plazamc.server.slime.skeleton.PlazaSkeletonSlimeWorld;
import org.plazamc.server.slime.util.PlazaNibbleArray;
import org.plazamc.server.slime.util.PlazaSlimeUtil;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Imports vanilla Anvil worlds into Plaza's Slime format.
 */
public final class PlazaAnvilImporter {

    private static final int SECTOR_SIZE = 4096;
    private static final Logger LOGGER = Logger.getLogger("Plaza");

    private PlazaAnvilImporter() {
        throw new AssertionError();
    }

    public static PlazaSlimeWorld importWorld(File worldDir, String newName, @Nullable PlazaWorldLoader loader)
            throws InvalidWorldException, WorldLoadedException, IOException, WorldAlreadyExistsException {
        Objects.requireNonNull(worldDir, "World directory cannot be null");
        Objects.requireNonNull(newName, "World name cannot be null");

        if (loader != null && loader.worldExists(newName)) {
            throw new WorldAlreadyExistsException(newName);
        }

        PlazaSlimeLoader slimeLoader;
        if (loader instanceof PlazaSlimeLoader sl) {
            slimeLoader = sl;
        } else if (loader == null) {
            slimeLoader = null;
        } else {
            throw new IllegalArgumentException("Anvil worlds can only be imported into Slime loaders");
        }

        Path worldPath = worldDir.toPath();
        Path levelFile = worldPath.resolve("level.dat");
        if (!Files.exists(levelFile) || !Files.isRegularFile(levelFile)) {
            throw new InvalidWorldException(worldPath.toString());
        }

        LevelData data = readLevelData(levelFile);
        int worldVersion = data.version;

        PlazaSlimePropertyMap propertyMap = new PlazaSlimePropertyMap();
        Path environmentDir = resolveEnvironment(worldPath, propertyMap);
        Path regionDir = environmentDir.resolve("region");

        Long2ObjectMap<PlazaSlimeChunk> chunks = new Long2ObjectOpenHashMap<>();
        try (var stream = Files.newDirectoryStream(regionDir, path -> path.toString().endsWith(".mca"))) {
            for (Path path : stream) {
                LOGGER.info("Loading region file " + path.getFileName() + "...");
                chunks.putAll(loadChunks(path, worldVersion, propertyMap).stream()
                        .collect(Collectors.toMap(chunk -> PlazaSlimeUtil.chunkPosition(chunk.getX(), chunk.getZ()), Function.identity())));
            }
        }

        Path entityDir = environmentDir.resolve("entities");
        if (Files.exists(entityDir) && Files.isDirectory(entityDir)) {
            try (var stream = Files.newDirectoryStream(entityDir, path -> path.toString().endsWith(".mca"))) {
                for (Path path : stream) {
                    LOGGER.info("Loading entity region file " + path.getFileName() + "...");
                    loadEntities(path, worldVersion, chunks);
                }
            }
        }

        if (chunks.isEmpty()) {
            throw new InvalidWorldException(environmentDir.toString());
        }

        propertyMap.setValue(PlazaSlimeProperties.SPAWN_X, data.x);
        propertyMap.setValue(PlazaSlimeProperties.SPAWN_Y, data.y);
        propertyMap.setValue(PlazaSlimeProperties.SPAWN_Z, data.z);

        return new PlazaSkeletonSlimeWorld(
                newName,
                slimeLoader,
                slimeLoader == null,
                chunks,
                new ConcurrentHashMap<>(),
                propertyMap,
                worldVersion
        );
    }

    private static Path resolveEnvironment(Path worldDir, PlazaSlimePropertyMap propertyMap) throws InvalidWorldException {
        propertyMap.setValue(PlazaSlimeProperties.ENVIRONMENT, "normal");
        if (doesWorldContainRegion(worldDir)) {
            return worldDir;
        }

        Path nether = worldDir.resolve("DIM-1");
        propertyMap.setValue(PlazaSlimeProperties.ENVIRONMENT, "nether");
        if (doesWorldContainRegion(nether)) {
            return nether;
        }

        Path end = worldDir.resolve("DIM1");
        propertyMap.setValue(PlazaSlimeProperties.ENVIRONMENT, "the_end");
        if (doesWorldContainRegion(end)) {
            return end;
        }

        throw new InvalidWorldException(worldDir.toString());
    }

    private static boolean doesWorldContainRegion(Path worldDir) {
        Path region = worldDir.resolve("region");
        return Files.exists(worldDir) && Files.isDirectory(worldDir) && Files.exists(region) && Files.isDirectory(region);
    }

    private static LevelData readLevelData(Path file) throws IOException, InvalidWorldException {
        CompoundBinaryTag tag = BinaryTagIO.unlimitedReader().read(file, BinaryTagIO.Compression.GZIP);
        CompoundBinaryTag dataTag = tag.getCompound("Data");
        if (dataTag.size() != 0) {
            int dataVersion = dataTag.getInt("DataVersion", -1);
            int spawnX = dataTag.getInt("SpawnX", 0);
            int spawnY = dataTag.getInt("SpawnY", 255);
            int spawnZ = dataTag.getInt("SpawnZ", 0);
            return new LevelData(dataVersion, spawnX, spawnY, spawnZ);
        }
        throw new InvalidWorldException(file.getParent().toString());
    }

    private static void loadEntities(Path path, int version, Long2ObjectMap<PlazaSlimeChunk> chunkMap) throws IOException {
        byte[] regionBytes = Files.readAllBytes(path);
        if (regionBytes.length == 0) {
            return;
        }

        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(regionBytes));
        List<ChunkEntry> chunks = new ArrayList<>(1024);
        for (int i = 0; i < 1024; i++) {
            int entry = inputStream.readInt();
            int chunkOffset = entry >>> 8;
            int chunkSize = entry & 15;
            if (entry != 0) {
                chunks.add(new ChunkEntry(chunkOffset * SECTOR_SIZE, chunkSize * SECTOR_SIZE));
            }
        }

        for (ChunkEntry entry : chunks) {
            try {
                DataInputStream headerStream = new DataInputStream(new ByteArrayInputStream(regionBytes, entry.offset(), entry.paddedSize()));
                int chunkSize = headerStream.readInt() - 1;
                int compressionScheme = headerStream.readByte();
                DataInputStream chunkStream = new DataInputStream(new ByteArrayInputStream(regionBytes, entry.offset() + 5, chunkSize));
                InputStream decompressorStream = compressionScheme == 1 ? new GZIPInputStream(chunkStream) : new InflaterInputStream(chunkStream);
                CompoundBinaryTag tag = BinaryTagIO.unlimitedReader().read(decompressorStream);
                readEntityChunk(tag, version, chunkMap);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static List<PlazaSlimeChunk> loadChunks(Path path, int worldVersion, PlazaSlimePropertyMap propertyMap) throws IOException {
        byte[] regionBytes = Files.readAllBytes(path);
        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(regionBytes));

        List<ChunkEntry> chunks = new ArrayList<>(1024);
        for (int i = 0; i < 1024; i++) {
            int entry = inputStream.readInt();
            int chunkOffset = entry >>> 8;
            int chunkSize = entry & 15;
            if (entry != 0) {
                chunks.add(new ChunkEntry(chunkOffset * SECTOR_SIZE, chunkSize * SECTOR_SIZE));
            }
        }

        int worldHeight;
        int minY;
        String environment = propertyMap.getValue(PlazaSlimeProperties.ENVIRONMENT);
        if ("normal".equals(environment)) {
            worldHeight = 384;
            minY = -64;
        } else if ("nether".equals(environment) || "the_end".equals(environment)) {
            worldHeight = 256;
            minY = 0;
        } else {
            throw new IllegalStateException("Unsupported environment, cannot obtain world height data");
        }

        int minSectionY = minY >> 4;
        int maxSectionY = (minY + worldHeight - 1) >> 4;

        return chunks.stream().map(entry -> {
            try {
                DataInputStream headerStream = new DataInputStream(new ByteArrayInputStream(regionBytes, entry.offset(), entry.paddedSize()));
                int chunkSize = headerStream.readInt() - 1;
                int compressionScheme = headerStream.readByte();
                DataInputStream chunkStream = new DataInputStream(new ByteArrayInputStream(regionBytes, entry.offset() + 5, chunkSize));
                InputStream decompressorStream = compressionScheme == 1 ? new GZIPInputStream(chunkStream) : new InflaterInputStream(chunkStream);
                CompoundBinaryTag tag = BinaryTagIO.unlimitedReader().read(decompressorStream);
                return readChunk(tag, worldVersion, minSectionY, maxSectionY);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private static void readEntityChunk(CompoundBinaryTag compound, int worldVersion, Long2ObjectMap<PlazaSlimeChunk> chunkMap) {
        int[] position = compound.getIntArray("Position");
        if (position.length == 0) {
            throw new IllegalStateException("Entity chunk is missing position data");
        }
        int chunkX = position[0];
        int chunkZ = position[1];

        int dataVersion = compound.getInt("DataVersion", -1);
        if (dataVersion != worldVersion) {
            LOGGER.log(Level.WARNING, "Cannot load entity chunk at {0},{1}: data version {2} does not match world version {3}",
                    new Object[]{chunkX, chunkZ, dataVersion, worldVersion});
            return;
        }

        PlazaSlimeChunk chunk = chunkMap.get(PlazaSlimeUtil.chunkPosition(chunkX, chunkZ));
        if (chunk == null) {
            LOGGER.warning("Lost entity chunk data at: " + chunkX + "," + chunkZ);
            return;
        }

        List<CompoundBinaryTag> entities = new ArrayList<>(chunk.getEntities());
        for (BinaryTag binaryTag : compound.getList("Entities", BinaryTagTypes.COMPOUND)) {
            entities.add((CompoundBinaryTag) binaryTag);
        }

        chunkMap.put(PlazaSlimeUtil.chunkPosition(chunkX, chunkZ), new PlazaSkeletonSlimeChunk(
                chunk.getX(),
                chunk.getZ(),
                chunk.getSections(),
                chunk.getHeightMaps(),
                chunk.getTileEntities(),
                entities,
                chunk.getExtraData(),
                chunk.getUpgradeData(),
                chunk.getPoiChunkSections(),
                chunk.getBlockTicks(),
                chunk.getFluidTicks()
        ));
    }

    private static PlazaSlimeChunk readChunk(CompoundBinaryTag compound, int worldVersion, int minSectionY, int maxSectionY) {
        int chunkX = compound.getInt("xPos");
        int chunkZ = compound.getInt("zPos");

        int dataVersion = compound.getInt("DataVersion", -1);
        if (dataVersion != worldVersion) {
            LOGGER.log(Level.WARNING, "Cannot load chunk at {0},{1}: data version {2} does not match world version {3}",
                    new Object[]{chunkX, chunkZ, dataVersion, worldVersion});
            return null;
        }

        String status = compound.getString("Status", "");
        if (!status.isEmpty() && !status.equals("postprocessed") && !status.startsWith("full") && !status.startsWith("minecraft:full")) {
            return null;
        }

        CompoundBinaryTag heightMaps = compound.getCompound("Heightmaps");
        List<CompoundBinaryTag> tileEntities = compound.getList("block_entities", BinaryTagTypes.COMPOUND).stream()
                .map(t -> (CompoundBinaryTag) t).collect(Collectors.toList());
        List<CompoundBinaryTag> entities = compound.getList("entities", BinaryTagTypes.COMPOUND).stream()
                .map(t -> (CompoundBinaryTag) t).collect(Collectors.toList());
        ListBinaryTag sectionsTag = compound.getList("sections", BinaryTagTypes.COMPOUND);

        PlazaSlimeChunkSection[] sectionArray = new PlazaSlimeChunkSection[maxSectionY - minSectionY + 1];
        boolean hasSection = false;
        for (BinaryTag rawTag : sectionsTag) {
            CompoundBinaryTag sectionTag = (CompoundBinaryTag) rawTag;
            int index = sectionTag.getByte("Y");
            if (index < minSectionY || index > maxSectionY) {
                continue;
            }

            CompoundBinaryTag blockStatesTag = sectionTag.getCompound("block_states");
            CompoundBinaryTag biomesTag = sectionTag.getCompound("biomes");
            if (blockStatesTag.isEmpty() && biomesTag.isEmpty()) {
                continue;
            }

            PlazaNibbleArray blockLightArray = byteArrayToNibble(sectionTag.getByteArray("BlockLight"));
            PlazaNibbleArray skyLightArray = byteArrayToNibble(sectionTag.getByteArray("SkyLight"));

            sectionArray[index - minSectionY] = new PlazaSkeletonSlimeSection(blockStatesTag, biomesTag, blockLightArray, skyLightArray);
            hasSection = true;
        }

        Map<String, BinaryTag> extraTag = new HashMap<>();
        CompoundBinaryTag chunkBukkitValues = compound.getCompound("ChunkBukkitValues");
        if (!chunkBukkitValues.isEmpty()) {
            extraTag.put("ChunkBukkitValues", chunkBukkitValues);
        }

        if (!hasSection) {
            return null;
        }

        return new PlazaSkeletonSlimeChunk(chunkX, chunkZ, sectionArray, heightMaps, tileEntities, entities, extraTag,
                null, null, null, null);
    }

    private static @Nullable PlazaNibbleArray byteArrayToNibble(byte[] data) {
        return data.length == 0 ? null : new PlazaNibbleArray(data);
    }

    private record ChunkEntry(int offset, int paddedSize) {}

    private record LevelData(int version, int x, int y, int z) {}
}
