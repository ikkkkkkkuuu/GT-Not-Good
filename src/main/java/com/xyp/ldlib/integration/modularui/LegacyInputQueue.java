package com.xyp.ldlib.integration.modularui;

import java.util.ArrayDeque;
import java.util.Deque;

final class LegacyInputQueue {

    private final Deque<Integer> keys = new ArrayDeque<>();
    private final Deque<Character> text = new ArrayDeque<>();

    void recordKey(int key) {
        if (key != 0) keys.addLast(key);
    }

    void recordText(String value) {
        for (int i = 0; i < value.length(); i++) text.addLast(value.charAt(i));
    }

    boolean consume(char character, int key) {
        if (keys.removeFirstOccurrence(key)) return true;
        if (key != 0 || text.isEmpty() || text.peekFirst() != character) return false;
        text.removeFirst();
        return true;
    }

    void clear() {
        keys.clear();
        text.clear();
    }
}
