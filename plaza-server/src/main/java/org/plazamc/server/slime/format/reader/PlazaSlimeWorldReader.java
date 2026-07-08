package org.plazamc.server.slime.format.reader;

import org.jspecify.annotations.Nullable;
import org.plazamc.server.slime.PlazaSlimeWorld;
import org.plazamc.server.slime.loader.PlazaSlimeLoader;
import org.plazamc.server.slime.properties.PlazaSlimePropertyMap;

import java.io.DataInputStream;
import java.io.IOException;

public interface PlazaSlimeWorldReader {

    PlazaSlimeWorld deserializeWorld(
            byte version,
            @Nullable PlazaSlimeLoader loader,
            String worldName,
            DataInputStream dataStream,
            PlazaSlimePropertyMap propertyMap,
            boolean readOnly
    ) throws IOException;
}
