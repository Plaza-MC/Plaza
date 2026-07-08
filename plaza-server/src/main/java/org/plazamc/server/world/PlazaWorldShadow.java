package org.plazamc.server.world;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;

/**
 * Maintains a fake vanilla folder structure for Plaza-managed worlds so that
 * legacy plugins (Multiverse, etc.) can validate worlds using the standard
 * Anvil folder schema. This shadow folder is not needed for the ANVIL format
 * because it already uses vanilla folders; it is used for all other Plaza
 * world formats (SLIME, and future formats like LinearV2 or Polar).
 *
 * <p>If a legacy plugin deletes the shadow folder, Plaza treats that as a
 * request to delete the real world data as well.</p>
 *
 * <p>This compatibility layer is permanent for Bukkit/Anvil plugins. Plaza may
 * expose its own optional world management API for developers who want better
 * control, but the shadow folder will remain so that existing plugins keep
 * working without adaptations.</p>
 */
public final class PlazaWorldShadow {

    private static final String REGION = "region";
    private static final String LEVEL_DAT = "level.dat";

    private PlazaWorldShadow() {
    }

    /**
     * Creates the fake folder structure for the given world if it does not
     * already exist.
     */
    public static void create(final String worldName) {
        final File folder = getShadowFolder(worldName);
        if (folder.isDirectory()) {
            return;
        }

        try {
            Files.createDirectories(folder.toPath());
            final File region = new File(folder, REGION);
            Files.createDirectories(region.toPath());
            final File levelDat = new File(folder, LEVEL_DAT);
            if (!levelDat.exists()) {
                Files.createFile(levelDat.toPath());
            }
        } catch (final IOException ex) {
            Logger.getLogger("Plaza").log(Level.SEVERE, "Could not create shadow folder for world " + worldName, ex);
        }
    }

    /**
     * Deletes the fake folder structure for the given world.
     */
    public static void delete(final String worldName) {
        final File folder = getShadowFolder(worldName);
        if (!folder.exists()) {
            return;
        }

        deleteRecursively(folder);
    }

    /**
     * Returns whether the shadow folder for the given world exists and looks
     * like a valid vanilla world folder.
     */
    public static boolean exists(final String worldName) {
        final File folder = getShadowFolder(worldName);
        if (!folder.isDirectory()) {
            return false;
        }
        return new File(folder, LEVEL_DAT).isFile() && new File(folder, REGION).isDirectory();
    }

    /**
     * Returns the legacy shadow folder location inside Bukkit's world container.
     */
    public static File getShadowFolder(final String worldName) {
        return new File(Bukkit.getWorldContainer(), worldName);
    }

    private static void deleteRecursively(final File file) {
        final File[] children = file.listFiles();
        if (children != null) {
            for (final File child : children) {
                deleteRecursively(child);
            }
        }
        if (!file.delete()) {
            Logger.getLogger("Plaza").warning("Could not delete shadow path: " + file);
        }
    }
}
