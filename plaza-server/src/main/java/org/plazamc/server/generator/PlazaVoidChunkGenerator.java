package org.plazamc.server.generator;

import java.util.List;
import java.util.Random;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.plazamc.server.PlazaConfig;
import org.jetbrains.annotations.NotNull;

/**
 * Empty Bukkit generator used by Plaza's plugin-driven default profile.
 */
public final class PlazaVoidChunkGenerator extends ChunkGenerator {
    private static final int PLATFORM_Y = 63;
    private static final int PLATFORM_RADIUS = 2;
    private static final Material PLATFORM_MATERIAL = Material.GLASS;

    @Override
    public @NotNull List<BlockPopulator> getDefaultPopulators(final @NotNull World world) {
        return List.of();
    }

    @Override
    public void generateNoise(
        final @NotNull WorldInfo worldInfo,
        final @NotNull Random random,
        final int chunkX,
        final int chunkZ,
        final @NotNull ChunkData chunkData
    ) {
        if (!PlazaConfig.spawnPlatformEnabled()) {
            return;
        }

        for (int x = -PLATFORM_RADIUS; x <= PLATFORM_RADIUS; x++) {
            for (int z = -PLATFORM_RADIUS; z <= PLATFORM_RADIUS; z++) {
                if (x >> 4 == chunkX && z >> 4 == chunkZ) {
                    chunkData.setBlock(x & 15, PLATFORM_Y, z & 15, PLATFORM_MATERIAL);
                }
            }
        }
    }

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }

    @Override
    public boolean canSpawn(final @NotNull World world, final int x, final int z) {
        return true;
    }

    @Override
    public Location getFixedSpawnLocation(final @NotNull World world, final @NotNull Random random) {
        return new Location(world, 0.5, PLATFORM_Y + 1.0, 0.5);
    }

    @Override
    public int getBaseHeight(
        final @NotNull WorldInfo worldInfo,
        final @NotNull Random random,
        final int x,
        final int z,
        final @NotNull HeightMap heightMap
    ) {
        return worldInfo.getMinHeight();
    }

    @Override
    @Deprecated(since = "1.17.1")
    public boolean isParallelCapable() {
        return true;
    }
}
