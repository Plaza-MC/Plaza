package org.plazamc.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.plazamc.api.world.PlazaWorld;

/**
 * Called after a Plaza world has been saved to its data source.
 */
public class PlazaWorldSaveEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final PlazaWorld world;

    public PlazaWorldSaveEvent(@NotNull PlazaWorld world) {
        this.world = world;
    }

    @NotNull
    public PlazaWorld getWorld() {
        return this.world;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
