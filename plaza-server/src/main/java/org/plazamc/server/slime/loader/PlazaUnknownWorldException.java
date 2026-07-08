package org.plazamc.server.slime.loader;

public class PlazaUnknownWorldException extends PlazaSlimeException {

    public PlazaUnknownWorldException(String world) {
        super("Unknown world " + world);
    }
}
