package org.plazamc.server.slime.nms.chunk;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.SavedTick;
import org.jspecify.annotations.Nullable;
import org.plazamc.server.slime.PlazaSlimeChunk;
import org.plazamc.server.slime.PlazaSlimeChunkSection;
import org.plazamc.server.slime.nms.PlazaNbtConverter;
import org.plazamc.server.slime.skeleton.PlazaSkeletonSlimeChunk;
import org.plazamc.server.slime.util.PlazaNibbleArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mimics how {@link net.minecraft.world.level.chunk.storage.SerializableChunkData} creates a copy
 * of the section data to be serialized asynchronously. This avoids serializing the live chunk
 * directly on the save thread.
 */
public record PlazaPartiallySerializedSlimeChunk(
        PalettedContainerFactory containerFactory,
        int x,
        int z,
        PlazaPartiallySerializedSlimeChunkSection[] sections,
        @Nullable CompoundBinaryTag heightMap,
        List<CompoundBinaryTag> blockEntities,
        List<CompoundBinaryTag> entities,
        Map<String, BinaryTag> extra,
        @Nullable CompoundBinaryTag upgradeData,
        @Nullable CompoundBinaryTag poiChunk,
        @Nullable List<SavedTick<Block>> blockTicks,
        @Nullable List<SavedTick<Fluid>> fluidTicks
) implements PlazaSlimeChunk {

    public static PlazaPartiallySerializedSlimeChunk of(PlazaNMSSlimeChunk slimeChunk, boolean saveBlockTicks, boolean saveFluidTicks, boolean savePoi) {
        LevelChunk chunk = slimeChunk.getChunk();

        PlazaPartiallySerializedSlimeChunkSection[] sections = new PlazaPartiallySerializedSlimeChunkSection[chunk.getSectionsCount()];
        LevelLightEngine lightEngine = chunk.getLevel().getChunkSource().getLightEngine();

        for (int sectionId = 0; sectionId < chunk.getSections().length; sectionId++) {
            LevelChunkSection section = chunk.getSections()[sectionId];

            // Block light nibble array
            PlazaNibbleArray blockLightArray = PlazaNbtConverter.convertArray(
                    lightEngine.getLayerListener(LightLayer.BLOCK).getDataLayerData(SectionPos.of(chunk.getPos(), sectionId)));

            // Sky light nibble array
            PlazaNibbleArray skyLightArray = PlazaNbtConverter.convertArray(
                    lightEngine.getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(chunk.getPos(), sectionId)));

            sections[sectionId] = new PlazaPartiallySerializedSlimeChunkSection(section.copy(), blockLightArray, skyLightArray);
        }

        List<SavedTick<Block>> blockTicks = null;
        List<SavedTick<Fluid>> fluidTicks = null;

        if (saveBlockTicks || saveFluidTicks) {
            ChunkAccess.PackedTicks ticksForSerialization = chunk.getTicksForSerialization(chunk.level.getGameTime());

            if (saveBlockTicks) {
                blockTicks = ticksForSerialization.blocks();
            }
            if (saveFluidTicks) {
                fluidTicks = ticksForSerialization.fluids();
            }
        }

        CompoundBinaryTag serializedPoiChunk = savePoi ? slimeChunk.getPoiChunkSections() : null;
        List<CompoundBinaryTag> entities = slimeChunk.getEntities();

        Map<String, BinaryTag> extra = new HashMap<>(slimeChunk.getExtraData());

        // Serialize Bukkit values (PDC)
        CompoundBinaryTag adventureTag = PlazaNbtConverter.convertTag(chunk.persistentDataContainer.toTagCompound());
        extra.put("ChunkBukkitValues", adventureTag);

        return new PlazaPartiallySerializedSlimeChunk(
                chunk.level.palettedContainerFactory(),
                chunk.locX,
                chunk.locZ,
                sections,
                slimeChunk.getHeightMaps(),
                slimeChunk.getTileEntities(),
                entities,
                extra,
                slimeChunk.getUpgradeData(),
                serializedPoiChunk,
                blockTicks,
                fluidTicks
        );
    }

    @Override
    public int getX() {
        return this.x;
    }

    @Override
    public int getZ() {
        return this.z;
    }

    @Override
    public PlazaSlimeChunkSection[] getSections() {
        PlazaSlimeChunkSection[] chunkSections = new PlazaSlimeChunkSection[this.sections.length];

        for (int i = 0; i < this.sections.length; i++) {
            PlazaPartiallySerializedSlimeChunkSection partial = this.sections[i];
            chunkSections[i] = PlazaSlimeChunkConverter.convertChunkSection(
                    containerFactory.biomeContainerCodec(),
                    containerFactory.blockStatesContainerCodec(),
                    partial.section,
                    partial.blockLight,
                    partial.skyLight);
        }

        return chunkSections;
    }

    @Override
    public CompoundBinaryTag getHeightMaps() {
        return this.heightMap;
    }

    @Override
    public List<CompoundBinaryTag> getTileEntities() {
        return this.blockEntities;
    }

    @Override
    public List<CompoundBinaryTag> getEntities() {
        return this.entities;
    }

    @Override
    public Map<String, BinaryTag> getExtraData() {
        return this.extra;
    }

    @Override
    public CompoundBinaryTag getUpgradeData() {
        return this.upgradeData;
    }

    @Override
    public ListBinaryTag getBlockTicks() {
        if (this.blockTicks == null) {
            return null;
        }
        return PlazaSlimeChunkConverter.convertSavedBlockTicks(this.blockTicks);
    }

    @Override
    public ListBinaryTag getFluidTicks() {
        if (this.fluidTicks == null) {
            return null;
        }
        return PlazaSlimeChunkConverter.convertSavedFluidTicks(this.fluidTicks);
    }

    @Override
    public CompoundBinaryTag getPoiChunkSections() {
        return this.poiChunk;
    }

    record PlazaPartiallySerializedSlimeChunkSection(LevelChunkSection section, PlazaNibbleArray blockLight, PlazaNibbleArray skyLight) {
    }
}
