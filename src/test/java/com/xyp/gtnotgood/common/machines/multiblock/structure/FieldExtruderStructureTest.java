package com.xyp.gtnotgood.common.machines.multiblock.structure;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class FieldExtruderStructureTest {

    @Test
    public void preservesCompletePinnedUpstreamShape() throws Exception {
        String[][] shape = FieldExtruderStructure.shape();
        assertEquals(9, shape.length);
        Map<Character, Integer> counts = new HashMap<>();
        for (String[] layer : shape) {
            assertEquals(63, layer.length);
            for (String row : layer) {
                assertEquals(15, row.length());
                for (char symbol : row.toCharArray()) counts.merge(symbol, 1, Integer::sum);
            }
        }
        assertEquals(9, counts.size());
        assertCount(counts, 'A', 728);
        assertCount(counts, 'B', 432);
        assertCount(counts, 'C', 928);
        assertCount(counts, 'D', 144);
        assertCount(counts, 'E', 48);
        assertCount(counts, 'F', 16);
        assertCount(counts, 'G', 302);
        assertCount(counts, ' ', 5906);
        assertCount(counts, '~', 1);
        assertFalse(counts.containsKey('-'));

        StringBuilder upstream = new StringBuilder();
        for (int depth = shape[0].length - 1; depth >= 0; depth--) {
            for (String[] layer : shape) {
                if (upstream.length() > 0) upstream.append('\n');
                upstream.append(layer[depth]);
            }
        }
        byte[] digest = MessageDigest.getInstance("SHA-256")
            .digest(upstream.toString().getBytes(StandardCharsets.UTF_8));
        StringBuilder actual = new StringBuilder();
        for (byte value : digest) {
            actual.append(Character.forDigit((value & 0xff) >>> 4, 16));
            actual.append(Character.forDigit(value & 0xf, 16));
        }
        assertEquals("d073706a31514a4d12b38985b3bf2d29aebed30a3610b5a263ff2c9e938efbbd", actual.toString());
    }

    @Test
    public void placesControllerAtFrontUsingStructureLibOffsets() {
        String[][] shape = FieldExtruderStructure.shape();
        assertEquals(15, FieldExtruderStructure.WIDTH);
        assertEquals(9, FieldExtruderStructure.HEIGHT);
        assertEquals(63, FieldExtruderStructure.LENGTH);
        assertEquals(7, FieldExtruderStructure.OFFSET_X);
        assertEquals(1, FieldExtruderStructure.OFFSET_Y);
        assertEquals(0, FieldExtruderStructure.OFFSET_Z);
        assertEquals('~', shape[FieldExtruderStructure.OFFSET_Y][FieldExtruderStructure.OFFSET_Z]
            .charAt(FieldExtruderStructure.OFFSET_X));
        assertEquals("CAB   AAA   BAC", shape[1][62]);
        assertEquals("CCCCCCCCCCCCCCC", shape[8][0]);
        assertEquals("               ", shape[8][1]);
    }

    @Test
    public void returnsIndependentLayersForStructureConsumers() {
        String[][] changed = FieldExtruderStructure.shape();
        changed[0][0] = "changed";
        changed[1] = null;
        String[][] untouched = FieldExtruderStructure.shape();
        assertEquals("AAB   AAA   BAA", untouched[0][0]);
        assertEquals("CAB   A~A   BAC", untouched[1][0]);
    }

    private static void assertCount(Map<Character, Integer> counts, char symbol, int expected) {
        assertEquals("symbol " + symbol, Integer.valueOf(expected), counts.get(symbol));
    }
}
