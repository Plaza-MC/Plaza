package org.plazamc.api.world;

import org.bukkit.persistence.PersistentDataHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.plazamc.api.exceptions.WorldAlreadyExistsException;

import java.io.IOException;

/**
 * In-memory representation of a Plaza world, independent of the storage format.
 */
public interface PlazaWorld extends PersistentDataHolder {

    /**
     * Returns the name of the world.
     *
     * @return The world name.
     */
    @NotNull
    String getName();

    /**
     * Returns the loader used to persist this world, or {@code null} if it is temporary.
     *
     * @return The loader, or {@code null}.
     */
    @Nullable
    PlazaWorldLoader getLoader();

    /**
     * Returns the storage format of this world.
     *
     * @return The format.
     */
    @NotNull
    PlazaWorldFormat getFormat();

    /**
     * Returns whether this world is read-only.
     *
     * @return {@code true} if read-only.
     */
    boolean isReadOnly();

    /**
     * Returns the property map of the world.
     *
     * @return The properties.
     */
    @NotNull
    PlazaWorldPropertyMap getPropertyMap();

    /**
     * Returns the data version of the world.
     *
     * @return The data version.
     */
    int getDataVersion();

    /**
     * Creates a read-only clone with a new name. The clone is not saved.
     *
     * @param worldName The new name.
     * @return The cloned world.
     * @throws IllegalArgumentException if the name is invalid.
     */
    @NotNull
    PlazaWorld clone(@NotNull String worldName);

    /**
     * Creates a clone with a new name and stores it in the given loader.
     *
     * @param worldName The new name.
     * @param loader    The target loader, or {@code null} for a temporary clone.
     * @return The cloned world.
     * @throws IllegalArgumentException    if the name is invalid.
     * @throws WorldAlreadyExistsException if the world already exists.
     * @throws IOException                 if the world could not be saved.
     */
    @NotNull
    PlazaWorld clone(@NotNull String worldName, @Nullable PlazaWorldLoader loader) throws WorldAlreadyExistsException, IOException;
}
