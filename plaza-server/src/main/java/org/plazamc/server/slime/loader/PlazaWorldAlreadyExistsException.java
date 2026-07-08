package org.plazamc.server.slime.loader;

public class PlazaWorldAlreadyExistsException extends PlazaSlimeException {

    public PlazaWorldAlreadyExistsException(String world) {
        super("World " + world + " already exists!");
    }
}
