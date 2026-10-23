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
        assertEquals(83, shape.length);
        Map<Character, Integer> counts = new HashMap<>();
        for (String[] layer : shape) {
            assertEquals(209, layer.length);
            for (String row : layer) {
                assertEquals(197, row.length());
                for (char symbol : row.toCharArray()) counts.merge(symbol, 1, Integer::sum);
            }
        }
        assertEquals(14, counts.size());
        assertCount(counts, '#', 24);
        assertCount(counts, 'A', 173483);
        assertCount(counts, 'B', 7556);
        assertCount(counts, 'D', 42995);
        assertCount(counts, 'E', 45634);
        assertCount(counts, 'F', 7774);
        assertCount(counts, 'G', 14);
        assertCount(counts, 'I', 4);
        assertCount(counts, 'J', 19250);
        assertCount(counts, 'K', 3244);
        assertCount(counts, 'L', 1992);
        assertCount(counts, 'N', 654);
        assertCount(counts, ' ', 3114734);
        assertCount(counts, '~', 1);
        assertFalse(counts.containsKey('-'));

        StringBuilder upstream = new StringBuilder("197 83 209\n");
        for (int depth = shape[0].length - 1; depth >= 0; depth--) {
            for (String[] layer : shape) {
                upstream.append(layer[depth].replace('~', '@')).append('\n');
            }
        }
        byte[] digest = MessageDigest.getInstance("SHA-256")
            .digest(upstream.toString().getBytes(StandardCharsets.UTF_8));
        StringBuilder actual = new StringBuilder();
        for (byte value : digest) {
            actual.append(Character.forDigit((value & 0xff) >>> 4, 16));
            actual.append(Character.forDigit(value & 0xf, 16));
        }
        assertEquals("01023570c9f99f8d7ac6c106347ff0b8d3066cf089371ec65a134c1aa6e37b96", actual.toString());
    }

    @Test
    public void anchorsTheOriginalControllerAndTwentyFourHatches() {
        assertEquals(197, SuperParticleColliderStructure.WIDTH);
        assertEquals(83, SuperParticleColliderStructure.HEIGHT);
        assertEquals(209, SuperParticleColliderStructure.LENGTH);
        assertEquals(93, SuperParticleColliderStructure.OFFSET_X);
        assertEquals(41, SuperParticleColliderStructure.OFFSET_Y);
        assertEquals(0, SuperParticleColliderStructure.OFFSET_Z);
        String[][] shape = SuperParticleColliderStructure.shape();
        assertEquals('~', shape[41][0].charAt(93));
        for (int row = 0; row < shape.length; row++) {
            for (int depth = 0; depth < shape[row].length; depth++) {
                for (int col = 0; col < shape[row][depth].length(); col++) {
                    if (shape[row][depth].charAt(col) != '#') continue;
                    assertEquals(0, depth);
                    assertTrue(row >= 39 && row <= 43 && col >= 91 && col <= 95);
                }
            }
        }
        assertEquals(' ', shape[41][104].charAt(98));
        assertEquals(41, SuperParticleColliderStructure.HEIGHT - SuperParticleColliderStructure.OFFSET_Y - 1);
    }

    @Test
    public void returnsIndependentLayersForStructureConsumers() {
        String[][] changed = SuperParticleColliderStructure.shape();
        changed[0][0] = "changed";
        changed[1] = null;
        String[][] untouched = SuperParticleColliderStructure.shape();
        assertEquals(197, untouched[0][0].length());
        assertNotNull(untouched[1]);
        assertEquals('~', untouched[41][0].charAt(93));
    }

    private static void assertCount(Map<Character, Integer> counts, char symbol, int expected) {
        assertEquals("symbol " + symbol, Integer.valueOf(expected), counts.get(symbol));
    }
}
