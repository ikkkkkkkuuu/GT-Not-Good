package com.xyp.gtnotgood.common.machines.multiblock.structure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class NyarlathotepsTentacleStructureTest {

    @Test
    public void originalGeometryAndControllerRemainIntact() throws Exception {
        String[][] shape = NyarlathotepsTentacleStructure.shape();
        assertEquals(48, shape.length);
        Map<Character, Integer> counts = new HashMap<>();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (int aisle = 48; aisle >= 0; aisle--) {
            for (int row = 47; row >= 0; row--) {
                assertEquals(49, shape[row].length);
                String line = shape[row][aisle];
                assertEquals(165, line.length());
                for (char symbol : line.toCharArray()) counts.merge(symbol, 1, Integer::sum);
                String source = line.replace('~', 'a').replace('b', '~');
                digest.update(source.getBytes(StandardCharsets.US_ASCII));
                digest.update((byte) '\n');
            }
        }
        assertEquals(Integer.valueOf(1), counts.get('~'));
        assertEquals(Integer.valueOf(34), counts.get('b'));
        assertEquals(Integer.valueOf(424), counts.get('Q'));
        assertEquals(Integer.valueOf(62), counts.get('A'));
        assertEquals(29045, 165 * 48 * 49 - counts.get(' '));
        assertEquals('~', shape[23][0].charAt(82));
        assertEquals("1b43e3e318af7bc8e96270e5c97a884d6d3be7ac251696640e746a8a2948bb25", hex(digest.digest()));
    }

    @Test
    public void callersCannotMutateCachedShape() {
        String[][] first = NyarlathotepsTentacleStructure.shape();
        String[][] second = NyarlathotepsTentacleStructure.shape();
        assertNotSame(first, second);
        assertNotSame(first[0], second[0]);
        String original = second[0][0];
        first[0][0] = "changed";
        assertEquals(original, NyarlathotepsTentacleStructure.shape()[0][0]);
    }

    private static String hex(byte[] bytes) {
        StringBuilder value = new StringBuilder();
        for (byte next : bytes) value.append(String.format("%02x", next & 255));
        return value.toString();
    }
}
