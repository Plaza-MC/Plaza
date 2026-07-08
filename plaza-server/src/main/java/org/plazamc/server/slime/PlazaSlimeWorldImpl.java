package org.plazamc.server.slime;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.kyori.adventure.nbt.BinaryTag;
import org.plazamc.server.slime.loader.PlazaSlimeLoader;
import org.plazamc.server.slime.properties.PlazaSlimePropertyMap;
import org.plazamc.server.slime.skeleton.PlazaSkeletonSlimeWorld;

import java.util.concurrent.ConcurrentMap;

/**
 * Simple {@link PlazaSlimeWorld} implementation backed by the skeleton world.
 */
public class PlazaSlimeWorldImpl extends PlazaSkeletonSlimeWorld {

    public PlazaSlimeWorldImpl(
            String name,
            PlazaSlimeLoader loader,
            boolean readOnly,
            Long2ObjectMap<PlazaSlimeChunk> chunkStorage,
            ConcurrentMap<String, BinaryTag> extraSerialized,
            PlazaSlimePropertyMap slimePropertyMap,
            int dataVersion
    ) {
        super(name, loader, readOnly, chunkStorage, extraSerialized, slimePropertyMap, dataVersion);
    }
}
