package org.plazamc.server.world;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility to back up Plaza world files before destructive operations.
 */
public final class PlazaWorldBackup {

    private static final Logger LOGGER = Logger.getLogger("Plaza");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    private PlazaWorldBackup() {
        throw new AssertionError();
    }

    /**
     * Creates a backup of the given file inside the {@code plaza_backups} folder.
     *
     * @param source   The file to back up.
     * @param worldName The world name to include in the backup file name.
     * @return The backup file, or {@code null} if the source does not exist.
     * @throws IOException if the backup cannot be created.
     */
    public static File backupFile(File source, String worldName) throws IOException {
        if (!source.exists()) {
            return null;
        }

        File backupDir = new File("plaza_backups");
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            throw new IOException("Could not create backup directory " + backupDir);
        }

        String timestamp = DATE_FORMAT.format(new Date());
        File backupFile = new File(backupDir, worldName + "_" + timestamp + ".slime");

        int suffix = 1;
        while (backupFile.exists()) {
            backupFile = new File(backupDir, worldName + "_" + timestamp + "_" + suffix + ".slime");
            suffix++;
        }

        Files.copy(source.toPath(), backupFile.toPath());
        LOGGER.info("Created backup of " + source + " at " + backupFile);
        return backupFile;
    }
}
