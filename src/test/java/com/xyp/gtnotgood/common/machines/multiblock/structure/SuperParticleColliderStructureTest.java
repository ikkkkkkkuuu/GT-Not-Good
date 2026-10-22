package com.xyp.gtnotgood.common.machines.multiblock.structure;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class SuperParticleColliderStructureTest {

    @Test
    public void preservesEveryPinnedUpstreamBlockAndUnconstrainedPosition() throws Exception {
        String[][] shape = SuperParticleColliderStructure.shape();
        assertEquals(7, shape.length);
        Map<Character, Integer> counts = new HashMap<>();
        for (String[] layer : shape) {
            assertEquals(47, layer.length);
            for (String row : layer) {
                assertEquals(47, row.length());
                for (char symbol : row.toCharArray()) counts.merge(symbol, 1, Integer::sum);
            }
        }
        assertEquals(7, counts.size());
        assertCount(counts, 'b', 14);
        assertCount(counts, 'c', 1777);
        assertCount(counts, 'd', 560);
        assertCount(counts, 'e', 320);
        assertCount(counts, 'f', 128);
        assertCount(counts, ' ', 12663);
        assertCount(counts, '~', 1);
        assertFalse(counts.containsKey('-'));

        StringBuilder upstream = new StringBuilder();
        for (int depth = shape[0].length - 1; depth >= 0; depth--) {
            for (String[] layer : shape) {
                if (upstream.length() > 0) upstream.append('\n');
                upstream.append(layer[depth].replace('~', 'a'));
            }
        }
        byte[] digest = MessageDigest.getInstance("SHA-256")
            .digest(upstream.toString().getBytes(StandardCharsets.UTF_8));
        StringBuilder actual = new StringBuilder();
        for (byte value : digest) {
            actual.append(Character.forDigit((value & 0xff) >>> 4, 16));
            actual.append(Character.forDigit(value & 0xf, 16));
        }
        assertEquals("bab9591197189fdddfa6ecf6b490286468be6dc0494f2bde0ebcbf4426b186d5", actual.toString());
    }

    @Test
    public void anchorsControllerAndHatchesAtFrontOfSymmetricRing() {
        assertEquals(47, SuperParticleColliderStructure.WIDTH);
        assertEquals(7, SuperParticleColliderStructure.HEIGHT);
        assertEquals(47, SuperParticleColliderStructure.LENGTH);
        assertEquals(23, SuperParticleColliderStructure.OFFSET_X);
        assertEquals(3, SuperParticleColliderStructure.OFFSET_Y);
        assertEquals(0, SuperParticleColliderStructure.OFFSET_Z);
        String[][] shape = SuperParticleColliderStructure.shape();
        assertEquals('~', shape[SuperParticleColliderStructure.OFFSET_Y][SuperParticleColliderStructure.OFFSET_Z]
            .charAt(SuperParticleColliderStructure.OFFSET_X));
        assertEquals("                    fbb~bbf                    ", shape[3][0]);
        assertEquals("                    fcccccf                    ", shape[3][46]);
        assertEquals(' ', shape[3][23].charAt(23));
        for (int row = 0; row < shape.length; row++) {
            for (int depth = 0; depth < shape[row].length; depth++) {
                String slice = shape[row][depth];
                assertEquals(slice, new StringBuilder(slice).reverse().toString());
                assertEquals(slice, shape[shape.length - row - 1][depth]);
                if (depth != 0) assertFalse(slice.contains("b"));
            }
        }
    }

    @Test
    public void returnsIndependentLayersForStructureConsumers() {
        String[][] changed = SuperParticleColliderStructure.shape();
        changed[0][0] = "changed";
        changed[1] = null;
        String[][] untouched = SuperParticleColliderStructure.shape();
        assertEquals("                                               ", untouched[0][0]);
        assertEquals("                    fbb~bbf                    ", untouched[3][0]);
    }

    private static void assertCount(Map<Character, Integer> counts, char symbol, int expected) {
        assertEquals("symbol " + symbol, Integer.valueOf(expected), counts.get(symbol));
    }
}
