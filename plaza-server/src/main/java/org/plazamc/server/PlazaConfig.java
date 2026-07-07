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

    public static boolean disableDefaultNether() {
        return config().getBoolean("plugin-driven.disable-default-nether", true);
    }

    public static boolean disableDefaultEnd() {
        return config().getBoolean("plugin-driven.disable-default-end", true);
    }

    public static boolean spawnPlatformEnabled() {
        return config().getBoolean("plugin-driven.spawn-platform.enabled", true);
    }

    private static void addDefaults(final YamlConfiguration config) {
        config.addDefault("config-version", 1);

        config.addDefault("plugin-driven.disable-vanilla-world-generation", true);
        config.addDefault("plugin-driven.disable-natural-spawning", true);
        config.addDefault("plugin-driven.disable-mob-ai-by-default", true);
        config.addDefault("plugin-driven.disable-default-nether", true);
        config.addDefault("plugin-driven.disable-default-end", true);
        config.addDefault("plugin-driven.spawn-platform.enabled", true);

        config.addDefault("slime-worlds.enabled", true);
        config.addDefault("slime-worlds.storage", "file");
        config.addDefault("slime-worlds.worlds-directory", "slime_worlds");
    }

    private static void save() {
        try {
            config.save(CONFIG_FILE);
        } catch (final IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not save plaza.yml", ex);
        }
    }
}
