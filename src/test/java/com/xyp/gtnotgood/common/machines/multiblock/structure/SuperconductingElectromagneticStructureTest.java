package com.xyp.gtnotgood.common.machines.multiblock.structure;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class SuperconductingElectromagneticStructureTest {

    @Test
    public void preservesCompletePinnedUpstreamGeometry() throws Exception {
        String[][] shape = SuperconductingElectromagneticStructure.shape();
        assertEquals(21, shape.length);
        Map<Character, Integer> counts = new HashMap<>();
        for (String[] layer : shape) {
            assertEquals(15, layer.length);
            for (String row : layer) {
                assertEquals(15, row.length());
                for (char symbol : row.toCharArray()) counts.merge(symbol, 1, Integer::sum);
            }
        }
        assertEquals(9, counts.size());
        assertCount(counts, 'A', 724);
        assertCount(counts, 'B', 364);
        assertCount(counts, 'C', 316);
        assertCount(counts, 'D', 24);
        assertCount(counts, 'E', 156);
        assertCount(counts, 'F', 25);
        assertCount(counts, 'H', 19);
        assertCount(counts, ' ', 3096);
        assertCount(counts, '~', 1);
        assertFalse(counts.containsKey('-'));

        StringBuilder source = new StringBuilder();
        for (int depth = 14; depth >= 0; depth--) for (int row = 20; row >= 0; row--) {
            if (source.length() > 0) source.append('\n');
            source.append(shape[row][depth]);
        }
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(source.toString().getBytes(StandardCharsets.UTF_8));
        StringBuilder actual = new StringBuilder();
        for (byte value : digest) {
            actual.append(Character.forDigit((value & 0xff) >>> 4, 16));
            actual.append(Character.forDigit(value & 0xf, 16));
        }
        assertEquals("dc20319fdeb108130e94cba4c203bd0eb5f7d706620ef2759fb00eec0ccdc70b", actual.toString());
    }

    @Test
    public void keepsControllerAtBottomAndHatchesOnOriginalFaces() {
        assertEquals(15, SuperconductingElectromagneticStructure.WIDTH);
        assertEquals(21, SuperconductingElectromagneticStructure.HEIGHT);
        assertEquals(15, SuperconductingElectromagneticStructure.LENGTH);
        assertEquals(7, SuperconductingElectromagneticStructure.OFFSET_X);
        assertEquals(20, SuperconductingElectromagneticStructure.OFFSET_Y);
        assertEquals(0, SuperconductingElectromagneticStructure.OFFSET_Z);
        String[][] shape = SuperconductingElectromagneticStructure.shape();
        assertEquals('~', shape[20][0].charAt(7));
        assertEquals("     AAAAA     ", shape[0][0]);
        assertEquals('F', shape[19][1].charAt(5));
        assertEquals('F', shape[0][9].charAt(7));
        assertEquals(' ', shape[10][7].charAt(3));
        for (int row = 1; row <= 19; row++) assertEquals('H', shape[row][7].charAt(7));
        for (int sourceRow = 0; sourceRow < 21; sourceRow++) {
            int structureLibRow = 20 - sourceRow;
            assertEquals("World Y must agree with GTCEu's UP axis", sourceRow,
                SuperconductingElectromagneticStructure.OFFSET_Y - structureLibRow);
        }
    }

    @Test
    public void returnsIndependentLayersForStructureConsumers() {
        String[][] changed = SuperconductingElectromagneticStructure.shape();
        changed[0][0] = "changed";
        changed[1] = null;
        String[][] untouched = SuperconductingElectromagneticStructure.shape();
        assertEquals("     AAAAA     ", untouched[0][0]);
        assertEquals("     AA~AA     ", untouched[20][0]);
    }

    private static void assertCount(Map<Character, Integer> counts, char symbol, int expected) {
        assertEquals("symbol " + symbol, Integer.valueOf(expected), counts.get(symbol));
    }
}
