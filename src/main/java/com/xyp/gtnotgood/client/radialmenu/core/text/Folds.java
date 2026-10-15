package com.xyp.gtnotgood.client.radialmenu.core.text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

public final class Folds {

    private final TreeSet<Integer> folded = new TreeSet<>();

    public static int endOf(List<String> lines, int[] depths, int line) {
        if (line < 0 || line >= lines.size() || line >= depths.length || isBlank(lines.get(line))) {
            return -1;
        }
        int end = line;
        while (end + 1 < depths.length && depths[end + 1] > depths[line]) {
            end++;
        }
        return end > line ? end : -1;
    }

    private static boolean isBlank(String line) {
        return line == null || line.trim().isEmpty();
    }

    public boolean isEmpty() {
        return folded.isEmpty();
    }

    public boolean isFolded(int line) {
        return folded.contains(Integer.valueOf(line));
    }

    public boolean toggle(List<String> lines, int[] depths, int line) {
        if (folded.remove(Integer.valueOf(line))) {
            return true;
        }
        if (endOf(lines, depths, line) < 0) {
            return false;
        }
        folded.add(Integer.valueOf(line));
        return true;
    }

    public int hiddenUnder(List<String> lines, int[] depths, int line) {
        for (Integer header : folded) {
            if (header.intValue() >= line) {
                break;
            }
            if (line <= endOf(lines, depths, header.intValue())) {
                return header.intValue();
            }
        }
        return -1;
    }

    public boolean reveal(List<String> lines, int[] depths, int line) {
        boolean changed = false;
        for (Iterator<Integer> it = folded.iterator(); it.hasNext();) {
            int header = it.next().intValue();
            if (header < line && line <= endOf(lines, depths, header)) {
                it.remove();
                changed = true;
            }
        }
        return changed;
    }

    public void edited(int from, int to, int delta, List<String> lines, int[] depths) {
        if (folded.isEmpty()) {
            return;
        }
        List<Integer> kept = new ArrayList<>();
        for (Integer header : folded) {
            int line = header.intValue();
            if (delta != 0 && line >= from && line <= to) {
                continue;
            }
            if (line > to) {
                line += delta;
            }
            if (endOf(lines, depths, line) >= 0) {
                kept.add(Integer.valueOf(line));
            }
        }
        folded.clear();
        folded.addAll(kept);
    }

    public int[] visibleLines(List<String> lines, int[] depths) {
        int count = lines.size();
        int[] visible = new int[count];
        int size = 0;
        int line = 0;
        while (line < count) {
            visible[size++] = line;
            int end = isFolded(line) ? endOf(lines, depths, line) : -1;
            line = end < 0 ? line + 1 : end + 1;
        }
        int[] trimmed = new int[size];
        System.arraycopy(visible, 0, trimmed, 0, size);
        return trimmed;
    }
}
