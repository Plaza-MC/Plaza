package org.plazamc.server;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Plaza configuration.
 */
public final class PlazaConfig {
    private static final File CONFIG_FILE = new File("plaza.yml");
    private static final int CURRENT_CONFIG_VERSION = 3;
    private static YamlConfiguration config;

    private PlazaConfig() {
    }

    public static void load() {
        config = YamlConfiguration.loadConfiguration(CONFIG_FILE);
        config.options().header("Plaza configuration");
        config.options().copyDefaults(true);

        migrate();
        addDefaults(config);
        save();
    }

    public static void reload() {
        load();
    }

    public static YamlConfiguration config() {
        if (config == null) {
            load();
        }
        return config;
    }

    // World format configuration

    public static String worldDefaultFormat() {
        return config().getString("world.default-format", "SLIME").toUpperCase();
    }

    public static boolean spawnPlatformEnabled() {
        return config().getBoolean("world.spawn-platform.enabled", true);
    }

    public static boolean dynamicWorldBorderEnabled() {
        return config().getBoolean("world.dynamic-world-border.enabled", true);
    }

    public static int dynamicWorldBorderMargin() {
        return config().getInt("world.dynamic-world-border.margin-blocks", 8);
    }

    public static double dynamicWorldBorderMinimumSize() {
        return config().getDouble("world.dynamic-world-border.minimum-size", 16.0D);
    }

    public static long dynamicWorldBorderRecalculationIntervalTicks() {
        return config().getLong("world.dynamic-world-border.recalculation-interval-ticks", 20L);
    }

    public static int dynamicWorldBorderMaxChunksScanned() {
        return config().getInt("world.dynamic-world-border.max-chunks-scanned", 1024);
    }

    // Plaza worlds (global data sources and per-world settings)

    public static String plazaWorldsDefaultSource() {
        return config().getString("plaza-worlds.default-source", "file").toLowerCase();
    }

    public static ConfigurationSection plazaWorldsSources() {
        ConfigurationSection section = config().getConfigurationSection("plaza-worlds.sources");
        if (section == null) {
            section = config().createSection("plaza-worlds.sources");
        }
        return section;
    }

    public static ConfigurationSection plazaWorldsSourceConfig(final String source) {
        ConfigurationSection section = plazaWorldsSources().getConfigurationSection(source);
        if (section == null) {
            section = plazaWorldsSources().createSection(source);
        }
        return section;
    }

    public static boolean plazaWorldsSourceEnabled(final String source) {
        return plazaWorldsSourceConfig(source).getBoolean("enabled", true);
    }

    public static String plazaWorldsSourceType(final String source) {
        return plazaWorldsSourceConfig(source).getString("type", source).toLowerCase();
    }

    public static File plazaWorldsFilePath() {
        return new File(plazaWorldsSourceConfig("file").getString("path", "plaza_worlds"));
    }

    public static ConfigurationSection plazaWorldsWorlds() {
        ConfigurationSection section = config().getConfigurationSection("plaza-worlds.worlds");
        if (section == null) {
            section = config().createSection("plaza-worlds.worlds");
        }
        return section;
    }

    public static ConfigurationSection plazaWorldsWorldConfig(final String worldName) {
        ConfigurationSection section = plazaWorldsWorlds().getConfigurationSection(worldName);
        if (section == null) {
            section = plazaWorldsWorlds().createSection(worldName);
        }
        return section;
    }

    public static String plazaWorldsWorldSource(final String worldName) {
        return plazaWorldsWorldConfig(worldName).getString("source", plazaWorldsDefaultSource()).toLowerCase();
    }

    public static boolean plazaWorldsWorldLoadOnStartup(final String worldName) {
        return plazaWorldsWorldConfig(worldName).getBoolean("load-on-startup", true);
    }

    public static boolean plazaWorldsWorldReadOnly(final String worldName) {
        return plazaWorldsWorldConfig(worldName).getBoolean("read-only", false);
    }

