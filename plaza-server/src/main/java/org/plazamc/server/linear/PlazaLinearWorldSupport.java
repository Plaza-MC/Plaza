package org.plazamc.server.linear;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.plazamc.server.PlazaConfig;

/**
 * Decides, per world storage, whether regions are stored as vanilla {@code .mca}
 * or Linear {@code .linear} files, and acts as the factory for region files.
 * <p>
 * A world uses the Linear format when its configured format is LINEAR (see
 * {@code worlds.yml}; worlds without an entry use {@code plaza-worlds.default-format}),
 * or when its storage folder already contains {@code .linear} files, so worlds
 * copied from another server keep working without config changes.
 */
public final class PlazaLinearWorldSupport {

    public static final String LINEAR_EXTENSION = ".linear";
    public static final String ANVIL_EXTENSION = ".mca";

    private PlazaLinearWorldSupport() {
    }

    public static boolean useLinearRegionFormat(final RegionStorageInfo info, final Path folder) {
        if (containsLinearFiles(folder)) {
            return true;
        }
        return "LINEAR".equalsIgnoreCase(worldFormat(info.level()));
    }

    /**
     * Reads the configured format of a world without creating a config entry
     * (unlike {@link PlazaConfig#plazaWorldsWorldFormat(String)}). This runs on
     * internal world names too (e.g. the Slime bootstrap's temporary level id),
     * which must not end up registered in worlds.yml.
     */
    private static String worldFormat(final String worldName) {
        final Section section = PlazaConfig.worldsConfig().getSection("worlds." + worldName);
        if (section == null) {
            return PlazaConfig.worldDefaultFormat();
        }
        return section.getString("format", PlazaConfig.worldDefaultFormat()).toUpperCase();
    }

    public static IRegionFile createRegionFile(final RegionStorageInfo info, final Path file, final Path folder,
                                               final boolean sync, final boolean linear) throws IOException {
        if (linear) {
            return new LinearRegionFile(info, file, folder, sync);
        }
        return new RegionFile(info, file, folder, sync);
    }

    private static boolean containsLinearFiles(final Path folder) {
        if (!Files.isDirectory(folder)) {
            return false;
        }
        try (final DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*" + LINEAR_EXTENSION)) {
            return stream.iterator().hasNext();
        } catch (final IOException ex) {
            return false;
        }
    }
}
