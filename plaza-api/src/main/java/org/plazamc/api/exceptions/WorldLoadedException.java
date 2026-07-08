package org.plazamc.api.exceptions;

/**
 * Thrown when an operation cannot be performed because the world is currently loaded.
 */
public class WorldLoadedException extends PlazaWorldException {

    private final String worldName;

    public WorldLoadedException(String worldName) {
        super("World '" + worldName + "' is currently loaded");
        this.worldName = worldName;
    }

    public String getWorldName() {
        return this.worldName;
    }
}
