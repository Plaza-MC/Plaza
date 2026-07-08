package org.plazamc.server.slime.nms.chunk;

import ca.spottedleaf.moonrise.patches.chunk_system.level.entity.ChunkEntitySlices;
import ca.spottedleaf.moonrise.patches.chunk_system.level.poi.PoiChunk;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.LongArrayBinaryTag;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jspecify.annotations.Nullable;
import org.plazamc.server.slime.PlazaSlimeChunk;
import org.plazamc.server.slime.PlazaSlimeChunkSection;
import org.plazamc.server.slime.nms.PlazaNbtConverter;
import org.plazamc.server.slime.nms.PlazaNmsUtil;
import org.plazamc.server.slime.util.PlazaNibbleArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A live {@link LevelChunk} viewed as a {@link PlazaSlimeChunk}.
 */
public final class PlazaNMSSlimeChunk implements PlazaSlimeChunk {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlazaNMSSlimeChunk.class);

    private LevelChunk chunk;
    private final Map<String, BinaryTag> extra;
    private final CompoundBinaryTag upgradeData;

    public PlazaNMSSlimeChunk(LevelChunk chunk, PlazaSlimeChunk reference) {
        this.chunk = chunk;
        this.extra = reference == null ? new HashMap<>() : reference.getExtraData();
        this.upgradeData = reference == null ? null : reference.getUpgradeData();
    }

    public void updatePersistentDataContainer() {
        this.extra.put("ChunkBukkitValues", PlazaNbtConverter.convertTag(chunk.persistentDataContainer.toTagCompound()));
    }

    @Override
    public int getX() {
        return chunk.getPos().x;
    }

    @Override
    public int getZ() {
        return chunk.getPos().z;
    }

    @Override
    public PlazaSlimeChunkSection[] getSections() {
        PlazaSlimeChunkSection[] sections = new PlazaSlimeChunkSection[this.chunk.getSectionsCount()];
        LevelLightEngine lightEngine = chunk.getLevel().getChunkSource().getLightEngine();

        Registry<Biome> biomeRegistry = chunk.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);

        for (int sectionId = 0; sectionId < chunk.getSections().length; sectionId++) {
            LevelChunkSection section = chunk.getSections()[sectionId];

            // Block light nibble array
            PlazaNibbleArray blockLightArray = PlazaNbtConverter.convertArray(
                    lightEngine.getLayerListener(LightLayer.BLOCK).getDataLayerData(SectionPos.of(chunk.getPos(), sectionId)));

            // Sky light nibble array
            PlazaNibbleArray skyLightArray = PlazaNbtConverter.convertArray(
                    lightEngine.getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(chunk.getPos(), sectionId)));

            sections[sectionId] = PlazaSlimeChunkConverter.convertChunkSection(
                    chunk.level.palettedContainerFactory().biomeContainerCodec(),
                    chunk.level.palettedContainerFactory().blockStatesContainerCodec(),
                    section, blockLightArray, skyLightArray);
        }

        return sections;
    }

    @Override
    public @Nullable ListBinaryTag getFluidTicks() {
        return PlazaSlimeChunkConverter.convertSavedFluidTicks(this.chunk.getTicksForSerialization(chunk.level.getGameTime()).fluids());
    }

    @Override
    public @Nullable CompoundBinaryTag getPoiChunkSections() {
        NewChunkHolder chunkHolder = PlazaNmsUtil.getChunkHolder(chunk);
        if (chunkHolder == null) {
            return null;
        }

        PoiChunk slices = chunkHolder.getPoiChunk();
        return getPoiChunkSections(slices);
    }

    public CompoundBinaryTag getPoiChunkSections(PoiChunk poiChunk) {
        return PlazaSlimeChunkConverter.toSlimeSections(poiChunk);
    }

    @Override
    public @Nullable ListBinaryTag getBlockTicks() {
        return PlazaSlimeChunkConverter.convertSavedBlockTicks(this.chunk.getTicksForSerialization(chunk.level.getGameTime()).blocks());
    }

    @Override
    public CompoundBinaryTag getHeightMaps() {
        CompoundBinaryTag.Builder heightMapsTagBuilder = CompoundBinaryTag.builder();

        this.chunk.heightmaps.forEach((type, map) -> {
            if (type.keepAfterWorldgen()) {
                heightMapsTagBuilder.put(type.name(), LongArrayBinaryTag.longArrayBinaryTag(map.getRawData()));
            }
        });

        return heightMapsTagBuilder.build();
    }

    @Override
    public List<CompoundBinaryTag> getTileEntities() {
        Collection<BlockEntity> blockEntities = this.chunk.blockEntities.values();
        List<CompoundBinaryTag> tileEntities = new ArrayList<>(blockEntities.size());

        for (BlockEntity entity : blockEntities) {
            CompoundTag entityNbt = entity.saveWithFullMetadata(net.minecraft.server.MinecraftServer.getServer().registryAccess());
            tileEntities.add(PlazaNbtConverter.convertTag(entityNbt));
        }

        return tileEntities;
    }

    @Override
    public List<CompoundBinaryTag> getEntities() {
        NewChunkHolder chunkHolder = PlazaNmsUtil.getChunkHolder(chunk);
        if (chunkHolder == null) {
            return new ArrayList<>();
        }

        ChunkEntitySlices slices = chunkHolder.getEntityChunk();
        return getEntities(slices);
    }

    public List<CompoundBinaryTag> getEntities(ChunkEntitySlices slices) {
        if (slices == null) {
            return new ArrayList<>();
        }
        List<CompoundBinaryTag> entities = new ArrayList<>(slices.getAllEntities().size());

        try (final ProblemReporter.ScopedCollector scopedCollector =
                     new ProblemReporter.ScopedCollector(ChunkAccess.problemPath(chunk.getPos()), LOGGER)) {
            for (Entity entity : slices.getAllEntities()) {
                try {
                    TagValueOutput tagValueOutput = TagValueOutput.createWithContext(scopedCollector, entity.registryAccess());
                    if (entity.save(tagValueOutput)) {
                        entities.add(PlazaNbtConverter.convertTag(tagValueOutput.buildResult()));
                    }
                } catch (final Exception e) {
                    LOGGER.error("Could not save the entity = {}, exception = {}", entity, e);
                }
            }
        }

        return entities;
    }

    @Override
    public Map<String, BinaryTag> getExtraData() {
        return extra;
    }

    @Override
    public CompoundBinaryTag getUpgradeData() {
        return upgradeData;
    }

    public LevelChunk getChunk() {
        return chunk;
    }

    public void setChunk(LevelChunk chunk) {
        this.chunk = chunk;
    }
}
