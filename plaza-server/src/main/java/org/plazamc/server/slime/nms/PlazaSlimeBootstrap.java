package org.plazamc.server.slime.nms;

import org.plazamc.server.slime.PlazaSlimeWorld;

/**
 * Simple bootstrap record passed into the {@link net.minecraft.server.level.ServerLevel}
 * constructor so that Plaza can install a Slime-backed world.
 */
public record PlazaSlimeBootstrap(PlazaSlimeWorld initial) {
}
