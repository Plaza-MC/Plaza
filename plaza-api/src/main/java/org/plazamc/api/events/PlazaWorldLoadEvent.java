package org.plazamc.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.plazamc.api.world.PlazaWorldInstance;

/**
 * Called after a Plaza world has been loaded as a live server level.
 */
public class PlazaWorldLoadEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final PlazaWorldInstance instance;

    public PlazaWorldLoadEvent(@NotNull PlazaWorldInstance instance) {
        this.instance = instance;
    }

    @NotNull
    public PlazaWorldInstance getInstance() {
        return this.instance;
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
