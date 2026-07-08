package org.plazamc.server.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelData;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.plazamc.server.PlazaConfig;
import org.plazamc.server.generator.PlazaVoidChunkGenerator;
import org.plazamc.server.slime.nms.PlazaSlimeLevelInstance;

/**
 * Runtime world defaults for Plaza's plugin-driven profile.
 */
public final class PlazaWorldDefaults {
    private static final BlockPos PLAZA_SPAWN = new BlockPos(0, PlazaVoidChunkGenerator.PLATFORM_Y + 1, 0);
    private static final float SPAWN_YAW = 0.0F;
    private static final float SPAWN_PITCH = 0.0F;

    private PlazaWorldDefaults() {
    }

    public static void applyOnWorldLoad(final ServerLevel level) {
        if (!isPlazaVoidWorld(level)) {
            return;
        }

        forceCenteredSpawn(level);
        recalculateDynamicWorldBorder(level, true);
    }

    public static void tickDynamicWorldBorder(final ServerLevel level) {
        if (!isPlazaVoidWorld(level) || !PlazaConfig.dynamicWorldBorderEnabled()) {
            return;
        }

        final long interval = Math.max(1L, PlazaConfig.dynamicWorldBorderRecalculationIntervalTicks());
        if (level.getGameTime() % interval != 0L) {
            return;
        }

        recalculateDynamicWorldBorder(level, false);
    }

    private static boolean isPlazaVoidWorld(final ServerLevel level) {
        return level.generator instanceof PlazaVoidChunkGenerator || level instanceof PlazaSlimeLevelInstance;
    }

    private static void forceCenteredSpawn(final ServerLevel level) {
        final LevelData.RespawnData current = level.serverLevelData.getRespawnData();
        if (current.pos().equals(PLAZA_SPAWN) && current.yaw() == SPAWN_YAW && current.pitch() == SPAWN_PITCH) {
            return;
        }

        // Only force the spawn for empty Slime worlds (no saved chunks). Imported or built worlds
        // keep their own spawn so that minigame/lobby maps behave as their creators intended.
        if (level instanceof PlazaSlimeLevelInstance slimeLevel && !slimeLevel.slimeInstance.getChunkStorage().isEmpty()) {
            return;
        }

        level.serverLevelData.setSpawn(LevelData.RespawnData.of(level.dimension(), PLAZA_SPAWN, SPAWN_YAW, SPAWN_PITCH));
    }

    private static void recalculateDynamicWorldBorder(final ServerLevel level, final boolean forceFallbackPlatformBounds) {
        if (!PlazaConfig.dynamicWorldBorderEnabled()) {
            return;
        }

        final World world = level.getWorld();
        final Chunk[] loadedChunks = world.getLoadedChunks();
        final int maxChunks = Math.max(1, PlazaConfig.dynamicWorldBorderMaxChunksScanned());
        final int worldMinY = world.getMinHeight();

        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        boolean foundBlock = false;
        int scanned = 0;

        for (final Chunk chunk : loadedChunks) {
            if (scanned++ >= maxChunks) {
                break;
            }

            final int baseX = chunk.getX() << 4;
            final int baseZ = chunk.getZ() << 4;
            for (int dx = 0; dx < 16; dx++) {
                final int x = baseX + dx;
                for (int dz = 0; dz < 16; dz++) {
                    final int z = baseZ + dz;
                    final int topY = world.getHighestBlockYAt(x, z);
                    if (topY <= worldMinY) {
                        continue;
                    }

                    final Material type = world.getBlockAt(x, topY, z).getType();
                    if (type.isAir()) {
                        continue;
                    }

                    foundBlock = true;
                    minX = Math.min(minX, x);
                    minZ = Math.min(minZ, z);
                    maxX = Math.max(maxX, x);
                    maxZ = Math.max(maxZ, z);
                }
            }
        }

        if (!foundBlock) {
            if (!forceFallbackPlatformBounds || !PlazaConfig.spawnPlatformEnabled()) {
                return;
            }

            minX = -PlazaVoidChunkGenerator.PLATFORM_RADIUS;
            minZ = -PlazaVoidChunkGenerator.PLATFORM_RADIUS;
            maxX = PlazaVoidChunkGenerator.PLATFORM_RADIUS;
            maxZ = PlazaVoidChunkGenerator.PLATFORM_RADIUS;
        }

        applyBorder(world, minX, minZ, maxX, maxZ);
    }

    private static void applyBorder(final World world, final int minX, final int minZ, final int maxX, final int maxZ) {
        final int margin = Math.max(0, PlazaConfig.dynamicWorldBorderMargin());
        final double centerX = (minX + maxX + 1) / 2.0D;
        final double centerZ = (minZ + maxZ + 1) / 2.0D;
        final double spanX = maxX - minX + 1.0D;
        final double spanZ = maxZ - minZ + 1.0D;
        final double size = Math.max(Math.max(spanX, spanZ) + (2.0D * margin), PlazaConfig.dynamicWorldBorderMinimumSize());

        final WorldBorder border = world.getWorldBorder();
        final Location currentCenter = border.getCenter();
        if (Math.abs(border.getSize() - size) < 1.0D
            && Math.abs(currentCenter.getX() - centerX) < 1.0D
            && Math.abs(currentCenter.getZ() - centerZ) < 1.0D) {
            return;
        }

        border.setCenter(centerX, centerZ);
        border.setSize(size);
    }
}
