package org.plazamc.api.exceptions;

import org.jetbrains.annotations.Nullable;

/**
 * Base exception for all Plaza world errors.
 */
public class PlazaWorldException extends Exception {

    public PlazaWorldException(String message) {
        super(message);
    }

    public PlazaWorldException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
