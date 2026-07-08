package org.plazamc.api.world;

import org.jetbrains.annotations.NotNull;

/**
 * Common world property keys used across Plaza world formats.
 */
public final class PlazaWorldProperties {

    private PlazaWorldProperties() {
    }

    public static final String SPAWN_X = "spawnX";
    public static final String SPAWN_Y = "spawnY";
    public static final String SPAWN_Z = "spawnZ";

    public static final String DIFFICULTY = "difficulty";
    public static final String PVP = "pvp";
    public static final String ENVIRONMENT = "environment";
    public static final String WORLD_TYPE = "worldType";
    public static final String DEFAULT_BIOME = "defaultBiome";
    public static final String SEA_LEVEL = "seaLevel";

    public static final String ALLOW_MONSTERS = "allowMonsters";
    public static final String ALLOW_ANIMALS = "allowAnimals";
    public static final String DRAGON_BATTLE = "dragonBattle";

    public static final String READ_ONLY = "readOnly";
    public static final String LOCKED = "locked";

    public static final String SAVE_POI = "savePoi";
    public static final String SAVE_BLOCK_TICKS = "saveBlockTicks";
    public static final String SAVE_FLUID_TICKS = "saveFluidTicks";

    public static final String DATA_VERSION = "dataVersion";

    /**
     * Returns the default property map used for newly created worlds.
     */
    @NotNull
    public static PlazaWorldPropertyMap defaults() {
        PlazaWorldPropertyMap map = new PlazaWorldPropertyMap();
        map.setString(PlazaWorldProperties.DIFFICULTY, "peaceful");
        map.setBoolean(PlazaWorldProperties.PVP, false);
        map.setString(PlazaWorldProperties.ENVIRONMENT, "normal");
        map.setString(PlazaWorldProperties.WORLD_TYPE, "default");
        map.setString(PlazaWorldProperties.DEFAULT_BIOME, "minecraft:plains");
        map.setInt(PlazaWorldProperties.SEA_LEVEL, 63);
        map.setBoolean(PlazaWorldProperties.ALLOW_MONSTERS, false);
        map.setBoolean(PlazaWorldProperties.ALLOW_ANIMALS, false);
        map.setBoolean(PlazaWorldProperties.DRAGON_BATTLE, false);
        map.setBoolean(PlazaWorldProperties.READ_ONLY, false);
        map.setBoolean(PlazaWorldProperties.LOCKED, false);
        map.setBoolean(PlazaWorldProperties.SAVE_POI, true);
        map.setBoolean(PlazaWorldProperties.SAVE_BLOCK_TICKS, false);
        map.setBoolean(PlazaWorldProperties.SAVE_FLUID_TICKS, false);
        return map;
    }
}
