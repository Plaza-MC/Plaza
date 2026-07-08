package org.plazamc.server.slime.format.reader;

import org.plazamc.server.slime.PlazaSlimeWorld;
import org.plazamc.server.slime.format.PlazaSlimeFormat;
import org.plazamc.server.slime.format.reader.v13.PlazaV13WorldFormat;
import org.plazamc.server.slime.loader.PlazaSlimeLoader;
import org.plazamc.server.slime.properties.PlazaSlimePropertyMap;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

public class PlazaSlimeWorldReaderRegistry {

    private static final PlazaSlimeWorldReader V13_READER = PlazaV13WorldFormat.FORMAT;

    public static PlazaSlimeWorld readWorld(
            PlazaSlimeLoader loader,
            String worldName,
            byte[] serializedWorld,
            PlazaSlimePropertyMap propertyMap,
            boolean readOnly
    ) throws IOException {
        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(serializedWorld));
        byte[] fileHeader = new byte[PlazaSlimeFormat.SLIME_HEADER.length];
        dataStream.read(fileHeader);

        if (!Arrays.equals(PlazaSlimeFormat.SLIME_HEADER, fileHeader)) {
            throw new IOException("Invalid Slime file header for world " + worldName);
        }

        byte version = dataStream.readByte();

        if (version != PlazaSlimeFormat.SLIME_VERSION) {
            throw new IOException("Unsupported Slime format version " + version + " for world " + worldName + ". Only version 13 is supported.");
        }

        return V13_READER.deserializeWorld(version, loader, worldName, dataStream, propertyMap, readOnly);
    }
}
