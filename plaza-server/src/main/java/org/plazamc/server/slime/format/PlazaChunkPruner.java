package org.plazamc.server.slime.format;

import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.plazamc.server.slime.PlazaSlimeChunk;
import org.plazamc.server.slime.PlazaSlimeChunkSection;
import org.plazamc.server.slime.PlazaSlimeWorld;
import org.plazamc.server.slime.properties.PlazaSlimeProperties;
import org.plazamc.server.slime.properties.PlazaSlimePropertyMap;

import java.util.List;

public class PlazaChunkPruner {

    public static boolean canBePruned(PlazaSlimeWorld world, PlazaSlimeChunk chunk) {
        PlazaSlimePropertyMap propertyMap = world.getPropertyMap();
        if (propertyMap.getValue(PlazaSlimeProperties.SHOULD_LIMIT_SAVE)) {
            int minX = propertyMap.getValue(PlazaSlimeProperties.SAVE_MIN_X);
            int maxX = propertyMap.getValue(PlazaSlimeProperties.SAVE_MAX_X);
            int minZ = propertyMap.getValue(PlazaSlimeProperties.SAVE_MIN_Z);
            int maxZ = propertyMap.getValue(PlazaSlimeProperties.SAVE_MAX_Z);

            int chunkX = chunk.getX();
            int chunkZ = chunk.getZ();

            if (chunkX < minX || chunkX > maxX) {
                return true;
            }

            if (chunkZ < minZ || chunkZ > maxZ) {
                return true;
            }
        }

        String pruningSetting = world.getPropertyMap().getValue(PlazaSlimeProperties.CHUNK_PRUNING);
        if (pruningSetting.equals("aggressive")) {
            return chunk.getTileEntities().isEmpty() && chunk.getEntities().isEmpty() && areSectionsEmpty(chunk.getSections());
        }

        return false;
    }

    private static boolean areSectionsEmpty(PlazaSlimeChunkSection[] sections) {
        for (PlazaSlimeChunkSection chunkSection : sections) {
            try {
                ListBinaryTag paletteTag = chunkSection.getBlockStatesTag().getList("palette");
                if (paletteTag.elementType() != BinaryTagTypes.COMPOUND) {
                    continue;
                }
                List<CompoundBinaryTag> palette = paletteTag.stream().map(tag -> (CompoundBinaryTag) tag).toList();
                if (palette.size() > 1) return false;
                if (!palette.getFirst().getString("Name").equals("minecraft:air")) return false;
            } catch (final Exception e) {
                return false;
            }
        }
        return true;
    }
}
