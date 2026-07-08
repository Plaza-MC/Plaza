package org.plazamc.server.slime.skeleton;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.plazamc.server.slime.PlazaSlimeChunk;
import org.plazamc.server.slime.PlazaSlimeChunkSection;
import org.plazamc.server.slime.PlazaSlimeWorld;
import org.plazamc.server.slime.loader.PlazaSlimeLoader;
import org.plazamc.server.slime.util.PlazaSlimeUtil;
import org.plazamc.server.slime.util.PlazaNibbleArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cloning helpers for Slime skeleton worlds.
 */
public final class PlazaSkeletonCloning {

    private PlazaSkeletonCloning() {
        throw new AssertionError();
    }

    public static PlazaSkeletonSlimeWorld fullClone(String worldName, PlazaSlimeWorld world, PlazaSlimeLoader loader, boolean readOnly) {
        return new PlazaSkeletonSlimeWorld(
                worldName,
                loader == null ? world.getLoader() : loader,
                loader == null || readOnly,
                cloneChunkStorage(world.getChunkStorage()),
                new ConcurrentHashMap<>(world.getExtraData()),
                world.getPropertyMap().clone(),
                world.getDataVersion()
        );
    }

    public static PlazaSkeletonSlimeWorld weakCopy(PlazaSlimeWorld world) {
        Long2ObjectMap<PlazaSlimeChunk> cloned = new Long2ObjectOpenHashMap<>();
        for (PlazaSlimeChunk chunk : world.getChunkStorage()) {
            long pos = PlazaSlimeUtil.chunkPosition(chunk.getX(), chunk.getZ());
            cloned.put(pos, chunk);
        }

        return new PlazaSkeletonSlimeWorld(
                world.getName(),
                world.getLoader(),
                world.isReadOnly(),
                cloned,
                new ConcurrentHashMap<>(world.getExtraData()),
                world.getPropertyMap().clone(),
                world.getDataVersion()
        );
    }

    private static Long2ObjectMap<PlazaSlimeChunk> cloneChunkStorage(Collection<PlazaSlimeChunk> slimeChunkMap) {
        Long2ObjectMap<PlazaSlimeChunk> cloned = new Long2ObjectOpenHashMap<>();
        for (PlazaSlimeChunk chunk : slimeChunkMap) {
            long pos = PlazaSlimeUtil.chunkPosition(chunk.getX(), chunk.getZ());

            PlazaSlimeChunkSection[] copied = new PlazaSlimeChunkSection[chunk.getSections().length];
            for (int i = 0; i < copied.length; i++) {
                PlazaSlimeChunkSection original = chunk.getSections()[i];
                if (original == null) {
                    continue;
                }

                PlazaNibbleArray blockLight = original.getBlockLight();
                PlazaNibbleArray skyLight = original.getSkyLight();

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
}
