package org.plazamc.server;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import org.bukkit.Bukkit;

/**
 * Plaza configuration.
 * <p>
 * Backed by BoostedYAML so comments in {@code plaza.yml} are preserved across
 * reloads and automatic updates.
 */
public final class PlazaConfig {
    private static final File CONFIG_FILE = new File("plaza.yml");
    private static final int CURRENT_CONFIG_VERSION = 5;
    private static final String RESOURCE_PATH = "/plaza.yml";
    private static final String WORLDS_RESOURCE_PATH = "/worlds.yml";
    private static final String WORLDS_FILE_NAME = "worlds.yml";
    private static YamlDocument config;
    private static YamlDocument worldsConfig;
    private static File worldsFile;

    private PlazaConfig() {
    }

    public static void load() {
        try {
            final InputStream defaults = PlazaConfig.class.getResourceAsStream(RESOURCE_PATH);
            config = YamlDocument.create(
                CONFIG_FILE,
                defaults,
                GeneralSettings.DEFAULT,
                LoaderSettings.builder().setAutoUpdate(true).build(),
                DumperSettings.DEFAULT,
                UpdaterSettings.builder().setVersioning(new BasicVersioning("config-version")).build()
            );
        } catch (final IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not load plaza.yml", ex);
            throw new RuntimeException("Could not load plaza.yml", ex);
        }

        migrate();
        save();
        loadWorldsConfig();
    }

    public static void reload() {
        if (config == null) {
            load();
            return;
        }

        try {
            config.reload();
            migrate();
            save();
        } catch (final IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not reload plaza.yml", ex);
        }

        reloadWorldsConfig();
    }

    public static YamlDocument config() {
        if (config == null) {
            load();
        }
        return config;
    }

    public static YamlDocument worldsConfig() {
        if (worldsConfig == null) {
            loadWorldsConfig();
        }
        return worldsConfig;
    }

    /**
     * Loads the world list from {@code worlds.yml}, stored inside the folder of
     * the 'file' world source.
     */
    private static void loadWorldsConfig() {
        worldsFile = new File(plazaWorldsFilePath(), WORLDS_FILE_NAME);
        final File parent = worldsFile.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }

        try {
            final InputStream defaults = PlazaConfig.class.getResourceAsStream(WORLDS_RESOURCE_PATH);
            worldsConfig = YamlDocument.create(
                worldsFile,
                defaults,
                GeneralSettings.DEFAULT,
                LoaderSettings.builder().setAutoUpdate(true).build(),
                DumperSettings.DEFAULT,
                UpdaterSettings.builder().setVersioning(new BasicVersioning("config-version")).build()
            );
        } catch (final IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not load " + worldsFile.getPath(), ex);
            throw new RuntimeException("Could not load " + worldsFile.getPath(), ex);
        }

        saveWorlds();
    }

    private static void reloadWorldsConfig() {
        if (worldsConfig == null) {
            loadWorldsConfig();
            return;
        }

        final File configuredFile = new File(plazaWorldsFilePath(), WORLDS_FILE_NAME);
        if (!configuredFile.equals(worldsFile)) {
            // The 'file' source path changed; load the world list from the new folder.
            loadWorldsConfig();
            return;
        }

        try {
            worldsConfig.reload();
        } catch (final IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not reload " + worldsFile.getPath(), ex);
        }
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

    public static boolean disableWorldTickWhenEmpty() {
        return config().getBoolean("plaza-worlds.disable-tick-when-empty", true);
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

    public static Section plazaWorldsSources() {
        return sectionOrCreate("plaza-worlds.sources");
    }

    public static Section plazaWorldsSourceConfig(final String source) {
        return sectionOrCreate("plaza-worlds.sources." + source);
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

    // World list. Lives in worlds.yml inside the 'file' source folder, not in
    // plaza.yml; see loadWorldsConfig().

    public static Section plazaWorldsWorlds() {
        return worldsSectionOrCreate("worlds");
    }

    public static Section plazaWorldsWorldConfig(final String worldName) {
        return worldsSectionOrCreate("worlds." + worldName);
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
        final Section worldSection = plazaWorldsWorldConfig(worldName);
        worldSection.set("format", format.toUpperCase());
        if (source != null && !source.isBlank()) {
            worldSection.set("source", source);
        }
        worldSection.set("load-on-startup", true);
        worldSection.set("read-only", false);
        saveWorlds();
        Bukkit.getLogger().info("Added world '" + worldName + "' to " + worldsFile.getPath() + " (format: " + format.toUpperCase() + ").");
    }

    public static void removePlazaWorld(final String worldName) {
        worldsConfig().remove("worlds." + worldName);
        saveWorlds();
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

    public static boolean disableAmbientParticles() {
        return config().getBoolean("tick-control.disable-ambient-particles", true);
    }

    public static boolean onlyTickItemsInHand() {
        return config().getBoolean("tick-control.only-tick-items-in-hand", true);
    }

    public static boolean sleepingBlockEntities() {
        return config().getBoolean("tick-control.sleeping-block-entities", true);
    }

    // Player

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

    // Achievements

    public static boolean disableAdvancementLoading() {
        return config().getBoolean("achievements.disable-advancement-loading", true);
    }

    public static boolean disableStatisticsTracking() {
        return config().getBoolean("achievements.disable-statistics-tracking", true);
    }

    // Physics

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
        // The BoostedYAML updater will copy missing keys from the bundled defaults and
        // preserve user edits/comments where possible.
        config.set("config-version", CURRENT_CONFIG_VERSION);
        try {
            config.update();
        } catch (final IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not update plaza.yml to version " + CURRENT_CONFIG_VERSION, ex);
        }
        Bukkit.getLogger().info("Migrated plaza.yml to version " + CURRENT_CONFIG_VERSION);
    }

    private static Section sectionOrCreate(final String route) {
        Section section = config().getSection(route);
        if (section == null) {
            section = config().createSection(route);
        }
        return section;
    }

    private static Section worldsSectionOrCreate(final String route) {
        Section section = worldsConfig().getSection(route);
        if (section == null) {
            section = worldsConfig().createSection(route);
        }
        return section;
    }

    public static void save() {
        try {
            config.save();
        } catch (final IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not save plaza.yml", ex);
        }
    }

    public static void saveWorlds() {
        try {
            worldsConfig.save();
        } catch (final IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not save " + worldsFile.getPath(), ex);
        }
    }
}
