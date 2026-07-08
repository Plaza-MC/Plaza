package org.plazamc.api.exceptions;

import org.jetbrains.annotations.Nullable;

/**
 * Thrown when world data cannot be parsed.
 */
public class CorruptedWorldException extends PlazaWorldException {

    public CorruptedWorldException(String message) {
        super(message);
    }

    public CorruptedWorldException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
