package com.xyp.gtnotgood.build;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ModernJavaCompatibilityTest {

    @Test
    public void preservesRecordValueSemanticsAfterDowngrading() {
        Reading first = new Reading("voltage", 32);
        Reading same = new Reading("voltage", 32);
        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotEquals(first, new Reading("voltage", 128));
        assertEquals("voltage", first.name());
        assertEquals(32, first.value());
        assertTrue(first.toString().contains("voltage"));
    }

    @Test
    public void preservesModernLibraryBehaviorAfterDowngrading() {
        List<String> values = new ArrayList<>(List.of("a", "b", "c"));
        List<String> reversed = values.reversed();
        assertEquals("c", reversed.getFirst());
        assertEquals("a", reversed.getLast());
        reversed.removeFirst();
        assertEquals(List.of("a", "b"), values);
        assertEquals("Voltage: 32", "  %s: %d  ".formatted("Voltage", 32).strip());
    }

    private record Reading(String name, int value) {}
}
