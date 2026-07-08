package org.plazamc.server.slime.util;

public final class PlazaSlimeUtil {

    private PlazaSlimeUtil() {
        throw new AssertionError();
    }

    public static long chunkPosition(final int x, final int z) {
        return ((((long) x) << 32) | (z & 0xFFFFFFFFL));
    }
}
