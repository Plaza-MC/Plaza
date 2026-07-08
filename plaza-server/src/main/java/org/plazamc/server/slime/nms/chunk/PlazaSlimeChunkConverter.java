package org.plazamc.server.slime.nms.chunk;

import ca.spottedleaf.moonrise.patches.chunk_system.level.poi.PoiChunk;
import ca.spottedleaf.moonrise.patches.starlight.light.SWMRNibbleArray;
import ca.spottedleaf.moonrise.patches.starlight.light.StarLightEngine;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.SavedTick;
import org.plazamc.server.slime.PlazaSlimeChunk;
import org.plazamc.server.slime.PlazaSlimeChunkSection;
import org.plazamc.server.slime.nms.PlazaNbtConverter;
import org.plazamc.server.slime.nms.PlazaSlimeLevelInstance;
import org.plazamc.server.slime.skeleton.PlazaSkeletonSlimeSection;
import org.plazamc.server.slime.util.PlazaNibbleArray;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class PlazaSlimeChunkConverter {

    private static final Codec<List<SavedTick<Block>>> BLOCK_TICKS_CODEC = SavedTick.codec(BuiltInRegistries.BLOCK.byNameCodec()).listOf();
    private static final Codec<List<SavedTick<Fluid>>> FLUID_TICKS_CODEC = SavedTick.codec(BuiltInRegistries.FLUID.byNameCodec()).listOf();

    private static final CompoundBinaryTag EMPTY_BLOCK_STATE_PALETTE;
    private static final CompoundBinaryTag EMPTY_BIOME_PALETTE;

    static {
        PalettedContainerFactory factory = PalettedContainerFactory.create(net.minecraft.server.MinecraftServer.getServer().registryAccess());
        {
            PalettedContainer<BlockState> empty = new PalettedContainer<>(Blocks.AIR.defaultBlockState(), factory.blockStatesStrategy(), null);
            Tag tag = factory.blockStatesContainerCodec().encodeStart(NbtOps.INSTANCE, empty).getOrThrow();
            EMPTY_BLOCK_STATE_PALETTE = PlazaNbtConverter.convertTag(tag);
        }
        {
            Registry<Biome> biomes = net.minecraft.server.MinecraftServer.getServer().registryAccess().lookupOrThrow(Registries.BIOME);
            PalettedContainer<Holder<Biome>> empty = new PalettedContainer<>(biomes.get(Biomes.PLAINS).orElseThrow(), factory.biomeStrategy(), null);
            Tag tag = factory.biomeContainerRWCodec().encodeStart(NbtOps.INSTANCE, empty).getOrThrow();
            EMPTY_BIOME_PALETTE = PlazaNbtConverter.convertTag(tag);
        }
    }

    private PlazaSlimeChunkConverter() {
        throw new AssertionError();
    }

    public static PlazaSlimeChunkLevel deserializeSlimeChunk(PlazaSlimeLevelInstance instance, PlazaSlimeChunk chunk) {
        int x = chunk.getX();
        int z = chunk.getZ();

        ChunkPos pos = new ChunkPos(x, z);

        LevelChunkSection[] sections = new LevelChunkSection[instance.getSectionsCount()];

        SWMRNibbleArray[] blockNibbles = StarLightEngine.getFilledEmptyLight(instance);
        SWMRNibbleArray[] skyNibbles = StarLightEngine.getFilledEmptyLight(instance);
        instance.getServer().scheduleOnMain(() -> {
            instance.getLightEngine().retainData(pos, true);
        });

        Registry<Biome> biomeRegistry = instance.registryAccess().lookupOrThrow(Registries.BIOME);

        Codec<PalettedContainer<Holder<Biome>>> codec = instance.palettedContainerFactory().biomeContainerRWCodec();

        for (int sectionId = 0; sectionId < chunk.getSections().length; sectionId++) {
            PlazaSlimeChunkSection slimeSection = chunk.getSections()[sectionId];

            if (slimeSection != null) {
                PlazaNibbleArray blockLight = slimeSection.getBlockLight();
                if (blockLight != null) {
                    blockNibbles[sectionId] = new SWMRNibbleArray(blockLight.getBacking());
                }

                PlazaNibbleArray skyLight = slimeSection.getSkyLight();
                if (skyLight != null) {
                    skyNibbles[sectionId] = new SWMRNibbleArray(skyLight.getBacking());
                }

                PalettedContainer<BlockState> blockPalette;
                if (slimeSection.getBlockStatesTag() != null) {
                    DataResult<PalettedContainer<BlockState>> dataresult = instance.palettedContainerFactory().blockStatesContainerCodec()
                            .parse(NbtOps.INSTANCE, PlazaNbtConverter.convertTag(slimeSection.getBlockStatesTag()))
                            .promotePartial((s) -> System.out.println("Recoverable error when parsing section " + x + "," + z + ": " + s));
                    blockPalette = dataresult.getOrThrow();
                } else {
                    blockPalette = new PalettedContainer<>(Blocks.AIR.defaultBlockState(), instance.palettedContainerFactory().blockStatesStrategy(), null);
                }

                PalettedContainer<Holder<Biome>> biomePalette;
                if (slimeSection.getBiomeTag() != null) {
                    DataResult<PalettedContainer<Holder<Biome>>> dataresult = codec
                            .parse(NbtOps.INSTANCE, PlazaNbtConverter.convertTag(slimeSection.getBiomeTag()))
                            .promotePartial((s) -> System.out.println("Recoverable error when parsing section " + x + "," + z + ": " + s));
                    biomePalette = dataresult.getOrThrow();
                } else {
                    biomePalette = new PalettedContainer<>(biomeRegistry.get(Biomes.PLAINS).orElseThrow(), instance.palettedContainerFactory().biomeStrategy(), null);
                }

                if (sectionId < sections.length) {
                    sections[sectionId] = new LevelChunkSection(blockPalette, biomePalette);
                }
            }
        }

        LevelChunkTicks<Block> blockLevelChunkTicks;
        if (chunk.getBlockTicks() != null) {
            ListTag tag = (ListTag) PlazaNbtConverter.convertTag(chunk.getBlockTicks());
            List<SavedTick<Block>> blockList = SavedTick.filterTickListForChunk(
                    BLOCK_TICKS_CODEC.parse(NbtOps.INSTANCE, tag).resultOrPartial().orElse(List.of()), pos);
            blockLevelChunkTicks = new LevelChunkTicks<>(blockList);
        } else {
            blockLevelChunkTicks = new LevelChunkTicks<>();
        }

        LevelChunkTicks<Fluid> fluidLevelChunkTicks;
        if (chunk.getFluidTicks() != null) {
            ListTag tag = (ListTag) PlazaNbtConverter.convertTag(chunk.getFluidTicks());
            List<SavedTick<Fluid>> fluidList = SavedTick.filterTickListForChunk(
                    FLUID_TICKS_CODEC.parse(NbtOps.INSTANCE, tag).resultOrPartial().orElse(List.of()), pos);
            fluidLevelChunkTicks = new LevelChunkTicks<>(fluidList);
        } else {
            fluidLevelChunkTicks = new LevelChunkTicks<>();
        }

        net.minecraft.world.level.chunk.UpgradeData upgradeData;
        if (chunk.getUpgradeData() != null) {
            upgradeData = new net.minecraft.world.level.chunk.UpgradeData((CompoundTag) PlazaNbtConverter.convertTag(chunk.getUpgradeData()), instance);
        } else {
            upgradeData = net.minecraft.world.level.chunk.UpgradeData.EMPTY;
        }

        List<CompoundTag> tileEntities = chunk.getTileEntities().stream()
                .map(tag -> (CompoundTag) PlazaNbtConverter.convertTag(tag))
                .toList();

        LevelChunk.PostLoadProcessor processor = levelChunk -> {
            for (CompoundTag tag : tileEntities) {
                BlockPos blockPos = BlockEntity.getPosFromTag(levelChunk.getPos(), tag);
                BlockState state = levelChunk.getBlockState(blockPos);
                BlockEntity blockEntity = BlockEntity.loadStatic(blockPos, state, tag, levelChunk.level.registryAccess());
                if (blockEntity != null) {
                    levelChunk.setBlockEntity(blockEntity);
                }
            }
        };

        PlazaSlimeChunkLevel nmsChunk = new PlazaSlimeChunkLevel(
                instance,
                chunk,
                pos,
                upgradeData,
                blockLevelChunkTicks,
                fluidLevelChunkTicks,
                0L,
                sections,
                processor,
                null
        );

        EnumSet<Heightmap.Types> heightMapTypes = nmsChunk.getPersistedStatus().heightmapsAfter();
        CompoundBinaryTag heightMaps = chunk.getHeightMaps();
        EnumSet<Heightmap.Types> unsetHeightMaps = EnumSet.noneOf(Heightmap.Types.class);

        nmsChunk.starlight$setBlockNibbles(blockNibbles);
        nmsChunk.starlight$setSkyNibbles(skyNibbles);

        for (Heightmap.Types type : heightMapTypes) {
            String name = type.getSerializedName();

            long[] heightMap = heightMaps.getLongArray(name);
            if (heightMap.length > 0) {
                nmsChunk.setHeightmap(type, heightMap);
            } else {
                unsetHeightMaps.add(type);
            }
        }

        if (!unsetHeightMaps.isEmpty()) {
            Heightmap.primeHeightmaps(nmsChunk, unsetHeightMaps);
        }

        if (chunk.getExtraData().containsKey("ChunkBukkitValues")) {
            nmsChunk.persistentDataContainer.putAll((CompoundTag) PlazaNbtConverter.convertTag(chunk.getExtraData().get("ChunkBukkitValues")));
        }

        return nmsChunk;
    }

    public static PlazaSlimeChunkSection convertChunkSection(
            Codec<PalettedContainerRO<Holder<Biome>>> biomeCodec,
            Codec<PalettedContainer<BlockState>> blockCodec,
            LevelChunkSection section,
            PlazaNibbleArray blockLightArray,
            PlazaNibbleArray skyLightArray) {

        CompoundBinaryTag blockStateTag;
        if (section.hasOnlyAir()) {
            blockStateTag = EMPTY_BLOCK_STATE_PALETTE;
        } else {
            Tag data = blockCodec.encodeStart(NbtOps.INSTANCE, section.getStates()).getOrThrow();
            blockStateTag = PlazaNbtConverter.convertTag(data);
        }

        CompoundBinaryTag biomeTag;
        @SuppressWarnings("unchecked")
        PalettedContainer<Holder<Biome>> biomes = (PalettedContainer<Holder<Biome>>) section.getBiomes();
        if (biomes.data.palette().getSize() == 1 && biomes.data.palette().maybeHas((h) -> h.is(Biomes.PLAINS))) {
            biomeTag = EMPTY_BIOME_PALETTE;
        } else {
            Tag biomeData = biomeCodec.encodeStart(NbtOps.INSTANCE, section.getBiomes()).getOrThrow();
            biomeTag = PlazaNbtConverter.convertTag(biomeData);
        }

        return new PlazaSkeletonSlimeSection(blockStateTag, biomeTag, blockLightArray, skyLightArray);
    }

    public static ListBinaryTag convertSavedFluidTicks(List<SavedTick<Fluid>> ticks) {
        Tag tag = FLUID_TICKS_CODEC.encodeStart(NbtOps.INSTANCE, ticks).getOrThrow();
        return PlazaNbtConverter.convertTag(tag);
    }

    public static ListBinaryTag convertSavedBlockTicks(List<SavedTick<Block>> ticks) {
        Tag tag = BLOCK_TICKS_CODEC.encodeStart(NbtOps.INSTANCE, ticks).getOrThrow();
        return PlazaNbtConverter.convertTag(tag);
    }

    public static CompoundTag createPoiChunk(PlazaSlimeChunk chunk) {
        return createPoiChunkFromSlimeSections(chunk.getPoiChunkSections(), SharedConstants.getCurrentVersion().dataVersion().version());
    }

    public static CompoundTag createPoiChunkFromSlimeSections(CompoundBinaryTag slimePoiSections, int dataVersion) {
        CompoundTag tag = new CompoundTag();
        tag.put("Sections", PlazaNbtConverter.convertTag(slimePoiSections));
        tag.putInt("DataVersion", dataVersion);
        return tag;
    }

    public static CompoundBinaryTag toSlimeSections(PoiChunk poiChunk) {
        CompoundTag save = poiChunk.save();
        return getSlimeSectionsFromPoiCompound(save);
    }

    public static CompoundBinaryTag getSlimeSectionsFromPoiCompound(CompoundTag save) {
        if (save == null) {
            return null;
        }

        CompoundTag sections = save.getCompoundOrEmpty("Sections");
        return PlazaNbtConverter.convertTag(sections);
    }
}
