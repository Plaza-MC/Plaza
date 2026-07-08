package org.plazamc.server.world;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.plazamc.api.exceptions.WorldAlreadyExistsException;
import org.plazamc.api.world.PlazaWorld;
import org.plazamc.api.world.PlazaWorldFormat;
import org.plazamc.api.world.PlazaWorldLoader;
import org.plazamc.api.world.PlazaWorldPropertyMap;
import org.plazamc.api.world.PlazaWorldPropertyMapImpl;
import org.plazamc.server.slime.pdc.PlazaAdventurePersistentDataContainer;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Minimal Plaza world wrapper for vanilla Anvil worlds. Used so that Bukkit-created
 * Anvil worlds can be tracked by Plaza's world manager.
 */
public final class PlazaAnvilWorld implements PlazaWorld {

    private final String name;
    private final boolean readOnly;
    private final PlazaWorldPropertyMap properties;
    private final ConcurrentMap<String, net.kyori.adventure.nbt.BinaryTag> extraData;
    private final PlazaAdventurePersistentDataContainer pdc;

    public PlazaAnvilWorld(@NotNull String name, boolean readOnly) {
        this(name, readOnly, new PlazaWorldPropertyMapImpl(), new ConcurrentHashMap<>());
    }

    public PlazaAnvilWorld(@NotNull String name, boolean readOnly,
                           @NotNull PlazaWorldPropertyMap properties,
                           @NotNull ConcurrentMap<String, net.kyori.adventure.nbt.BinaryTag> extraData) {
        this.name = name;
        this.readOnly = readOnly;
        this.properties = properties;
        this.extraData = extraData;
        this.pdc = new PlazaAdventurePersistentDataContainer(extraData);
    }

    @Override
    @NotNull
    public String getName() {
        return this.name;
    }

    @Override
    @Nullable
    public PlazaWorldLoader getLoader() {
        return null;
    }

    @Override
    @NotNull
    public PlazaWorldFormat getFormat() {
        return PlazaWorldFormat.ANVIL;
    }

    @Override
    public boolean isReadOnly() {
        return this.readOnly;
    }

    @Override
    @NotNull
    public PlazaWorldPropertyMap getPropertyMap() {
        return this.properties;
    }

    @Override
    public int getDataVersion() {
        return net.minecraft.SharedConstants.getCurrentVersion().dataVersion().version();
    }

    @Override
    @NotNull
    public PlazaWorld clone(@NotNull String worldName) {
        try {
            return clone(worldName, null);
        } catch (WorldAlreadyExistsException | IOException ignored) {
            return null;
        }
    }

    @Override
    @NotNull
    public PlazaWorld clone(@NotNull String worldName, @Nullable PlazaWorldLoader loader) throws WorldAlreadyExistsException, IOException {
        throw new UnsupportedOperationException("Cloning Anvil worlds is not supported yet");
    }

    @Override
    @NotNull
    public PersistentDataContainer getPersistentDataContainer() {
        return this.pdc;
    }
}
