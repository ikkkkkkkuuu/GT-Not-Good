package com.xyp.gtnotgood.common.machines.multiblock.structure;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class HeartOfTheUniverseStructureTest {

    @Test
    public void preservesEveryPinnedCoordinateAndUnconstrainedSpace() throws Exception {
        String[][] shape = HeartOfTheUniverseStructure.shape();
        assertEquals(112, shape.length);
        Map<Character, Integer> counts = new HashMap<>();
        for (String[] layer : shape) {
            assertEquals(109, layer.length);
            for (String row : layer) {
                assertEquals(121, row.length());
                for (char symbol : row.toCharArray()) counts.merge(symbol, 1, Integer::sum);
            }
        }
        String symbols = "ABCDEFGHPRST~ ";
        int[] expected = { 2839, 977, 17604, 555, 11539, 253, 213, 152, 628, 1336, 26, 1021, 1, 1440024 };
        assertEquals(expected.length, counts.size());
        for (int index = 0; index < expected.length; index++) assertEquals("symbol " + symbols.charAt(index),
            Integer.valueOf(expected[index]), counts.get(symbols.charAt(index)));
        assertFalse(counts.containsKey('-'));
        StringBuilder original = new StringBuilder();
        for (int aisle = 0; aisle < 109; aisle++) {
            for (int row = 0; row < 112; row++) {
                if (original.length() > 0) original.append('\n');
                original.append(shape[111 - row][108 - aisle]);
            }
        }
        assertEquals("0a008d25ee66ea91af814df4f9148c1cb7fe5827266ef696330f46499f742373",
            digest(original.toString().getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    public void shipsThePinnedDeterministicStructureEncoding() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream(HeartOfTheUniverseStructure.RESOURCE)) {
            assertNotNull(stream);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = new byte[4096];
            int read;
            while ((read = stream.read(bytes)) != -1) digest.update(bytes, 0, read);
            assertEquals("d5de2d3170a7575604657c8cb30c513c8b29d840be8ab74180ecd45c7a421fca", hex(digest.digest()));
        }
    }

    @Test
    public void placesControllerWithoutMirroringWidth() {
        assertEquals(121, HeartOfTheUniverseStructure.WIDTH);
        assertEquals(112, HeartOfTheUniverseStructure.HEIGHT);
        assertEquals(109, HeartOfTheUniverseStructure.LENGTH);
        assertEquals(62, HeartOfTheUniverseStructure.OFFSET_X);
        assertEquals(88, HeartOfTheUniverseStructure.OFFSET_Y);
        assertEquals(15, HeartOfTheUniverseStructure.OFFSET_Z);
        assertEquals('~', HeartOfTheUniverseStructure.shape()[88][15].charAt(62));
    }

    @Test
    public void returnsIndependentLayersForStructureConsumers() {
        String[][] original = HeartOfTheUniverseStructure.shape();
        String[][] modified = HeartOfTheUniverseStructure.shape();
        modified[0][0] = "modified";
        modified[1] = null;
        String[][] next = HeartOfTheUniverseStructure.shape();
        for (int row = 0; row < original.length; row++) assertArrayEquals(original[row], next[row]);
    }

    private static String digest(byte[] data) throws Exception {
        return hex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    private static String hex(byte[] bytes) {
        StringBuilder text = new StringBuilder();
        for (byte value : bytes) {
            text.append(Character.forDigit((value & 255) >>> 4, 16));
            text.append(Character.forDigit(value & 15, 16));
        }
        return text.toString();
    }
}
