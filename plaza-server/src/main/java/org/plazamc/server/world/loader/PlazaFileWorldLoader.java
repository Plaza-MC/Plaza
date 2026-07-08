package org.plazamc.server.world.loader;

import org.jetbrains.annotations.NotNull;
import org.plazamc.api.exceptions.UnknownWorldException;
import org.plazamc.api.world.PlazaWorld;
import org.plazamc.api.world.PlazaWorldLoader;
import org.plazamc.api.world.PlazaWorldPropertyMap;
import org.plazamc.server.slime.PlazaSlimeWorld;
import org.plazamc.server.slime.format.PlazaSlimeSerializer;
import org.plazamc.server.slime.format.reader.PlazaSlimeWorldReaderRegistry;
import org.plazamc.server.slime.loader.PlazaSlimeLoader;
import org.plazamc.server.slime.properties.PlazaSlimePropertyMap;
import org.plazamc.server.world.PlazaWorldBackup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * File-based world loader. Currently handles the Slime format; future formats
 * will be detected from the file header.
 */
public class PlazaFileWorldLoader implements PlazaSlimeLoader {

    private static final FilenameFilter WORLD_FILE_FILTER = (dir, name) -> name.endsWith(".slime");
    private static final Logger LOGGER = Logger.getLogger("Plaza-Slime");

    private final File worldDir;

    public PlazaFileWorldLoader(File worldDir) throws IllegalStateException {
        this.worldDir = worldDir;

        if (worldDir.exists() && !worldDir.isDirectory()) {
            LOGGER.log(Level.WARNING, "A file named ''{0}'' has been deleted, as this is the name used for the worlds directory.", worldDir.getName());
            if (!worldDir.delete()) {
                throw new IllegalStateException("Failed to delete the file named '" + worldDir.getName() + "'.");
            }
        }

        if (!worldDir.exists() && !worldDir.mkdirs()) {
            throw new IllegalStateException("Failed to create the worlds directory.");
        }
    }

    @Override
    public byte[] readWorldBytes(String worldName) throws UnknownWorldException, IOException {
        if (!worldExists(worldName)) {
            throw new UnknownWorldException(worldName);
        }

        try (FileInputStream fis = new FileInputStream(new File(worldDir, worldName + ".slime"))) {
            return fis.readAllBytes();
        }
    }

    @Override
    public boolean worldExists(String worldName) {
        return new File(worldDir, worldName + ".slime").exists();
    }

    @Override
    @NotNull
    public List<String> listWorlds() throws NotDirectoryException {
        String[] worlds = worldDir.list(WORLD_FILE_FILTER);

        if (worlds == null) {
            throw new NotDirectoryException(worldDir.getPath());
        }

        return Arrays.stream(worlds).map((c) -> c.substring(0, c.length() - 6)).collect(Collectors.toList());
    }

    @Override
    public void saveWorld(String worldName, byte[] serializedWorld) throws IOException {
        File worldFile = new File(worldDir, worldName + ".slime");
        PlazaWorldBackup.backupFile(worldFile, worldName);
        try (FileOutputStream fos = new FileOutputStream(worldFile)) {
            fos.write(serializedWorld);
        }
    }

    @Override
    public void deleteWorld(String worldName) throws UnknownWorldException, IOException {
        if (!worldExists(worldName)) {
            throw new UnknownWorldException(worldName);
        }

        File worldFile = new File(worldDir, worldName + ".slime");
        PlazaWorldBackup.backupFile(worldFile, worldName);
        if (!worldFile.delete()) {
            throw new IOException("Failed to delete the world file. File#delete() returned false.");
        }
    }

    @Override
    public void lockWorld(@NotNull String worldName) throws IOException {
        File lockFile = new File(worldDir, worldName + ".slime.lock");
        if (!lockFile.exists() && !lockFile.createNewFile()) {
            throw new IOException("Failed to create lock file for world " + worldName);
        }
    }

    @Override
    public void unlockWorld(@NotNull String worldName) {
        File lockFile = new File(worldDir, worldName + ".slime.lock");
        if (lockFile.exists() && !lockFile.delete()) {
            LOGGER.warning("Could not delete lock file for world " + worldName);
        }
    }

    @Override
    public boolean isWorldLocked(@NotNull String worldName) {
        return new File(worldDir, worldName + ".slime.lock").exists();
    }

    @Override
    @NotNull
    public PlazaWorld readWorld(@NotNull String worldName, boolean readOnly, @NotNull PlazaWorldPropertyMap properties) throws UnknownWorldException, IOException {
        byte[] data = readWorldBytes(worldName);
        PlazaSlimePropertyMap slimeProperties = properties instanceof PlazaSlimePropertyMap slime
                ? slime
                : new PlazaSlimePropertyMap();
        if (!(properties instanceof PlazaSlimePropertyMap)) {
            slimeProperties.merge(properties);
        }
        return PlazaSlimeWorldReaderRegistry.readWorld(this, worldName, data, slimeProperties, readOnly);
    }

    @Override
    public void saveWorld(@NotNull PlazaWorld world) throws IOException {
        if (!(world instanceof PlazaSlimeWorld slimeWorld)) {
            throw new IllegalArgumentException("PlazaFileWorldLoader can only save Slime worlds");
        }
        saveWorld(slimeWorld.getName(), PlazaSlimeSerializer.serialize(slimeWorld));
    }

    @Override
    @NotNull
    public String getName() {
        return "file";
    }
}
