package com.xyp.gtnotgood.common.machines.basicMachine;

/**
 * Constant-memory, top-down scan of a square below a pump. The serialized offset resumes without skipping a layer.
 * World access and per-tick limits belong to the caller; a blocked drain must not advance this cursor.
 */
final class PumpScanCursor {

    private final int radius;
    private final int width;
    private int offset;

    PumpScanCursor(int radius) {
        this.radius = radius;
        this.width = radius * 2 + 1;
    }

    int xOffset() {
        return offset % width - radius;
    }

    int zOffset() {
        return offset / width % width - radius;
    }

    int y(int top) {
        return top - offset / (width * width);
    }

    int offset() {
        return offset;
    }

    void restore(int savedOffset, int top) {
        int volume = Math.max(0, top + 1) * width * width;
        offset = savedOffset >= 0 && savedOffset < volume ? savedOffset : 0;
    }

    /** @return true after the last coordinate, resetting for a later pass over newly loaded chunks. */
    boolean advance(int top) {
        offset++;
        if (y(top) >= 0) return false;
        offset = 0;
        return true;
    }
}
