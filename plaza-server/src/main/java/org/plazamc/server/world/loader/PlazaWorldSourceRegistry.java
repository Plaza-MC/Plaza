package org.plazamc.server.world.loader;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.plazamc.api.world.PlazaWorldLoader;
import org.plazamc.server.PlazaConfig;
import org.plazamc.server.world.loader.mongodb.PlazaMongoWorldLoader;
import org.plazamc.server.world.loader.sql.PlazaSqlWorldLoader;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
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

        Section sources = PlazaConfig.plazaWorldsSources();
        for (String sourceName : sources.getRoutesAsStrings(false)) {
            Section section = sources.getSection(sourceName);
            if (section == null) {
                continue;
            }

            String type = section.getString("type", sourceName).toLowerCase();
            // Database sources default to disabled unless explicitly enabled.
            boolean defaultEnabled = switch (type) {
                case "sql", "mongodb" -> false;
                default -> true;
            };
            if (!section.getBoolean("enabled", defaultEnabled)) {
                continue;
            }

            try {
                PlazaWorldLoader loader = createLoader(sourceName, type);
                if (loader != null) {
                    LOADERS.put(sourceName.toLowerCase(), loader);
                    LOGGER.info("Registered Plaza world source '" + sourceName + "' (type: " + type + ")");
                }
            } catch (final Exception ex) {
                LOGGER.log(Level.SEVERE, "Could not initialize Plaza world source '" + sourceName + "' (type: " + type + "). Skipping.", ex);
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
    private static PlazaWorldLoader createLoader(String sourceName, String type) {
        return switch (type) {
            case "file" -> new PlazaFileWorldLoader(new File(PlazaConfig.plazaWorldsSourceConfig(sourceName).getString("path", "plaza_worlds")));
            case "sql" -> new PlazaSqlWorldLoader(sourceName);
            case "mongodb" -> new PlazaMongoWorldLoader(sourceName);
            default -> {
                LOGGER.warning("Unknown Plaza world source type '" + type + "' for source " + sourceName);
                yield null;
            }
        };
    }
}
