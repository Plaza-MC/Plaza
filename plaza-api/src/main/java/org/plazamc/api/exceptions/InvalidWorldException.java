package org.plazamc.api.exceptions;

/**
 * Thrown when a world folder cannot be parsed as a valid world.
 */
public class InvalidWorldException extends PlazaWorldException {

    private final String worldPath;

    public InvalidWorldException(String worldPath) {
        super("Invalid world: " + worldPath);
        this.worldPath = worldPath;
    }

    public String getWorldPath() {
        return this.worldPath;
    }
}
