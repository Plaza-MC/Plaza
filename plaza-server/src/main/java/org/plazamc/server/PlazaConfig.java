package org.plazamc.server;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Minimal Plaza configuration bootstrap.
 */
public final class PlazaConfig {
    private static final File CONFIG_FILE = new File("plaza.yml");
    private static YamlConfiguration config;

    private PlazaConfig() {
    }

    public static void load() {
        config = YamlConfiguration.loadConfiguration(CONFIG_FILE);
        config.options().header("Plaza configuration");
        config.options().copyDefaults(true);

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

    public static boolean disableVanillaWorldGeneration() {
        return config().getBoolean("plugin-driven.disable-vanilla-world-generation", true);
    }

    public static boolean disableNaturalSpawning() {
        return config().getBoolean("plugin-driven.disable-natural-spawning", true);
    }

    public static boolean disableMobAiByDefault() {
        return config().getBoolean("plugin-driven.disable-mob-ai-by-default", true);
    }

    public static boolean slimeWorldsEnabled() {
        return config().getBoolean("slime-worlds.enabled", true);
    }

    public static String slimeWorldsStorage() {
        return config().getString("slime-worlds.storage", "file");
    }

    public static File slimeWorldsDirectory() {
        return new File(config().getString("slime-worlds.worlds-directory", "slime_worlds"));
    }

    public static String slimeDefaultBiome() {
        return config().getString("slime-worlds.default-biome", "minecraft:plains");
    }

    public static boolean slimeReadOnly() {
        return config().getBoolean("slime-worlds.read-only", false);
    }

    public static boolean slimeSavePoi() {
        return config().getBoolean("slime-worlds.save-poi", true);
    }

    public static boolean slimeSaveBlockTicks() {
        return config().getBoolean("slime-worlds.save-block-ticks", false);
    }

    public static boolean slimeSaveFluidTicks() {
        return config().getBoolean("slime-worlds.save-fluid-ticks", false);
    }

    public static boolean disableDefaultNether() {
        return config().getBoolean("plugin-driven.disable-default-nether", true);
    }

    public static boolean disableDefaultEnd() {
        return config().getBoolean("plugin-driven.disable-default-end", true);
    }

    public static boolean spawnPlatformEnabled() {
        return config().getBoolean("plugin-driven.spawn-platform.enabled", true);
    }

    public static boolean dynamicWorldBorderEnabled() {
        return config().getBoolean("plugin-driven.dynamic-world-border.enabled", true);
    }

    public static int dynamicWorldBorderMargin() {
        return config().getInt("plugin-driven.dynamic-world-border.margin-blocks", 8);
    }

    public static double dynamicWorldBorderMinimumSize() {
        return config().getDouble("plugin-driven.dynamic-world-border.minimum-size", 16.0D);
    }

    public static long dynamicWorldBorderRecalculationIntervalTicks() {
        return config().getLong("plugin-driven.dynamic-world-border.recalculation-interval-ticks", 20L);
    }

    public static int dynamicWorldBorderMaxChunksScanned() {
        return config().getInt("plugin-driven.dynamic-world-border.max-chunks-scanned", 1024);
    }

    private static void addDefaults(final YamlConfiguration config) {
        config.addDefault("config-version", 1);

        config.addDefault("plugin-driven.disable-vanilla-world-generation", true);
        config.addDefault("plugin-driven.disable-natural-spawning", true);
        config.addDefault("plugin-driven.disable-mob-ai-by-default", true);
        config.addDefault("plugin-driven.disable-default-nether", true);
        config.addDefault("plugin-driven.disable-default-end", true);
        config.addDefault("plugin-driven.spawn-platform.enabled", true);
        config.addDefault("plugin-driven.dynamic-world-border.enabled", true);
        config.addDefault("plugin-driven.dynamic-world-border.margin-blocks", 8);
        config.addDefault("plugin-driven.dynamic-world-border.minimum-size", 16.0D);
        config.addDefault("plugin-driven.dynamic-world-border.recalculation-interval-ticks", 20L);
        config.addDefault("plugin-driven.dynamic-world-border.max-chunks-scanned", 1024);

        config.addDefault("slime-worlds.enabled", true);
        config.addDefault("slime-worlds.storage", "file");
        config.addDefault("slime-worlds.worlds-directory", "slime_worlds");
        config.addDefault("slime-worlds.default-biome", "minecraft:plains");
        config.addDefault("slime-worlds.read-only", false);
        config.addDefault("slime-worlds.save-poi", true);
        config.addDefault("slime-worlds.save-block-ticks", false);
        config.addDefault("slime-worlds.save-fluid-ticks", false);
    }

    private static void save() {
        try {
            config.save(CONFIG_FILE);
        } catch (final IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not save plaza.yml", ex);
        }
    }
}
