package org.plazamc.server.slime.nms;

import ca.spottedleaf.moonrise.patches.chunk_system.level.entity.ChunkEntitySlices;
import ca.spottedleaf.moonrise.patches.chunk_system.level.poi.PoiChunk;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelChunkTicks;
import org.bukkit.persistence.PersistentDataContainer;
import org.plazamc.server.PlazaConfig;
import org.plazamc.server.slime.PlazaSlimeChunk;
import org.plazamc.server.slime.PlazaSlimeWorld;
import org.plazamc.server.slime.format.PlazaSlimeSerializer;
import org.plazamc.server.slime.loader.PlazaSlimeLoader;
import org.plazamc.api.exceptions.WorldAlreadyExistsException;
import org.plazamc.api.world.PlazaWorldLoader;
import org.plazamc.server.slime.nms.chunk.PlazaFastChunkPruner;
import org.plazamc.server.slime.nms.chunk.PlazaNMSSlimeChunk;
import org.plazamc.server.slime.nms.chunk.PlazaPartiallySerializedSlimeChunk;
import org.plazamc.server.slime.nms.chunk.PlazaSafeNmsChunkWrapper;
import org.plazamc.server.slime.nms.chunk.PlazaSlimeChunkConverter;
import org.plazamc.server.slime.nms.chunk.PlazaSlimeChunkLevel;
import org.plazamc.server.slime.pdc.PlazaAdventurePersistentDataContainer;
import org.plazamc.server.slime.properties.PlazaSlimeProperties;
import org.plazamc.server.slime.properties.PlazaSlimePropertyMap;
import org.plazamc.server.slime.skeleton.PlazaSkeletonCloning;
import org.plazamc.server.slime.skeleton.PlazaSkeletonSlimeWorld;
import org.plazamc.server.slime.util.PlazaSlimeUtil;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory live representation of a Slime world.
 *
 * <p>This stores Slime chunks, and when unloaded converts them back to normal Slime chunks
 * for storage. It implements {@link PlazaSlimeWorld} so the rest of the Slime format layer
 * can treat it like any other Slime world.</p>
 */
public final class PlazaSlimeInMemoryWorld implements PlazaSlimeWorld {

    private final PlazaSlimeLevelInstance instance;

    private final ConcurrentMap<String, BinaryTag> extra;
    private final PlazaAdventurePersistentDataContainer extraPdc;
    private final PlazaSlimePropertyMap propertyMap;
    private final PlazaSlimeLoader loader;

    private final Long2ObjectMap<PlazaSlimeChunk> chunkStorage = new Long2ObjectOpenHashMap<>();
    private boolean readOnly;

    public PlazaSlimeInMemoryWorld(PlazaSlimeBootstrap bootstrap, PlazaSlimeLevelInstance instance) {
        this.instance = instance;
        this.extra = bootstrap.initial().getExtraData();
        this.propertyMap = bootstrap.initial().getPropertyMap();
        this.loader = bootstrap.initial().getLoader();
        this.readOnly = bootstrap.initial().isReadOnly();

        for (PlazaSlimeChunk initial : bootstrap.initial().getChunkStorage()) {
            long pos = PlazaSlimeUtil.chunkPosition(initial.getX(), initial.getZ());
            this.chunkStorage.put(pos, initial);
        }

        this.extraPdc = new PlazaAdventurePersistentDataContainer(this.extra);
    }

    @Override
    public String getName() {
        return this.instance.serverLevelData.getLevelName();
    }

    @Override
    public PlazaSlimeLoader getLoader() {
        return this.loader;
    }

    public LevelChunk createChunk(int x, int z, PlazaSlimeChunk chunk) {
        PlazaSlimeChunkLevel levelChunk;
        if (chunk == null) {
            ChunkPos pos = new ChunkPos(x, z);
            LevelChunkTicks<Block> blockLevelChunkTicks = new LevelChunkTicks<>();
            LevelChunkTicks<Fluid> fluidLevelChunkTicks = new LevelChunkTicks<>();

            levelChunk = new PlazaSlimeChunkLevel(
                    this.instance,
                    null,
                    pos,
                    UpgradeData.EMPTY,
                    blockLevelChunkTicks,
                    fluidLevelChunkTicks,
                    0L,
                    null,
                    null,
                    null
            );

            // Make the default biome property work for empty chunks.
            levelChunk.fillBiomesFromNoise(
                    instance.chunkSource.getGenerator().getBiomeSource(),
                    instance.chunkSource.randomState().sampler()
            );

            // Plaza start - spawn platform for default empty Slime worlds
            if (PlazaConfig.spawnPlatformEnabled()) {
                generateSpawnPlatform(levelChunk, x, z);
            }
            // Plaza end - spawn platform for default empty Slime worlds
        } else {
            levelChunk = PlazaSlimeChunkConverter.deserializeSlimeChunk(this.instance, chunk);
        }

        return levelChunk;
    }

