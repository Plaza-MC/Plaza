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
    private static final int CURRENT_CONFIG_VERSION = 4;
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

    // Plaza worlds configuration (all world-related settings live here)

    public static String worldDefaultFormat() {
        return config().getString("plaza-worlds.default-format", "SLIME").toUpperCase();
    }

    public static String plazaWorldsDefaultSource() {
        return config().getString("plaza-worlds.default-source", "file").toLowerCase();
    }

    public static boolean spawnPlatformEnabled() {
        return config().getBoolean("plaza-worlds.spawn-platform.enabled", true);
    }

    public static boolean dynamicWorldBorderEnabled() {
        return config().getBoolean("plaza-worlds.dynamic-world-border.enabled", true);
    }

    public static int dynamicWorldBorderMargin() {
        return config().getInt("plaza-worlds.dynamic-world-border.margin-blocks", 8);
    }

    public static double dynamicWorldBorderMinimumSize() {
        return config().getDouble("plaza-worlds.dynamic-world-border.minimum-size", 16.0D);
    }

    public static long dynamicWorldBorderRecalculationIntervalTicks() {
        return config().getLong("plaza-worlds.dynamic-world-border.recalculation-interval-ticks", 20L);
    }

    public static int dynamicWorldBorderMaxChunksScanned() {
        return config().getInt("plaza-worlds.dynamic-world-border.max-chunks-scanned", 1024);
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

    // SQL source helpers

    public static String plazaWorldsSqlDialect(final String source) {
        return plazaWorldsSourceConfig(source).getString("dialect", "mysql").toLowerCase();
    }

    public static String plazaWorldsSqlHost(final String source) {
        return plazaWorldsSourceConfig(source).getString("host", "127.0.0.1");
    }

    public static int plazaWorldsSqlPort(final String source) {
        return plazaWorldsSourceConfig(source).getInt("port", 3306);
    }

    public static String plazaWorldsSqlDatabase(final String source) {
        return plazaWorldsSourceConfig(source).getString("database", "plazamc");
    }

    public static String plazaWorldsSqlUsername(final String source) {
        return plazaWorldsSourceConfig(source).getString("username", "plazamc");
    }

    public static String plazaWorldsSqlPassword(final String source) {
        return plazaWorldsSourceConfig(source).getString("password", "");
    }

    public static boolean plazaWorldsSqlUseSsl(final String source) {
        return plazaWorldsSourceConfig(source).getBoolean("use-ssl", false);
    }

    public static String plazaWorldsSqlTable(final String source) {
        return plazaWorldsSourceConfig(source).getString("table", "worlds");
    }

    // MongoDB source helpers

    public static String plazaWorldsMongoUri(final String source) {
        return plazaWorldsSourceConfig(source).getString("uri", "");
    }

    public static String plazaWorldsMongoHost(final String source) {
        return plazaWorldsSourceConfig(source).getString("host", "127.0.0.1");
    }

    public static int plazaWorldsMongoPort(final String source) {
        return plazaWorldsSourceConfig(source).getInt("port", 27017);
    }

    public static String plazaWorldsMongoDatabase(final String source) {
        return plazaWorldsSourceConfig(source).getString("database", "plazamc");
    }

    public static String plazaWorldsMongoCollection(final String source) {
        return plazaWorldsSourceConfig(source).getString("collection", "worlds");
    }

    public static String plazaWorldsMongoUsername(final String source) {
        return plazaWorldsSourceConfig(source).getString("username", "");
    }

    public static String plazaWorldsMongoPassword(final String source) {
        return plazaWorldsSourceConfig(source).getString("password", "");
    }

    public static String plazaWorldsMongoAuthSource(final String source) {
        return plazaWorldsSourceConfig(source).getString("auth-source", "admin");
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

    public static String plazaWorldsWorldFormat(final String worldName) {
        return plazaWorldsWorldConfig(worldName).getString("format", worldDefaultFormat()).toUpperCase();
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

    public static void addPlazaWorld(final String worldName, final String format, final String source) {
        final String path = "plaza-worlds.worlds." + worldName;
        config().set(path + ".format", format.toUpperCase());
        if (source != null && !source.isBlank()) {
            config().set(path + ".source", source);
        }
        config().set(path + ".load-on-startup", true);
        config().set(path + ".read-only", false);
        save();
        Bukkit.getLogger().info("Added world '" + worldName + "' to plaza.yml (format: " + format.toUpperCase() + ").");
    }

    // Format-specific configuration

    public static boolean slimeWorldsEnabled() {
        return "SLIME".equalsIgnoreCase(worldDefaultFormat());
    }

    public static String slimeDefaultBiome() {
        return config().getString("plaza-worlds.formats.slime.default-biome", "minecraft:plains");
    }

    public static boolean slimeReadOnly() {
        return config().getBoolean("plaza-worlds.formats.slime.read-only", false);
    }

    public static boolean slimeSavePoi() {
        return config().getBoolean("plaza-worlds.formats.slime.save-poi", true);
    }

    public static boolean slimeSaveBlockTicks() {
        return config().getBoolean("plaza-worlds.formats.slime.save-block-ticks", false);
    }

    public static boolean slimeSaveFluidTicks() {
        return config().getBoolean("plaza-worlds.formats.slime.save-fluid-ticks", false);
    }

    public static boolean anvilWorldsEnabled() {
        return config().getBoolean("plaza-worlds.formats.anvil.enabled", false);
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

        // Plaza is in intensive development; older configs are reset to the new structure.
        // We only bump the version so defaults are rewritten on save.
        config.set("config-version", CURRENT_CONFIG_VERSION);
        Bukkit.getLogger().info("Migrated plaza.yml to version " + CURRENT_CONFIG_VERSION);
    }

    private static void addDefaults(final YamlConfiguration config) {
        config.addDefault("config-version", CURRENT_CONFIG_VERSION);

        // Plaza worlds: all world-related configuration lives here.
        config.addDefault("plaza-worlds.default-format", "SLIME");
        config.addDefault("plaza-worlds.default-source", "file");

        config.addDefault("plaza-worlds.formats.slime.default-biome", "minecraft:plains");
        config.addDefault("plaza-worlds.formats.slime.read-only", false);
        config.addDefault("plaza-worlds.formats.slime.save-poi", true);
        config.addDefault("plaza-worlds.formats.slime.save-block-ticks", false);
        config.addDefault("plaza-worlds.formats.slime.save-fluid-ticks", false);

        config.addDefault("plaza-worlds.formats.anvil.enabled", false);

        config.addDefault("plaza-worlds.spawn-platform.enabled", true);
        config.addDefault("plaza-worlds.dynamic-world-border.enabled", true);
        config.addDefault("plaza-worlds.dynamic-world-border.margin-blocks", 8);
        config.addDefault("plaza-worlds.dynamic-world-border.minimum-size", 16.0D);
        config.addDefault("plaza-worlds.dynamic-world-border.recalculation-interval-ticks", 20L);
        config.addDefault("plaza-worlds.dynamic-world-border.max-chunks-scanned", 1024);

        // Global Plaza world sources. Source keys are arbitrary; the 'type' field selects the backend.
        config.addDefault("plaza-worlds.sources.file.type", "file");
        config.addDefault("plaza-worlds.sources.file.path", "plaza_worlds");

        config.addDefault("plaza-worlds.sources.sql.type", "sql");
        config.addDefault("plaza-worlds.sources.sql.enabled", false);
        config.addDefault("plaza-worlds.sources.sql.dialect", "mysql");
        config.addDefault("plaza-worlds.sources.sql.host", "127.0.0.1");
        config.addDefault("plaza-worlds.sources.sql.port", 3306);
        config.addDefault("plaza-worlds.sources.sql.database", "plazamc");
        config.addDefault("plaza-worlds.sources.sql.username", "plazamc");
        config.addDefault("plaza-worlds.sources.sql.password", "");
        config.addDefault("plaza-worlds.sources.sql.use-ssl", false);
        config.addDefault("plaza-worlds.sources.sql.table", "worlds");

        config.addDefault("plaza-worlds.sources.mongodb.type", "mongodb");
        config.addDefault("plaza-worlds.sources.mongodb.enabled", false);
        config.addDefault("plaza-worlds.sources.mongodb.uri", "");
        config.addDefault("plaza-worlds.sources.mongodb.host", "127.0.0.1");
        config.addDefault("plaza-worlds.sources.mongodb.port", 27017);
        config.addDefault("plaza-worlds.sources.mongodb.database", "plazamc");
        config.addDefault("plaza-worlds.sources.mongodb.collection", "worlds");
        config.addDefault("plaza-worlds.sources.mongodb.username", "");
        config.addDefault("plaza-worlds.sources.mongodb.password", "");
        config.addDefault("plaza-worlds.sources.mongodb.auth-source", "admin");

        config.addDefault("plaza-worlds.worlds.world.format", "SLIME");
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

    public static void save() {
        try {
            config.save(CONFIG_FILE);
        } catch (final IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not save plaza.yml", ex);
        }
    }
}
