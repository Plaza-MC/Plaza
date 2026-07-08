package org.plazamc.server.slime.nms;

import ca.spottedleaf.concurrentutil.util.Priority;
import ca.spottedleaf.moonrise.patches.chunk_system.level.entity.ChunkEntitySlices;
import ca.spottedleaf.moonrise.patches.chunk_system.level.poi.PoiChunk;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.task.ChunkLoadTask;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.task.GenericDataLoadTask;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.logging.LogUtils;
import net.kyori.adventure.nbt.BinaryTag;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.storage.CommandStorage;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.validation.DirectoryValidator;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.world.WorldSaveEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.plazamc.server.slime.PlazaSlimeWorld;
import org.plazamc.server.slime.format.PlazaSlimeSerializer;
import org.plazamc.server.slime.nms.moonrise.PlazaChunkDataLoadTask;
import org.plazamc.server.slime.nms.moonrise.PlazaSlimeEntityDataLoader;
import org.plazamc.server.slime.nms.moonrise.PlazaSlimePoiDataLoader;
import org.plazamc.server.slime.properties.PlazaSlimeProperties;
import org.plazamc.server.slime.properties.PlazaSlimePropertyMap;
import org.slf4j.Logger;
import org.spigotmc.AsyncCatcher;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public final class PlazaSlimeLevelInstance extends ServerLevel {

    public static LevelStorageSource CUSTOM_LEVEL_STORAGE;
    private static final Logger LOGGER = LogUtils.getClassLogger();

    static {
        try {
            Path path = Files.createTempDirectory("plaza-slime-" + UUID.randomUUID().toString().substring(0, 5)).toAbsolutePath();
            DirectoryValidator directoryValidator = LevelStorageSource.parseValidator(path.resolve("allowed_symlinks.txt"));
            CUSTOM_LEVEL_STORAGE = new LevelStorageSource(path, path, directoryValidator, DataFixers.getDataFixer());

            FileUtils.forceDeleteOnExit(path.toFile());
        } catch (IOException ex) {
            throw new IllegalStateException("Couldn't create dummy level directory.", ex);
        }
    }

    private static final ExecutorService WORLD_SAVER_SERVICE = Executors.newFixedThreadPool(4,
            new ThreadFactoryBuilder().setNameFormat("Plaza-Slime-Save-Thread #%1$d").build());

    private final Object saveLock = new Object();

    public PlazaSlimeLevelInstance(PlazaSlimeBootstrap slimeBootstrap, PrimaryLevelData primaryLevelData,
                                   ResourceKey<Level> worldKey,
                                   ResourceKey<LevelStem> dimensionKey, LevelStem worldDimension,
                                   org.bukkit.World.Environment environment) throws IOException {
        super(slimeBootstrap,
                MinecraftServer.getServer(),
                MinecraftServer.getServer().executor,
                CUSTOM_LEVEL_STORAGE.createAccess(slimeBootstrap.initial().getName() + UUID.randomUUID(), dimensionKey),
                primaryLevelData,
                worldKey,
                worldDimension,
                false,
                0,
                Collections.emptyList(),
                true,
                null,
                environment,
                null,
                null
        );
        this.slimeInstance = new PlazaSlimeInMemoryWorld(slimeBootstrap, this);

        PlazaSlimePropertyMap propertyMap = slimeBootstrap.initial().getPropertyMap();

        this.serverLevelData.setDifficulty(Difficulty.valueOf(propertyMap.getValue(PlazaSlimeProperties.DIFFICULTY).toUpperCase()));
        this.serverLevelData.setSpawn(
                new LevelData.RespawnData(
                        GlobalPos.of(
                                ResourceKey.create(Registries.DIMENSION, this.dimension().identifier()),
                                new BlockPos(
                                        propertyMap.getValue(PlazaSlimeProperties.SPAWN_X),
                                        propertyMap.getValue(PlazaSlimeProperties.SPAWN_Y),
                                        propertyMap.getValue(PlazaSlimeProperties.SPAWN_Z)
                                )
                        ),
                        Mth.wrapDegrees(propertyMap.getValue(PlazaSlimeProperties.SPAWN_YAW)),
                        Mth.wrapDegrees(0F)
                )
        );

        super.chunkSource.setSpawnSettings(
                propertyMap.getValue(PlazaSlimeProperties.ALLOW_MONSTERS),
                propertyMap.getValue(PlazaSlimeProperties.ALLOW_ANIMALS)
        );

        java.util.concurrent.ConcurrentMap<String, BinaryTag> extraData = this.slimeInstance.getExtraData();
        if (extraData.containsKey("BukkitValues")) {
            getWorld().readBukkitValues(PlazaNbtConverter.convertTag(extraData.get("BukkitValues")));
        }

        propertyMap.getOptionalValue(PlazaSlimeProperties.PVP)
                .ifPresent(val -> getGameRules().set(GameRules.PVP, val, this));

        this.entityDataController = new PlazaSlimeEntityDataLoader(this, this.moonrise$getChunkTaskScheduler());
        this.poiDataController = new PlazaSlimePoiDataLoader(this, this.moonrise$getChunkTaskScheduler());
    }

    @Override
    public @NotNull ChunkGenerator getGenerator(PlazaSlimeBootstrap slimeBootstrap) {
        String biomeStr = slimeBootstrap.initial().getPropertyMap().getValue(PlazaSlimeProperties.DEFAULT_BIOME);
        ResourceKey<Biome> biomeKey = ResourceKey.create(Registries.BIOME, Identifier.parse(biomeStr));
        Holder<Biome> defaultBiome = MinecraftServer.getServer().registryAccess().lookupOrThrow(Registries.BIOME).get(biomeKey).orElseThrow();
        return new PlazaSlimeLevelGenerator(defaultBiome, this);
    }

    @Override
    public void save(@Nullable ProgressListener progressUpdate, boolean forceSave, boolean savingDisabled, boolean close) {
        if (!savingDisabled) {
            save();
        }
    }

    public void unload(@NotNull LevelChunk chunk, ChunkEntitySlices slices, PoiChunk poiChunk) {
        slimeInstance.unload(chunk, slices, poiChunk);
    }

    @Override
    public void saveIncrementally(boolean doFull) {
        if (doFull) {
            save();
        }
    }

    public Future<?> save() {
        AsyncCatcher.catchOp("Plaza slime world save");
        try {
            if (!this.slimeInstance.isReadOnly() && this.slimeInstance.getLoader() != null) {
                Bukkit.getPluginManager().callEvent(new WorldSaveEvent(getWorld()));

                this.serverLevelData.setCustomBossEvents(MinecraftServer.getServer().getCustomBossEvents().save(MinecraftServer.getServer().registryAccess()));

                if (MinecraftServer.getServer().isStopped()) {
                    saveInternal().get();
                } else {
                    return this.saveInternal();
                }
            }
        } catch (Throwable e) {
            LOGGER.error("There was a problem saving the PlazaSlimeLevelInstance {}", serverLevelData.getLevelName(), e);
            return CompletableFuture.failedFuture(e);
        }
        return CompletableFuture.completedFuture(null);
    }

    private Future<?> saveInternal() {
        synchronized (saveLock) {
            PlazaSlimeInMemoryWorld slimeWorld = this.slimeInstance;
            LOGGER.debug("Saving world {}...", this.slimeInstance.getName());
            long start = System.currentTimeMillis();

            PlazaSlimeWorld world = this.slimeInstance.getSerializableCopy();
            return WORLD_SAVER_SERVICE.submit(() -> {
                try {
                    byte[] serializedWorld = PlazaSlimeSerializer.serialize(world);
                    long saveStart = System.currentTimeMillis();
                    slimeWorld.getLoader().saveWorld(slimeWorld.getName(), serializedWorld);
                    LOGGER.debug("World {} serialized in {}ms and saved in {}ms.", slimeWorld.getName(), saveStart - start, System.currentTimeMillis() - saveStart);
                } catch (Exception ex) {
                    LOGGER.error("There was an issue saving world {} asynchronously.", slimeWorld.getName(), ex);
                }
            });
        }
    }

    public PlazaChunkDataLoadTask getLoadTask(ChunkLoadTask task, ChunkTaskScheduler scheduler, ServerLevel world,
                                              int chunkX, int chunkZ, Priority priority,
                                              Consumer<GenericDataLoadTask.TaskResult<ChunkAccess, Throwable>> onRun) {
        return new PlazaChunkDataLoadTask(task, scheduler, world, chunkX, chunkZ, priority, onRun);
    }

    public void deleteTempFiles() {
        WORLD_SAVER_SERVICE.execute(() -> {
            Path path = this.levelStorageAccess.levelDirectory.path();
            try {
                Files.walkFileTree(path, new SimpleFileVisitor<>() {
                    @Override
                    public @NotNull FileVisitResult visitFile(Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                        if (!file.equals(path)) {
                            Files.deleteIfExists(file);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public @NotNull FileVisitResult postVisitDirectory(Path dir, @Nullable IOException exception) throws IOException {
                        if (exception != null) {
                            throw exception;
                        } else {
                            if (dir.equals(levelStorageAccess.levelDirectory.path())) {
                                Files.deleteIfExists(path);
                            }
                            Files.deleteIfExists(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    }
                });
            } catch (IOException e) {
                LOGGER.warn("Unable to delete temp level directory", e);
            }
        });
    }
}
