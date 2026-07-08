package org.plazamc.server.world.loader;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.plazamc.api.world.PlazaWorldLoader;
import org.plazamc.server.PlazaConfig;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Registry of configured Plaza world data sources.
 */
public final class PlazaWorldSourceRegistry {

    private static final Logger LOGGER = Logger.getLogger("Plaza");
    private static final Map<String, PlazaWorldLoader> LOADERS = new HashMap<>();

    private PlazaWorldSourceRegistry() {
    }

    public static void load() {
        LOADERS.clear();

        ConfigurationSection sources = PlazaConfig.plazaWorldsSources();
        for (String sourceName : sources.getKeys(false)) {
            ConfigurationSection section = sources.getConfigurationSection(sourceName);
            if (section == null) {
                continue;
            }

            if (!section.getBoolean("enabled", true)) {
                continue;
            }

            String type = section.getString("type", sourceName).toLowerCase();
            PlazaWorldLoader loader = createLoader(sourceName, type, section);
            if (loader != null) {
                LOADERS.put(sourceName, loader);
                LOGGER.info("Registered Plaza world source '" + sourceName + "' (type: " + type + ")");
            }
        }
    }

    public static void reload() {
        load();
    }

    @Nullable
    public static PlazaWorldLoader getLoader(@NotNull String name) {
        return LOADERS.get(name.toLowerCase());
    }

    @NotNull
    public static PlazaWorldLoader getDefaultLoader() {
        PlazaWorldLoader loader = getLoader(PlazaConfig.plazaWorldsDefaultSource());
        if (loader == null) {
            throw new IllegalStateException("Default Plaza world source '" + PlazaConfig.plazaWorldsDefaultSource() + "' is not configured");
        }
        return loader;
    }

    @Nullable
    private static PlazaWorldLoader createLoader(String sourceName, String type, ConfigurationSection section) {
        return switch (type) {
            case "file" -> new PlazaFileWorldLoader(new File(section.getString("path", "plaza_worlds")));
            case "mysql" -> {
                LOGGER.warning("MySQL world source is not implemented yet: " + sourceName);
                yield null;
            }
            case "mongodb" -> {
                LOGGER.warning("MongoDB world source is not implemented yet: " + sourceName);
                yield null;
            }
            default -> {
                LOGGER.warning("Unknown Plaza world source type '" + type + "' for source " + sourceName);
                yield null;
            }
        };
    }
}
