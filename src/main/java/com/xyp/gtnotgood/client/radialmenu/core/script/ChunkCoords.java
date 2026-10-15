package com.xyp.gtnotgood.client.radialmenu.core.script;

public final class ChunkCoords {

    public static final int SIZE = 16;

    private ChunkCoords() {}

    public static int chunkOf(int world) {
        return world >> 4;
    }

    public static int offsetOf(int world) {
        return world & 15;
    }

    public static int toWorld(int chunk, int offset) {
        return chunk * SIZE + offset;
    }
}
