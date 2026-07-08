package org.plazamc.api.world;

import org.jetbrains.annotations.NotNull;

/**
 * Supported world storage formats in Plaza.
 */
public enum PlazaWorldFormat {

    SLIME("slime"),
    ANVIL("anvil"),
    LINEAR("linear"),
    POLAR("polar");

    private final String id;

    PlazaWorldFormat(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public static PlazaWorldFormat fromId(@NotNull String id) {
        for (PlazaWorldFormat format : values()) {
            if (format.id.equalsIgnoreCase(id)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unknown world format: " + id);
    }
}
