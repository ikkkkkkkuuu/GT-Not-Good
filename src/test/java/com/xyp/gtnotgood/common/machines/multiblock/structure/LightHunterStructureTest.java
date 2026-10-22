package com.xyp.gtnotgood.common.machines.multiblock.structure;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.junit.Test;

public class LightHunterStructureTest {

    @Test
    public void preservesEveryStationCoordinateAndUnconstrainedSpace() throws Exception {
        verify(LightHunterSpaceStationStructure.shape(), 73, 73, 189, 48567, LightHunterSpaceStationStructure.SYMBOLS,
            "1419ffc0e28e96c7a1f004ac16e975c1b031356b007870e30700d6ed9f1c246b");
    }

    @Test
    public void placesStationControllerWithoutMirroringWidth() {
        assertEquals(73, LightHunterSpaceStationStructure.WIDTH);
        assertEquals(73, LightHunterSpaceStationStructure.HEIGHT);
        assertEquals(189, LightHunterSpaceStationStructure.LENGTH);
        assertEquals(36, LightHunterSpaceStationStructure.OFFSET_X);
        assertEquals(36, LightHunterSpaceStationStructure.OFFSET_Y);
        assertEquals(0, LightHunterSpaceStationStructure.OFFSET_Z);
        assertEquals('~', LightHunterSpaceStationStructure.shape()[36][0].charAt(36));
    }

    @Test
    public void shipsDeterministicStationEncoding() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream(LightHunterSpaceStationStructure.RESOURCE)) {
            assertNotNull(stream);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[4096];
            int size;
            while ((size = stream.read(buffer)) != -1) digest.update(buffer, 0, size);
            assertEquals("c9c9339404b2df67c4782c16546ab4386b4d14571f150d3cb1e7614daca75b82", hex(digest.digest()));
        }
    }

    @Test
    public void returnsIndependentStationLayers() {
        String[][] original = LightHunterSpaceStationStructure.shape();
        String[][] modified = LightHunterSpaceStationStructure.shape();
        modified[0][0] = "modified";
        modified[1] = null;
        String[][] next = LightHunterSpaceStationStructure.shape();
        for (int row = 0; row < original.length; row++) assertArrayEquals(original[row], next[row]);
    }

    @Test
    public void preservesEveryModuleCoordinateAndUnconstrainedSpace() throws Exception {
        verify(NexusSatelliteFactoryStructure.shape(), 5, 21, 13, 795, NexusSatelliteFactoryStructure.SYMBOLS,
            "112fbfb19d16ca4608bc537ec8c4dea090ebb3310423ee561e7cdc4d5ac5479b");
    }

    @Test
    public void placesModuleControllerWithoutMirroringWidth() {
        assertEquals(5, NexusSatelliteFactoryStructure.WIDTH);
        assertEquals(21, NexusSatelliteFactoryStructure.HEIGHT);
        assertEquals(13, NexusSatelliteFactoryStructure.LENGTH);
        assertEquals(2, NexusSatelliteFactoryStructure.OFFSET_X);
        assertEquals(10, NexusSatelliteFactoryStructure.OFFSET_Y);
        assertEquals(0, NexusSatelliteFactoryStructure.OFFSET_Z);
        assertEquals('~', NexusSatelliteFactoryStructure.shape()[10][0].charAt(2));
    }

    @Test
    public void shipsDeterministicModuleEncoding() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream(NexusSatelliteFactoryStructure.RESOURCE)) {
            assertNotNull(stream);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[4096];
            int size;
            while ((size = stream.read(buffer)) != -1) digest.update(buffer, 0, size);
            assertEquals("a9186f3a60455cc651e9da271778734d3a03c5ccafa4d355fdf9f2034ab25ea3", hex(digest.digest()));
        }
    }

    @Test
    public void returnsIndependentModuleLayers() {
        String[][] original = NexusSatelliteFactoryStructure.shape();
        String[][] modified = NexusSatelliteFactoryStructure.shape();
        modified[0][0] = "modified";
        modified[1] = null;
        String[][] next = NexusSatelliteFactoryStructure.shape();
        for (int row = 0; row < original.length; row++) assertArrayEquals(original[row], next[row]);
    }

    private static void verify(String[][] shape, int width, int height, int length, int nonSpace, String symbols,
        String expectedHash) throws Exception {
        assertEquals(height, shape.length);
        int solids = 0;
        int controllers = 0;
        for (String[] layer : shape) {
            assertEquals(length, layer.length);
            for (String row : layer) {
                assertEquals(width, row.length());
                for (char symbol : row.toCharArray()) {
                    if (symbol != ' ') solids++;
                    if (symbol == '~') controllers++;
                    assertTrue(symbol == ' ' || symbol == '~' || symbols.indexOf(symbol) >= 0);
                }
            }
        }
        assertEquals(nonSpace, solids);
        assertEquals(1, controllers);
        StringBuilder source = new StringBuilder();
        for (int aisle = 0; aisle < length; aisle++) {
            for (int row = 0; row < height; row++) {
                if (source.length() > 0) source.append('\n');
                source.append(shape[height - row - 1][length - aisle - 1]);
            }
        }
        assertEquals(expectedHash,
            hex(MessageDigest.getInstance("SHA-256").digest(source.toString().getBytes(StandardCharsets.US_ASCII))));
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
