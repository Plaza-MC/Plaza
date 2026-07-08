package org.plazamc.api.world;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a {@link PlazaWorld} that has been loaded as a live server level.
 */
public interface PlazaWorldInstance {

    /**
     * Returns the in-memory world data.
     *
     * @return The world data.
     */
    @NotNull
    PlazaWorld getWorldData();

    /**
     * Returns the live Bukkit world.
     *
     * @return The Bukkit world.
     */
    @NotNull
    World getBukkitWorld();

    /**
     * Returns whether this instance is still loaded.
     *
     * @return {@code true} if loaded.
     */
    boolean isLoaded();
}
