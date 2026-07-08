package org.plazamc.server.slime.format.reader.v13;

import org.plazamc.server.slime.format.reader.PlazaSimpleWorldFormat;

public interface PlazaV13WorldFormat {

    PlazaSimpleWorldFormat FORMAT = new PlazaSimpleWorldFormat(new PlazaV13SlimeWorldDeserializer());
}
