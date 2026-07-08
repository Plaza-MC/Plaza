package org.plazamc.server.slime.properties;

import org.plazamc.server.slime.properties.type.PlazaSlimePropertyBoolean;
import org.plazamc.server.slime.properties.type.PlazaSlimePropertyFloat;
import org.plazamc.server.slime.properties.type.PlazaSlimePropertyInt;
import org.plazamc.server.slime.properties.type.PlazaSlimePropertyString;

/**
 * Class with all existing Slime world properties.
 */
public class PlazaSlimeProperties {

    public static final PlazaSlimePropertyInt SPAWN_X = PlazaSlimePropertyInt.create("spawnX", 0);
    public static final PlazaSlimePropertyInt SPAWN_Y = PlazaSlimePropertyInt.create("spawnY", 64);
    public static final PlazaSlimePropertyInt SPAWN_Z = PlazaSlimePropertyInt.create("spawnZ", 0);
    public static final PlazaSlimePropertyFloat SPAWN_YAW = PlazaSlimePropertyFloat.create("spawnYaw", 0.0f);

    public static final PlazaSlimePropertyString DIFFICULTY = PlazaSlimePropertyString.create("difficulty", "peaceful", (value) ->
            value.equalsIgnoreCase("peaceful") || value.equalsIgnoreCase("easy")
                    || value.equalsIgnoreCase("normal") || value.equalsIgnoreCase("hard")
    );

    public static final PlazaSlimePropertyBoolean ALLOW_MONSTERS = PlazaSlimePropertyBoolean.create("allowMonsters", true);
    public static final PlazaSlimePropertyBoolean ALLOW_ANIMALS = PlazaSlimePropertyBoolean.create("allowAnimals", true);
    public static final PlazaSlimePropertyBoolean DRAGON_BATTLE = PlazaSlimePropertyBoolean.create("dragonBattle", false);
    public static final PlazaSlimePropertyBoolean PVP = PlazaSlimePropertyBoolean.create("pvp", true);

    public static final PlazaSlimePropertyString ENVIRONMENT = PlazaSlimePropertyString.create("environment", "normal", (value) ->
            value.equalsIgnoreCase("normal") || value.equalsIgnoreCase("nether") || value.equalsIgnoreCase("the_end")
    );

    public static final PlazaSlimePropertyString WORLD_TYPE = PlazaSlimePropertyString.create("worldtype", "default", (value) ->
            value.equalsIgnoreCase("default") || value.equalsIgnoreCase("flat") || value.equalsIgnoreCase("large_biomes")
                    || value.equalsIgnoreCase("amplified") || value.equalsIgnoreCase("customized")
                    || value.equalsIgnoreCase("debug_all_block_states") || value.equalsIgnoreCase("default_1_1")
    );

    public static final PlazaSlimePropertyString DEFAULT_BIOME = PlazaSlimePropertyString.create("defaultBiome", "minecraft:plains");

    public static final PlazaSlimePropertyBoolean SHOULD_LIMIT_SAVE = PlazaSlimePropertyBoolean.create("hasSaveBounds", false);
    public static final PlazaSlimePropertyInt SAVE_MIN_X = PlazaSlimePropertyInt.create("saveMinX", 0);
    public static final PlazaSlimePropertyInt SAVE_MIN_Z = PlazaSlimePropertyInt.create("saveMinZ", 0);
    public static final PlazaSlimePropertyInt SAVE_MAX_X = PlazaSlimePropertyInt.create("saveMaxX", 0);
    public static final PlazaSlimePropertyInt SAVE_MAX_Z = PlazaSlimePropertyInt.create("saveMaxZ", 0);

    public static final PlazaSlimePropertyString CHUNK_PRUNING = PlazaSlimePropertyString.create("pruning", "aggressive", (value) ->
            value.equalsIgnoreCase("aggressive") || value.equalsIgnoreCase("never")
    );

    @Deprecated(forRemoval = true)
    public static final PlazaSlimePropertyInt CHUNK_SECTION_MIN = PlazaSlimePropertyInt.create("chunkSectionMin", -4);

    @Deprecated(forRemoval = true)
    public static final PlazaSlimePropertyInt CHUNK_SECTION_MAX = PlazaSlimePropertyInt.create("chunkSectionMax", 19);

    public static final PlazaSlimePropertyInt SEA_LEVEL = PlazaSlimePropertyInt.create("seaLevel", -63);

    public static final PlazaSlimePropertyBoolean SAVE_POI = PlazaSlimePropertyBoolean.create("savePOI", false);
    public static final PlazaSlimePropertyBoolean SAVE_BLOCK_TICKS = PlazaSlimePropertyBoolean.create("saveBlockTicks", false);
    public static final PlazaSlimePropertyBoolean SAVE_FLUID_TICKS = PlazaSlimePropertyBoolean.create("saveFluidTicks", false);

    private PlazaSlimeProperties() {
        throw new AssertionError();
    }
}
