package com.xyp.gtnotgood.common.machines.multiblock.structure;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class MageAssemblerStructureTest {

    @Test
    public void preservesCompletePinnedUpstreamGeometry() throws Exception {
        String[][] shape = MageAssemblerStructure.shape();
        assertEquals(10, shape.length);
        Map<Character, Integer> counts = new HashMap<>();
        for (String[] layer : shape) {
            assertEquals(33, layer.length);
            for (String row : layer) {
                assertEquals(9, row.length());
                for (char symbol : row.toCharArray()) counts.merge(symbol, 1, Integer::sum);
            }
        }
        assertEquals(11, counts.size());
        assertCount(counts, 'a', 730);
        assertCount(counts, 'c', 280);
        assertCount(counts, 'd', 124);
        assertCount(counts, 'e', 30);
        assertCount(counts, 'f', 55);
        assertCount(counts, 'g', 4);
        assertCount(counts, 'h', 27);
        assertCount(counts, 'i', 126);
        assertCount(counts, '-', 1);
        assertCount(counts, ' ', 1592);
        assertCount(counts, '~', 1);

        StringBuilder source = new StringBuilder();
        for (int depth = 32; depth >= 0; depth--) for (int row = 9; row >= 0; row--) {
            if (source.length() > 0) source.append('\n');
            source.append(shape[row][depth]);
        }
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(source.toString().getBytes(StandardCharsets.UTF_8));
        StringBuilder actual = new StringBuilder();
        for (byte value : digest) {
            actual.append(Character.forDigit((value & 0xff) >>> 4, 16));
            actual.append(Character.forDigit(value & 0xf, 16));
        }
        assertEquals("0cc2fa0c5afbbb124ec55603e70cdeb096d7e670b1af7d75697e6f9d7474680a", actual.toString());
    }

    @Test
    public void preservesUpwardsSourceAxisAndRearMuffler() {
        assertEquals(9, MageAssemblerStructure.WIDTH);
        assertEquals(10, MageAssemblerStructure.HEIGHT);
        assertEquals(33, MageAssemblerStructure.LENGTH);
        assertEquals(4, MageAssemblerStructure.OFFSET_X);
        assertEquals(5, MageAssemblerStructure.OFFSET_Y);
        assertEquals(0, MageAssemblerStructure.OFFSET_Z);
        String[][] shape = MageAssemblerStructure.shape();
        assertEquals('~', shape[5][0].charAt(4));
        assertEquals('-', shape[5][32].charAt(4));
        assertEquals("aaaaaaaaa", shape[9][16]);
        assertEquals("   aha   ", shape[0][16]);
        assertEquals('g', shape[8][29].charAt(3));
        assertEquals(' ', shape[8][16].charAt(4));
        for (int sourceRow = 0; sourceRow < 10; sourceRow++) {
            int structureLibRow = 9 - sourceRow;
            assertEquals("World Y must agree with GTCEu's UP axis", sourceRow - 4,
                MageAssemblerStructure.OFFSET_Y - structureLibRow);
        }
    }

    @Test
    public void returnsIndependentLayersForStructureConsumers() {
        String[][] changed = MageAssemblerStructure.shape();
        changed[0][0] = "changed";
        changed[1] = null;
        String[][] untouched = MageAssemblerStructure.shape();
        assertEquals("         ", untouched[0][0]);
        assertEquals("aaaa~aaaa", untouched[5][0]);
    }

    private static void assertCount(Map<Character, Integer> counts, char symbol, int expected) {
        assertEquals("symbol " + symbol, Integer.valueOf(expected), counts.get(symbol));
    }
}
