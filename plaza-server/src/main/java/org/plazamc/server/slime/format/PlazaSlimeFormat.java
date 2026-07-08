package org.plazamc.server.slime.format;

/**
 * Class containing some standards of the Slime format.
 */
public class PlazaSlimeFormat {

    /** First bytes of every Slime file. */
    public static final byte[] SLIME_HEADER = new byte[] { -79, 11 };

    /** Latest version of the Slime format supported by Plaza. */
    public static final byte SLIME_VERSION = 13;

    private PlazaSlimeFormat() {
        throw new AssertionError();
    }
}
