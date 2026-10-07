package com.xyp.gtnotgood.common.machines.basic;

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Bounded connected-fluid search using pump-relative X/Z and absolute Y coordinates.
 * Below neighbors take priority; visited positions are retained until the complete area scan finishes.
 * World checks and drainage belong to the caller, which must retain the head when a drain stalls.
 */
final class PumpFluidSearch {

    private final int radius;
    private final int width;
    private final int sectionWidth;
    private final int limit;
    private final Deque<Integer> pending = new ArrayDeque<>();
    private final Map<Integer, BitSet> visited = new HashMap<>();
    private boolean started;

    PumpFluidSearch(int radius, int limit) {
        this.radius = radius;
        this.width = radius * 2 + 1;
        this.sectionWidth = (width + 15) / 16;
        this.limit = limit;
    }

    /** Checks the column directly below the pump before searching the surrounding area. */
    void start(int top) {
        if (started) return;
        started = true;
        for (int y = top; y >= 0; y--) offer(0, y, 0, top, false);
    }

    boolean started() {
        return started;
    }

    boolean hasPending() {
        return !pending.isEmpty();
    }

    int xOffset() {
        return pending.getFirst() % width - radius;
    }

    int zOffset() {
        return pending.getFirst() / width % width - radius;
    }

    int y() {
        return pending.getFirst() / (width * width);
    }

    void removeFirst() {
        pending.removeFirst();
    }

    /** Adds only geometrically valid positions; each eventual world lookup consumes the caller's scan budget. */
    void follow(int x, int y, int z, int top) {
        offer(x, y - 1, z, top, true);
        offer(x - 1, y, z, top, false);
        offer(x + 1, y, z, top, false);
        offer(x, y, z - 1, top, false);
        offer(x, y, z + 1, top, false);
        offer(x, y + 1, z, top, false);
    }

    private void offer(int x, int y, int z, int top, boolean first) {
        if (x < -radius || x > radius || z < -radius || z > radius || y < 0 || y > top) return;
        int position = (y * width + z + radius) * width + x + radius;
        if (pending.size() >= limit || !markVisited(position)) return;
        if (first) pending.addFirst(position);
        else pending.addLast(position);
    }

    /** Allocates visit bits only for explored 16-cube sections, keeping a narrow oil column inexpensive. */
    private boolean markVisited(int position) {
        int x = position % width;
        int z = position / width % width;
        int y = position / (width * width);
        int section = (y / 16 * sectionWidth + z / 16) * sectionWidth + x / 16;
        int index = ((y & 15) * 16 + (z & 15)) * 16 + (x & 15);
        BitSet bits = visited.computeIfAbsent(section, ignored -> new BitSet());
        if (bits.get(index)) return false;
        bits.set(index);
        return true;
    }

    int[] positions() {
        return pending.stream()
            .mapToInt(Integer::intValue)
            .toArray();
    }

    /** Restores only valid queued positions; already drained blocks need no persisted visit history. */
    void restore(int[] positions, boolean savedStarted, int top) {
        reset();
        started = savedStarted;
        int volume = Math.max(0, top + 1) * width * width;
        for (int position : positions) {
            if (position < 0 || position >= volume) continue;
            if (pending.size() >= limit) break;
            if (!markVisited(position)) continue;
            pending.addLast(position);
        }
    }

    void reset() {
        pending.clear();
        visited.clear();
        started = false;
    }
}