    // Legacy Slime format configuration (kept for format-specific defaults)

    public static boolean slimeWorldsEnabled() {
        return "SLIME".equalsIgnoreCase(worldDefaultFormat());
    }

    public static String slimeDefaultBiome() {
        return config().getString("world.formats.slime.default-biome", "minecraft:plains");
    }

    public static boolean slimeReadOnly() {
        return config().getBoolean("world.formats.slime.read-only", false);
    }

    public static boolean slimeSavePoi() {
        return config().getBoolean("world.formats.slime.save-poi", true);
    }

    public static boolean slimeSaveBlockTicks() {
        return config().getBoolean("world.formats.slime.save-block-ticks", false);
    }

    public static boolean slimeSaveFluidTicks() {
        return config().getBoolean("world.formats.slime.save-fluid-ticks", false);
    }

    public static boolean anvilWorldsEnabled() {
        return config().getBoolean("world.formats.anvil.enabled", false);
    }

    // Tick control

    public static boolean disableNaturalSpawning() {
        return config().getBoolean("tick-control.disable-natural-spawning", true);
    }

    public static boolean disableMobAiByDefault() {
        return config().getBoolean("tick-control.disable-mob-ai-by-default", true);
    }

    public static boolean disableMobPathfinding() {
        return config().getBoolean("tick-control.disable-mob-pathfinding", true);
    }

    public static boolean disableMobBreeding() {
        return config().getBoolean("tick-control.disable-mob-breeding", true);
    }

    public static boolean disableItemDespawn() {
        return config().getBoolean("tick-control.disable-item-despawn", true);
    }

    public static boolean disableWeatherCycle() {
        return config().getBoolean("tick-control.disable-weather-cycle", true);
    }

    public static boolean disableDaylightCycle() {
        return config().getBoolean("tick-control.disable-daylight-cycle", true);
    }

    public static boolean disableCropGrowth() {
        return config().getBoolean("tick-control.disable-crop-growth", true);
    }

    public static boolean disablePlantGrowth() {
        return config().getBoolean("tick-control.disable-plant-growth", true);
    }

    public static boolean disableLeafDecay() {
        return config().getBoolean("tick-control.disable-leaf-decay", true);
    }

    public static boolean disableFireSpread() {
        return config().getBoolean("tick-control.disable-fire-spread", true);
    }

    public static boolean disableLavaFlow() {
        return config().getBoolean("tick-control.disable-lava-flow", true);
    }

    public static boolean disableWaterFlow() {
        return config().getBoolean("tick-control.disable-water-flow", true);
    }

    public static boolean disableRedstoneUpdates() {
        return config().getBoolean("tick-control.disable-redstone-updates", true);
    }

    public static boolean disableHopperTick() {
        return config().getBoolean("tick-control.disable-hopper-tick", true);
    }

    public static boolean disablePistonUpdate() {
        return config().getBoolean("tick-control.disable-piston-update", true);
    }

    // Player (WIP)

    public static boolean disableHunger() {
        return config().getBoolean("player.disable-hunger", true);
    }

    public static boolean disableHealthRegeneration() {
        return config().getBoolean("player.disable-health-regeneration", true);
    }

    public static boolean disableExperienceOrbMerge() {
        return config().getBoolean("player.disable-experience-orb-merge", true);
    }

    public static boolean disableItemPickupDelay() {
        return config().getBoolean("player.disable-item-pickup-delay", true);
    }

    // Achievements (WIP)

    public static boolean disableAdvancementLoading() {
        return config().getBoolean("achievements.disable-advancement-loading", true);
    }

    public static boolean disableStatisticsTracking() {
        return config().getBoolean("achievements.disable-statistics-tracking", true);
    }

    // Physics (WIP)

    public static boolean disableGravityEntities() {
        return config().getBoolean("physics.disable-gravity-entities", true);
    }

