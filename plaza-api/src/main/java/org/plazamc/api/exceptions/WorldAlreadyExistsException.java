package org.plazamc.api.exceptions;

/**
 * Thrown when attempting to create or save a world that already exists.
 */
public class WorldAlreadyExistsException extends PlazaWorldException {

    private final String worldName;

    public WorldAlreadyExistsException(String worldName) {
        super("World '" + worldName + "' already exists");
        this.worldName = worldName;
    }

    public String getWorldName() {
        return this.worldName;
    }
}