    private static void generateSpawnPlatform(final LevelChunk levelChunk, final int chunkX, final int chunkZ) {
        // Platform matches PlazaVoidChunkGenerator: 5x5 glass at y=63 centered on 0,0.
        final int platformMinX = -2;
        final int platformMaxX = 2;
        final int platformMinZ = -2;
        final int platformMaxZ = 2;
        final int platformY = 63;

        final int chunkBlockX = chunkX << 4;
        final int chunkBlockZ = chunkZ << 4;
        final int startX = Math.max(platformMinX, chunkBlockX);
        final int endX = Math.min(platformMaxX, chunkBlockX + 15);
        final int startZ = Math.max(platformMinZ, chunkBlockZ);
        final int endZ = Math.min(platformMaxZ, chunkBlockZ + 15);

        if (startX > endX || startZ > endZ) {
            return;
        }

        final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int x = startX; x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {
                mutable.set(x, platformY, z);
                levelChunk.setBlockState(mutable, Blocks.GLASS.defaultBlockState(), 0);
            }
        }
    }

    /**
     * Unloads a live chunk and converts it back into a Slime skeleton for storage.
     */
    public void unload(LevelChunk providedChunk, ChunkEntitySlices slices, PoiChunk poiChunk) {
        final int x = providedChunk.locX;
        final int z = providedChunk.locZ;

        if (PlazaFastChunkPruner.canBePruned(this, providedChunk, slices)) {
            this.chunkStorage.remove(PlazaSlimeUtil.chunkPosition(x, z));
            return;
        }

        PlazaNMSSlimeChunk chunk;
        if (providedChunk instanceof PlazaSlimeChunkLevel slimeChunkLevel) {
            chunk = slimeChunkLevel.getNmsSlimeChunk();
        } else {
            chunk = new PlazaNMSSlimeChunk(providedChunk, getChunk(x, z));
        }
        chunk.updatePersistentDataContainer();

        ListBinaryTag blockTicks = null;
        ListBinaryTag fluidTicks = null;

        if (getPropertyMap().getValue(PlazaSlimeProperties.SAVE_BLOCK_TICKS)
                || getPropertyMap().getValue(PlazaSlimeProperties.SAVE_FLUID_TICKS)) {
            ChunkAccess.PackedTicks ticksForSerialization = chunk.getChunk().getTicksForSerialization(this.instance.getGameTime());

            if (getPropertyMap().getValue(PlazaSlimeProperties.SAVE_BLOCK_TICKS)) {
                blockTicks = PlazaSlimeChunkConverter.convertSavedBlockTicks(ticksForSerialization.blocks());
            }
            if (getPropertyMap().getValue(PlazaSlimeProperties.SAVE_FLUID_TICKS)) {
                fluidTicks = PlazaSlimeChunkConverter.convertSavedFluidTicks(ticksForSerialization.fluids());
            }
        }

        CompoundBinaryTag poiSections = null;
        if (getPropertyMap().getValue(PlazaSlimeProperties.SAVE_POI)) {
            poiSections = chunk.getPoiChunkSections(poiChunk);
        }

        this.chunkStorage.put(PlazaSlimeUtil.chunkPosition(x, z), new org.plazamc.server.slime.skeleton.PlazaSkeletonSlimeChunk(
                chunk.getX(),
                chunk.getZ(),
                chunk.getSections(),
                chunk.getHeightMaps(),
                chunk.getTileEntities(),
                chunk.getEntities(slices),
                chunk.getExtraData(),
                null,
                poiSections,
                blockTicks,
                fluidTicks
        ));
    }

    @Override
    public PlazaSlimeChunk getChunk(int x, int z) {
        return this.chunkStorage.get(PlazaSlimeUtil.chunkPosition(x, z));
    }

    @Override
    public Collection<PlazaSlimeChunk> getChunkStorage() {
        return this.chunkStorage.values();
    }

    @Override
    public ConcurrentMap<String, BinaryTag> getExtraData() {
        return this.extra;
    }

    @Override
    public Collection<CompoundBinaryTag> getWorldMaps() {
        return List.of();
    }

    @Override
    public PlazaSlimePropertyMap getPropertyMap() {
        return this.propertyMap;
    }

    @Override
    public boolean isReadOnly() {
        return this.getLoader() == null || this.readOnly;
    }

    @Override
    public PlazaSlimeWorld clone(String worldName) {
        try {
            return clone(worldName, null);
        } catch (WorldAlreadyExistsException | IOException ignored) {
            return null;
        }
    }

    @Override
    public PlazaSlimeWorld clone(String worldName, PlazaWorldLoader loader) throws WorldAlreadyExistsException, IOException {
        if (this.getName().equals(worldName)) {
            throw new IllegalArgumentException("The clone world cannot have the same name as the original world!");
        }
        if (worldName == null) {
            throw new IllegalArgumentException("The world name cannot be null!");
        }
        if (!(loader instanceof PlazaSlimeLoader slimeLoader)) {
            throw new IllegalArgumentException("Cannot clone a Slime world into a non-Slime loader");
        }
        if (slimeLoader != null && slimeLoader.worldExists(worldName)) {
            throw new WorldAlreadyExistsException(worldName);
        }

        PlazaSlimeWorld cloned = PlazaSkeletonCloning.fullClone(worldName, this, slimeLoader, false);
        if (slimeLoader != null) {
            slimeLoader.saveWorld(worldName, PlazaSlimeSerializer.serialize(cloned));
        }

        return cloned;
    }

    @Override
    public int getDataVersion() {
        return SharedConstants.getCurrentVersion().dataVersion().version();
    }

    @Override
    public PersistentDataContainer getPersistentDataContainer() {
        return this.extraPdc;
    }

    public PlazaSlimeLevelInstance getInstance() {
        return instance;
    }

    /**
     * Returns a serializable copy of this live world, converting any live NMS chunks
     * back to Slime skeletons on the fly.
     */
    public PlazaSlimeWorld getSerializableCopy() {
        PlazaSlimeWorld world = PlazaSkeletonCloning.weakCopy(this);

        Long2ObjectMap<PlazaSlimeChunk> cloned = new Long2ObjectOpenHashMap<>();
        for (Long2ObjectMap.Entry<PlazaSlimeChunk> entry : this.chunkStorage.long2ObjectEntrySet()) {
            PlazaSlimeChunk clonedChunk = entry.getValue();

            PlazaNMSSlimeChunk chunk = null;
            if (clonedChunk instanceof PlazaSafeNmsChunkWrapper safeNmsChunkWrapper) {
                if (safeNmsChunkWrapper.shouldDefaultBackToSlimeChunk()) {
                    clonedChunk = safeNmsChunkWrapper.getSafety();
                } else {
                    chunk = safeNmsChunkWrapper.getWrapper();
                }
            } else if (clonedChunk instanceof PlazaNMSSlimeChunk nmsSlimeChunk) {
                chunk = nmsSlimeChunk;
            }

            if (chunk != null) {
                if (PlazaFastChunkPruner.canBePruned(world, chunk.getChunk())) {
                    continue;
                }

                clonedChunk = PlazaPartiallySerializedSlimeChunk.of(
                        chunk,
                        getPropertyMap().getValue(PlazaSlimeProperties.SAVE_BLOCK_TICKS),
                        getPropertyMap().getValue(PlazaSlimeProperties.SAVE_FLUID_TICKS),
                        getPropertyMap().getValue(PlazaSlimeProperties.SAVE_POI)
                );
            }

            cloned.put(entry.getLongKey(), clonedChunk);
        }

        // Serialize Bukkit values (PDC)
        CompoundTag nmsTag = new CompoundTag();
        this.instance.getWorld().storeBukkitValues(nmsTag);
        CompoundBinaryTag adventureTag = PlazaNbtConverter.convertTag(nmsTag.getCompoundOrEmpty("BukkitValues"));
        world.getExtraData().put("BukkitValues", adventureTag);

        return new PlazaSkeletonSlimeWorld(
                world.getName(),
                world.getLoader(),
                world.isReadOnly(),
                cloned,
                world.getExtraData(),
                world.getPropertyMap(),
                world.getDataVersion()
        );
    }

    public void promoteInChunkStorage(PlazaSlimeChunkLevel chunk) {
        chunkStorage.put(PlazaSlimeUtil.chunkPosition(chunk.locX, chunk.locZ), chunk.getSafeSlimeReference());
    }
}