    public static boolean disableFallingBlocks() {
        return config().getBoolean("physics.disable-falling-blocks", true);
    }

    public static boolean disableTntPhysics() {
        return config().getBoolean("physics.disable-tnt-physics", true);
    }

    private static void migrate() {
        final int version = config.getInt("config-version", 1);
        if (version >= CURRENT_CONFIG_VERSION) {
            return;
        }

        if (version == 1) {
            moveConfigKey("plugin-driven.disable-natural-spawning", "tick-control.disable-natural-spawning");
            moveConfigKey("plugin-driven.disable-mob-ai-by-default", "tick-control.disable-mob-ai-by-default");
            moveConfigKey("plugin-driven.spawn-platform", "world.spawn-platform");
            moveConfigKey("plugin-driven.dynamic-world-border", "world.dynamic-world-border");
            config.set("plugin-driven", null);

            if (config.contains("slime-worlds")) {
                config.set("world.formats.slime", config.getConfigurationSection("slime-worlds"));
                config.set("slime-worlds", null);
            }
        }

        if (version <= 2) {
            moveConfigKey("disabled-features.disable-natural-spawning", "tick-control.disable-natural-spawning");
            moveConfigKey("disabled-features.disable-mob-ai-by-default", "tick-control.disable-mob-ai-by-default");
            moveConfigKey("disabled-features.spawn-platform", "world.spawn-platform");
            moveConfigKey("disabled-features.dynamic-world-border", "world.dynamic-world-border");
            config.set("disabled-features.disable-vanilla-world-generation", null);
            config.set("disabled-features.disable-default-nether", null);
            config.set("disabled-features.disable-default-end", null);
            config.set("disabled-features", null);

            // Migrate legacy slime-worlds directory into the global plaza-worlds file source.
            if (config.contains("world.formats.slime.worlds-directory")) {
                final String legacyPath = config.getString("world.formats.slime.worlds-directory", "slime_worlds");
                config.set("plaza-worlds.sources.file.type", "file");
                config.set("plaza-worlds.sources.file.path", legacyPath);
                config.set("world.formats.slime.worlds-directory", null);
            }

            // Old storage key used a plain string; migrate to the new source name.
            if (config.contains("world.formats.slime.storage")) {
                final String legacyStorage = config.getString("world.formats.slime.storage", "file").toLowerCase();
                config.set("plaza-worlds.default-source", legacyStorage);
                config.set("world.formats.slime.storage", null);
            }
        }

        config.set("config-version", CURRENT_CONFIG_VERSION);
        Bukkit.getLogger().info("Migrated plaza.yml to version " + CURRENT_CONFIG_VERSION);
    }

    private static void moveConfigKey(final String from, final String to) {
        if (config.contains(from)) {
            config.set(to, config.get(from));
            config.set(from, null);
        }
    }

