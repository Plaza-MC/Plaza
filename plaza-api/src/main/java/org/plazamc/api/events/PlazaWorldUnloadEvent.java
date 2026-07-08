package org.plazamc.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.plazamc.api.world.PlazaWorld;

/**
 * Called before a Plaza world is unloaded.
 */
public class PlazaWorldUnloadEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final PlazaWorld world;

    public PlazaWorldUnloadEvent(@NotNull PlazaWorld world) {
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
