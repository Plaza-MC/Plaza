package org.plazamc.api.exceptions;

/**
 * Thrown when a world cannot be found in its data source.
 */
public class UnknownWorldException extends PlazaWorldException {

    private final String worldName;

    public UnknownWorldException(String worldName) {
        super("World '" + worldName + "' does not exist");
        this.worldName = worldName;
    }

    public String getWorldName() {
        return this.worldName;
    }
}
