package com.xyp.gtnotgood.common.blocks.webscreen;

import java.util.HashSet;
import java.util.Set;

public final class WebScreenShape {

    public static final int MAX_WIDTH = 16;
    public static final int MAX_HEIGHT = 9;
    public static final int MAX_AREA = MAX_WIDTH * MAX_HEIGHT;
    public final int anchorX;
    public final int anchorY;
    public final int anchorZ;
    public final int width;
    public final int height;
    public final int facing;

    @FunctionalInterface
    public interface CellLookup {

        /** Rejects unloaded cells and tiles with a different facing. */
        boolean matches(int x, int y, int z);
    }

    private WebScreenShape(int anchorX, int anchorY, int anchorZ, int width, int height, int facing) {
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
        this.width = width;
        this.height = height;
        this.facing = facing;
    }

    /** Irregular or oversized connected components fall back to the selected tile. */
    public static WebScreenShape discover(int x, int y, int z, int facing, CellLookup lookup) {
        if (facing < 2 || facing > 5) throw new IllegalArgumentException("Screen facing must be horizontal");
        WebScreenShape single = new WebScreenShape(x, y, z, 1, 1, facing);
        if (!lookup.matches(x, y, z)) return single;
        boolean horizontalX = facing <= 3;
        int horizontal = horizontalX ? x : z;
        int[] horizontalQueue = new int[MAX_AREA];
        int[] verticalQueue = new int[MAX_AREA];
        horizontalQueue[0] = horizontal;
        verticalQueue[0] = y;
        Set<Long> checked = new HashSet<>();
        checked.add(cellKey(horizontal, y));
        int head = 0;
        int tail = 1;
        int minHorizontal = horizontal;
        int maxHorizontal = horizontal;
        int minY = y;
        int maxY = y;
        while (head < tail) {
            int currentHorizontal = horizontalQueue[head];
            int currentY = verticalQueue[head++];
            for (int direction = 0; direction < 4; direction++) {
                int nextHorizontal = currentHorizontal + (direction == 0 ? -1 : direction == 1 ? 1 : 0);
                int nextY = currentY + (direction == 2 ? -1 : direction == 3 ? 1 : 0);
                if (!checked.add(cellKey(nextHorizontal, nextY))) continue;
                boolean matches = lookup.matches(horizontalX ? nextHorizontal : x, nextY,
                    horizontalX ? z : nextHorizontal);
                if (!matches) continue;
                minHorizontal = Math.min(minHorizontal, nextHorizontal);
                maxHorizontal = Math.max(maxHorizontal, nextHorizontal);
                minY = Math.min(minY, nextY);
                maxY = Math.max(maxY, nextY);
                if (
                    (long) maxHorizontal - minHorizontal + 1 > MAX_WIDTH || (long) maxY - minY + 1 > MAX_HEIGHT
                        || tail == MAX_AREA
                ) return single;
                horizontalQueue[tail] = nextHorizontal;
                verticalQueue[tail++] = nextY;
            }
        }
        int width = maxHorizontal - minHorizontal + 1;
        int height = maxY - minY + 1;
        if (tail != width * height) return single;
        return new WebScreenShape(horizontalX ? minHorizontal : x, minY, horizontalX ? z : minHorizontal, width, height,
            facing);
    }

    public boolean contains(int x, int y, int z) {
        if (y < anchorY || (long) y - anchorY >= height) return false;
        if (facing <= 3) return z == anchorZ && x >= anchorX && (long) x - anchorX < width;
        return x == anchorX && z >= anchorZ && (long) z - anchorZ < width;
    }

    private static long cellKey(int horizontal, int y) {
        return (long) horizontal << 32 | y & 0xFFFFFFFFL;
    }
}
