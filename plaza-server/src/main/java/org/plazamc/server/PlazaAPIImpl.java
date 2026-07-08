package org.plazamc.server;

import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.plazamc.api.PlazaAPI;
import org.plazamc.api.exceptions.CorruptedWorldException;
import org.plazamc.api.exceptions.InvalidWorldException;
import org.plazamc.api.exceptions.UnknownWorldException;
import org.plazamc.api.exceptions.WorldAlreadyExistsException;
import org.plazamc.api.exceptions.WorldLoadedException;
import org.plazamc.api.world.PlazaWorld;
import org.plazamc.api.world.PlazaWorldInstance;
import org.plazamc.api.world.PlazaWorldLoader;
import org.plazamc.api.world.PlazaWorldPropertyMap;
import org.plazamc.server.world.PlazaWorldManager;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Server-side implementation of {@link PlazaAPI}.
 */
public final class PlazaAPIImpl implements PlazaAPI {

    @Override
    @NotNull
    public PlazaWorld readWorld(@NotNull PlazaWorldLoader loader, @NotNull String worldName, boolean readOnly,
                                @NotNull PlazaWorldPropertyMap properties) throws UnknownWorldException, IOException, CorruptedWorldException {
        return PlazaWorldManager.readWorld(loader, worldName, readOnly, properties);
    }

    @Override
    @Nullable
    public PlazaWorldInstance getLoadedWorld(@NotNull String worldName) {
        return PlazaWorldManager.getLoadedWorld(worldName);
    }

    @Override
    @NotNull
    public List<PlazaWorldInstance> getLoadedWorlds() {
        return PlazaWorldManager.getLoadedWorlds();
    }

    @Override
    @NotNull
    public PlazaWorldInstance loadWorld(@NotNull PlazaWorld world, boolean callWorldLoadEvent) {
        return PlazaWorldManager.loadWorld(world, callWorldLoadEvent);
    }

    @Override
    public boolean worldLoaded(@NotNull PlazaWorld world) {
        return PlazaWorldManager.worldLoaded(world);
    }

    @Override
    public void saveWorld(@NotNull PlazaWorld world) throws IOException {
        PlazaWorldManager.saveWorld(world);
    }

    @Override
    public void migrateWorld(@NotNull String worldName, @NotNull PlazaWorldLoader currentLoader,
                             @NotNull PlazaWorldLoader newLoader) throws IOException, WorldAlreadyExistsException, UnknownWorldException {
        PlazaWorldManager.migrateWorld(worldName, currentLoader, newLoader);
    }

    @Override
    @NotNull
    public PlazaWorld createEmptyWorld(@NotNull String worldName, boolean readOnly,
                                       @NotNull PlazaWorldPropertyMap properties, @Nullable PlazaWorldLoader loader) {
        return PlazaWorldManager.createEmptyWorld(worldName, readOnly, properties, loader);
    }

    @Override
    @NotNull
    public PlazaWorld importVanillaWorld(@NotNull File worldDir, @NotNull String worldName,
                                         @NotNull PlazaWorldLoader loader) throws IOException, WorldAlreadyExistsException, WorldLoadedException, InvalidWorldException {
        return PlazaWorldManager.importVanillaWorld(worldDir, worldName, loader);
    }

    @Override
    public void exportWorld(@NotNull PlazaWorld world, @NotNull File worldDir) throws IOException {
        PlazaWorldManager.exportWorld(world, worldDir);
    }

    @Override
    @NotNull
    public World createBukkitWorld(@NotNull WorldCreator creator) {
        return PlazaWorldManager.createBukkitWorld(creator);
    }
}
