package com.xyp.ldlib.integration.modularui;

import static org.junit.Assert.*;

import org.junit.Test;

public class LegacyInputQueueTest {

    @Test
    public void eachPressAndRepeatConsumesExactlyOneLegacyCopy() {
        LegacyInputQueue queue = new LegacyInputQueue();
        queue.recordKey(14);
        queue.recordKey(14);
        assertTrue(queue.consume('\0', 14));
        assertTrue(queue.consume('\0', 14));
        assertFalse(queue.consume('\0', 14));
    }

    @Test
    public void unicodeCommitConsumesItsLegacyCharactersWithoutChangingUnrelatedKeys() {
        LegacyInputQueue queue = new LegacyInputQueue();
        queue.recordKey(0);
        queue.recordKey(30);
        queue.recordText("岩浆water");
        assertFalse(queue.consume('\0', 1));
        assertFalse(queue.consume('\0', 2));
        assertTrue(queue.consume('a', 30));
        for (char character : "岩浆water".toCharArray()) assertTrue(queue.consume(character, 0));
        assertFalse(queue.consume('r', 0));
    }

    @Test
    public void clearDiscardsOnlyThePendingLegacyCopies() {
        LegacyInputQueue queue = new LegacyInputQueue();
        queue.recordKey(14);
        queue.recordText("水");
        queue.clear();
        assertFalse(queue.consume('\0', 14));
        assertFalse(queue.consume('水', 0));
    }
}
