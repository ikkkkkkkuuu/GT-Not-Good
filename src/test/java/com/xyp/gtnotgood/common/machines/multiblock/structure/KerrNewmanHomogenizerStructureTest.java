package com.xyp.gtnotgood.common.machines.multiblock.structure;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class KerrNewmanHomogenizerStructureTest {

    @Test
    public void preservesEveryPinnedUpstreamCoordinateAndUnconstrainedSpace() throws Exception {
        String[][] shape = KerrNewmanHomogenizerStructure.shape();
        assertEquals(36, shape.length);
        Map<Character, Integer> counts = new HashMap<>();
        for (String[] layer : shape) {
            assertEquals(83, layer.length);
            for (String row : layer) {
                assertEquals(83, row.length());
                for (char symbol : row.toCharArray()) counts.merge(symbol, 1, Integer::sum);
            }
        }
        String symbols = "ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`a~ ";
        int[] expected = { 512, 1200, 2220, 668, 2016, 272, 112, 56, 172, 288, 418, 386, 187, 1136, 110, 468, 224, 104,
            544, 512, 320, 10, 64, 608, 256, 168, 896, 472, 528, 566, 102, 32, 10, 1, 232366 };
        assertEquals(expected.length, counts.size());
        for (int index = 0; index < expected.length; index++) assertEquals("symbol " + symbols.charAt(index),
            Integer.valueOf(expected[index]), counts.get(symbols.charAt(index)));
        assertFalse(counts.containsKey('-'));

        StringBuilder original = new StringBuilder();
        for (int aisle = 0; aisle < 83; aisle++) {
            for (int row = 0; row < 36; row++) {
                if (original.length() > 0) original.append('\n');
                original.append(shape[35 - row][82 - aisle].replace('~', 'b'));
            }
        }
        assertEquals("74f42886b775152571cd79824b5144e7907b2fc3a9aa69680f20f7ef0d86eec3",
            digest(original.toString().getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    public void shipsTheOriginalCompressedStructureWithoutModification() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream(KerrNewmanHomogenizerStructure.RESOURCE)) {
            assertNotNull(stream);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = new byte[4096];
            int read;
            while ((read = stream.read(bytes)) != -1) digest.update(bytes, 0, read);
            assertEquals("1e97839453a7d5e31e1460c14a4ca217ff5214b7ed7949ebbd8e7d2a3c176dde", hex(digest.digest()));
        }
    }

    @Test
    public void placesControllerSevenBlocksBehindTheFrontWithoutMirroringWidth() {
        assertEquals(83, KerrNewmanHomogenizerStructure.WIDTH);
        assertEquals(36, KerrNewmanHomogenizerStructure.HEIGHT);
        assertEquals(83, KerrNewmanHomogenizerStructure.LENGTH);
        assertEquals(41, KerrNewmanHomogenizerStructure.OFFSET_X);
        assertEquals(12, KerrNewmanHomogenizerStructure.OFFSET_Y);
        assertEquals(7, KerrNewmanHomogenizerStructure.OFFSET_Z);
        assertEquals('~', KerrNewmanHomogenizerStructure.shape()[12][7].charAt(41));
    }

    @Test
    public void returnsIndependentLayersForStructureConsumers() {
        String[][] original = KerrNewmanHomogenizerStructure.shape();
        String[][] modified = KerrNewmanHomogenizerStructure.shape();
        modified[0][0] = "modified";
        modified[1] = null;
        String[][] next = KerrNewmanHomogenizerStructure.shape();
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