    private static void addDefaults(final YamlConfiguration config) {
        config.addDefault("config-version", CURRENT_CONFIG_VERSION);

        // World formats
        config.addDefault("world.default-format", "SLIME");

        config.addDefault("world.formats.slime.default-biome", "minecraft:plains");
        config.addDefault("world.formats.slime.read-only", false);
        config.addDefault("world.formats.slime.save-poi", true);
        config.addDefault("world.formats.slime.save-block-ticks", false);
        config.addDefault("world.formats.slime.save-fluid-ticks", false);

        config.addDefault("world.formats.anvil.enabled", false);

        // World defaults (not disabled features)
        config.addDefault("world.spawn-platform.enabled", true);
        config.addDefault("world.dynamic-world-border.enabled", true);
        config.addDefault("world.dynamic-world-border.margin-blocks", 8);
        config.addDefault("world.dynamic-world-border.minimum-size", 16.0D);
        config.addDefault("world.dynamic-world-border.recalculation-interval-ticks", 20L);
        config.addDefault("world.dynamic-world-border.max-chunks-scanned", 1024);

        // Global Plaza world sources and worlds
        config.addDefault("plaza-worlds.default-source", "file");

        config.addDefault("plaza-worlds.sources.file.type", "file");
        config.addDefault("plaza-worlds.sources.file.path", "plaza_worlds");

        config.addDefault("plaza-worlds.sources.mysql.type", "mysql");
        config.addDefault("plaza-worlds.sources.mysql.enabled", false);
        config.addDefault("plaza-worlds.sources.mysql.host", "127.0.0.1");
        config.addDefault("plaza-worlds.sources.mysql.port", 3306);
        config.addDefault("plaza-worlds.sources.mysql.database", "plazamc");
        config.addDefault("plaza-worlds.sources.mysql.username", "plazamc");
        config.addDefault("plaza-worlds.sources.mysql.password", "");
        config.addDefault("plaza-worlds.sources.mysql.use-ssl", false);
        config.addDefault("plaza-worlds.sources.mysql.table", "worlds");

        config.addDefault("plaza-worlds.sources.mongodb.type", "mongodb");
        config.addDefault("plaza-worlds.sources.mongodb.enabled", false);
        config.addDefault("plaza-worlds.sources.mongodb.host", "127.0.0.1");
        config.addDefault("plaza-worlds.sources.mongodb.port", 27017);
        config.addDefault("plaza-worlds.sources.mongodb.database", "plazamc");
        config.addDefault("plaza-worlds.sources.mongodb.collection", "worlds");
        config.addDefault("plaza-worlds.sources.mongodb.username", "");
        config.addDefault("plaza-worlds.sources.mongodb.password", "");
        config.addDefault("plaza-worlds.sources.mongodb.auth-source", "admin");
        config.addDefault("plaza-worlds.sources.mongodb.uri", "");

        config.addDefault("plaza-worlds.worlds.world.source", "file");
        config.addDefault("plaza-worlds.worlds.world.load-on-startup", true);
        config.addDefault("plaza-worlds.worlds.world.read-only", false);

        // Tick control (WIP)
        config.addDefault("tick-control.disable-natural-spawning", true);
        config.addDefault("tick-control.disable-mob-ai-by-default", true);
        config.addDefault("tick-control.disable-mob-pathfinding", true);
        config.addDefault("tick-control.disable-mob-breeding", true);
        config.addDefault("tick-control.disable-item-despawn", true);
        config.addDefault("tick-control.disable-weather-cycle", true);
        config.addDefault("tick-control.disable-daylight-cycle", true);
        config.addDefault("tick-control.disable-crop-growth", true);
        config.addDefault("tick-control.disable-plant-growth", true);
        config.addDefault("tick-control.disable-leaf-decay", true);
        config.addDefault("tick-control.disable-fire-spread", true);
        config.addDefault("tick-control.disable-lava-flow", true);
        config.addDefault("tick-control.disable-water-flow", true);
        config.addDefault("tick-control.disable-redstone-updates", true);
        config.addDefault("tick-control.disable-hopper-tick", true);
        config.addDefault("tick-control.disable-piston-update", true);

        // Player (WIP)
        config.addDefault("player.disable-hunger", true);
        config.addDefault("player.disable-health-regeneration", true);
        config.addDefault("player.disable-experience-orb-merge", true);
        config.addDefault("player.disable-item-pickup-delay", true);

        // Achievements (WIP)
        config.addDefault("achievements.disable-advancement-loading", true);
        config.addDefault("achievements.disable-statistics-tracking", true);

        // Physics (WIP)
        config.addDefault("physics.disable-gravity-entities", true);
        config.addDefault("physics.disable-falling-blocks", true);
        config.addDefault("physics.disable-tnt-physics", true);
    }

    private static void save() {
        try {
            config.save(CONFIG_FILE);
        } catch (final IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not save plaza.yml", ex);
        }
    }
}
