package com.xyp.gtnotgood.common.machines.multiblock.structure;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class DimensionalFocusEngravingStructureTest {

    @Test
    public void preservesEveryPinnedUpstreamCoordinate() throws Exception {
        String[][] shape = DimensionalFocusEngravingStructure.shape();
        assertEquals(89, shape.length);
        Map<Character, Integer> counts = new HashMap<>();
        for (String[] layer : shape) {
            assertEquals(89, layer.length);
            for (String row : layer) {
                assertEquals(89, row.length());
                for (char symbol : row.toCharArray()) counts.merge(symbol, 1, Integer::sum);
            }
        }
        assertEquals(10, counts.size());
        assertCount(counts, 'A', 43);
        assertCount(counts, 'B', 976);
        assertCount(counts, 'C', 1821);
        assertCount(counts, 'D', 462);
        assertCount(counts, 'E', 96);
        assertCount(counts, 'F', 144);
        assertCount(counts, 'G', 3199);
        assertCount(counts, 'H', 3312);
        assertCount(counts, ' ', 694915);
        assertCount(counts, '~', 1);
        assertFalse(counts.containsKey('-'));

        StringBuilder upstream = new StringBuilder();
        for (int aisle = 0; aisle < 89; aisle++) {
            for (int row = 0; row < 89; row++) {
                if (upstream.length() > 0) upstream.append('\n');
                for (int depth = 0; depth < 89; depth++) {
                    char symbol = shape[88 - row][depth].charAt(88 - aisle);
                    upstream.append(symbol == '~' ? 'I' : symbol);
                }
            }
        }
        byte[] digest = MessageDigest.getInstance("SHA-256")
            .digest(upstream.toString().getBytes(StandardCharsets.US_ASCII));
        StringBuilder actual = new StringBuilder();
        for (byte value : digest) {
            actual.append(Character.forDigit((value & 0xff) >>> 4, 16));
            actual.append(Character.forDigit(value & 0xf, 16));
        }
        assertEquals("e56898976e4517d82f1bb02b17fb6038bb4ce97235f602dedb65f8351245910b", actual.toString());
    }

    @Test
    public void placesControllerUsingConvertedUpstreamAxes() {
        assertEquals(89, DimensionalFocusEngravingStructure.WIDTH);
        assertEquals(89, DimensionalFocusEngravingStructure.HEIGHT);
        assertEquals(89, DimensionalFocusEngravingStructure.LENGTH);
        assertEquals(44, DimensionalFocusEngravingStructure.OFFSET_X);
        assertEquals(26, DimensionalFocusEngravingStructure.OFFSET_Y);
        assertEquals(22, DimensionalFocusEngravingStructure.OFFSET_Z);
        String[][] shape = DimensionalFocusEngravingStructure.shape();
        assertEquals('~',
            shape[DimensionalFocusEngravingStructure.OFFSET_Y][DimensionalFocusEngravingStructure.OFFSET_Z]
                .charAt(DimensionalFocusEngravingStructure.OFFSET_X));
    }

    @Test
    public void returnsIndependentLayersForStructureConsumers() {
        String[][] untouched = DimensionalFocusEngravingStructure.shape();
        String[][] changed = DimensionalFocusEngravingStructure.shape();
        changed[0][0] = "changed";
        changed[1] = null;
        String[][] result = DimensionalFocusEngravingStructure.shape();
        for (int row = 0; row < untouched.length; row++) assertArrayEquals(untouched[row], result[row]);
    }

    private static void assertCount(Map<Character, Integer> counts, char symbol, int expected) {
        assertEquals("symbol " + symbol, Integer.valueOf(expected), counts.get(symbol));
    }
}
