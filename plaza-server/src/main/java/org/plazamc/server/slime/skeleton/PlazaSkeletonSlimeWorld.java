package org.plazamc.server.slime.skeleton;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.persistence.PersistentDataContainer;
import org.jspecify.annotations.Nullable;
import org.plazamc.server.slime.PlazaSlimeChunk;
import org.plazamc.server.slime.PlazaSlimeChunkSection;
import org.plazamc.server.slime.PlazaSlimeWorld;
import org.plazamc.server.slime.format.PlazaSlimeSerializer;
import org.plazamc.server.slime.loader.PlazaSlimeLoader;
import org.plazamc.server.slime.loader.PlazaWorldAlreadyExistsException;
import org.plazamc.server.slime.pdc.PlazaAdventurePersistentDataContainer;
import org.plazamc.server.slime.properties.PlazaSlimePropertyMap;
import org.plazamc.server.slime.util.PlazaSlimeUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PlazaSkeletonSlimeWorld implements PlazaSlimeWorld {

    private final String name;
    private final @Nullable PlazaSlimeLoader loader;
    private final boolean readOnly;
    private final Long2ObjectMap<PlazaSlimeChunk> chunkStorage;
    private final ConcurrentMap<String, BinaryTag> extraSerialized;
    private final PlazaSlimePropertyMap slimePropertyMap;
    private final int dataVersion;
    private final PlazaAdventurePersistentDataContainer pdc;

    public PlazaSkeletonSlimeWorld(
            String name,
            @Nullable PlazaSlimeLoader loader,
            boolean readOnly,
            Long2ObjectMap<PlazaSlimeChunk> chunkStorage,
            ConcurrentMap<String, BinaryTag> extraSerialized,
            PlazaSlimePropertyMap slimePropertyMap,
            int dataVersion
    ) {
        this.name = name;
        this.loader = loader;
        this.readOnly = readOnly;
        this.chunkStorage = chunkStorage;
        this.extraSerialized = extraSerialized;
        this.slimePropertyMap = slimePropertyMap;
        this.dataVersion = dataVersion;
        this.pdc = new PlazaAdventurePersistentDataContainer(extraSerialized);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public PlazaSlimeLoader getLoader() {
        return this.loader;
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
        return this.extraSerialized;
    }

    @Override
    public Collection<CompoundBinaryTag> getWorldMaps() {
        return List.of();
    }

    @Override
    public PlazaSlimePropertyMap getPropertyMap() {
        return this.slimePropertyMap;
    }

    @Override
    public boolean isReadOnly() {
        return this.readOnly || this.loader == null;
    }

    @Override
    public int getDataVersion() {
        return this.dataVersion;
    }

    @Override
    public PlazaSlimeWorld clone(String worldName) {
        try {
            return clone(worldName, null);
        } catch (PlazaWorldAlreadyExistsException | IOException ignored) {
            return null; // Never going to happen
        }
    }

    @Override
    public PlazaSlimeWorld clone(String worldName, PlazaSlimeLoader loader) throws PlazaWorldAlreadyExistsException, IOException {
        if (name.equals(worldName)) {
            throw new IllegalArgumentException("The clone world cannot have the same name as the original world!");
        }

        if (worldName == null) {
            throw new IllegalArgumentException("The world name cannot be null!");
        }

        if (loader != null && loader.worldExists(worldName)) {
            throw new PlazaWorldAlreadyExistsException(worldName);
        }

        PlazaSlimeWorld cloned = fullClone(worldName, loader == null ? this.loader : loader, loader == null);
        if (loader != null) {
            loader.saveWorld(worldName, PlazaSlimeSerializer.serialize(cloned));
        }

        return cloned;
    }

    private PlazaSkeletonSlimeWorld fullClone(String worldName, @Nullable PlazaSlimeLoader loader, boolean readOnly) {
        return new PlazaSkeletonSlimeWorld(
                worldName,
                loader,
                readOnly,
                cloneChunkStorage(this.getChunkStorage()),
                new ConcurrentHashMap<>(this.getExtraData()),
                this.getPropertyMap().clone(),
                this.getDataVersion()
        );
    }

    private static Long2ObjectMap<PlazaSlimeChunk> cloneChunkStorage(Collection<PlazaSlimeChunk> chunks) {
        Long2ObjectMap<PlazaSlimeChunk> cloned = new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();
        for (PlazaSlimeChunk chunk : chunks) {
            long pos = PlazaSlimeUtil.chunkPosition(chunk.getX(), chunk.getZ());

            PlazaSlimeChunkSection[] copied = new PlazaSlimeChunkSection[chunk.getSections().length];
            for (int i = 0; i < copied.length; i++) {
                PlazaSlimeChunkSection original = chunk.getSections()[i];
                if (original == null) continue;

                org.plazamc.server.slime.util.PlazaNibbleArray blockLight = original.getBlockLight();
                org.plazamc.server.slime.util.PlazaNibbleArray skyLight = original.getSkyLight();

                copied[i] = new PlazaSkeletonSlimeSection(
                        original.getBlockStatesTag(),
                        original.getBiomeTag(),
                        blockLight == null ? null : blockLight.clone(),
                        skyLight == null ? null : skyLight.clone()
                );
            }

            cloned.put(pos, new PlazaSkeletonSlimeChunk(
                    chunk.getX(),
                    chunk.getZ(),
                    copied,
                    chunk.getHeightMaps(),
                    new ArrayList<>(chunk.getTileEntities()),
                    new ArrayList<>(chunk.getEntities()),
                    new ConcurrentHashMap<>(chunk.getExtraData()),
                    null,
                    chunk.getPoiChunkSections(),
                    chunk.getBlockTicks(),
                    chunk.getFluidTicks()
            ));
        }

        return cloned;
    }

    @Override
    public PersistentDataContainer getPersistentDataContainer() {
        return this.pdc;
    }

    public String name() {
        return name;
    }

    public @Nullable PlazaSlimeLoader loader() {
        return loader;
    }

    public boolean readOnly() {
        return readOnly;
    }

    public Long2ObjectMap<PlazaSlimeChunk> chunkStorage() {
        return chunkStorage;
    }

    public ConcurrentMap<String, BinaryTag> extraSerialized() {
        return extraSerialized;
    }

    public PlazaSlimePropertyMap slimePropertyMap() {
        return slimePropertyMap;
    }

    public int dataVersion() {
        return dataVersion;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (PlazaSkeletonSlimeWorld) obj;
        return Objects.equals(this.name, that.name) &&
               Objects.equals(this.loader, that.loader) &&
               this.readOnly == that.readOnly &&
               Objects.equals(this.chunkStorage, that.chunkStorage) &&
               Objects.equals(this.extraSerialized, that.extraSerialized) &&
               Objects.equals(this.slimePropertyMap, that.slimePropertyMap) &&
               this.dataVersion == that.dataVersion;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, loader, readOnly, chunkStorage, extraSerialized, slimePropertyMap, dataVersion);
    }

    @Override
    public String toString() {
        return "PlazaSkeletonSlimeWorld[" +
               "name=" + name + ", " +
               "loader=" + loader + ", " +
               "readOnly=" + readOnly + ", " +
               "chunkStorage=" + chunkStorage + ", " +
               "extraSerialized=" + extraSerialized + ", " +
               "slimePropertyMap=" + slimePropertyMap + ", " +
               "dataVersion=" + dataVersion + ']';
    }
}
