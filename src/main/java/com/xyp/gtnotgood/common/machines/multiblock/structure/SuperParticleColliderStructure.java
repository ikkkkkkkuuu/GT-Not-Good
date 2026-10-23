// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.common.machines.multiblock.structure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import com.xyp.gtnotgood.utils.enums.ModList;

// GTL-Enhancedcore 3.8.0 hollow structure; META-INF/particle-collider-port/ENHANCED_STRUCTURE.json.
public final class SuperParticleColliderStructure {

    public static final int WIDTH = 197;
    public static final int HEIGHT = 83;
    public static final int LENGTH = 209;
    public static final int OFFSET_X = 93;
    public static final int OFFSET_Y = 41;
    public static final int OFFSET_Z = 0;
    public static final int BLOCK_COUNT = 302625;
    public static final String RESOURCE = "/assets/" + ModList.ModIds.GT_NOT_GOOD
        + "/structures/super_particle_collider.pattern.gz";
    private static final String[][] SHAPE = loadShape();

    private SuperParticleColliderStructure() {}

    /** Horizontal layers, top to bottom and front to back; spaces remain unconstrained. */
    public static String[][] shape() {
        String[][] shape = new String[HEIGHT][];
        for (int row = 0; row < HEIGHT; row++) shape[row] = SHAPE[row].clone();
        return shape;
    }

    private static String[][] loadShape() {
        String[][] result = new String[HEIGHT][LENGTH];
        try (InputStream stream = SuperParticleColliderStructure.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IOException("Missing collider structure: " + RESOURCE);
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new GZIPInputStream(stream), StandardCharsets.US_ASCII))) {
                if (!(WIDTH + " " + HEIGHT + " " + LENGTH).equals(reader.readLine()))
                    throw new IOException("Invalid collider dimensions");
                int blocks = 0;
                int controllers = 0;
                for (int aisle = 0; aisle < LENGTH; aisle++) {
                    for (int row = 0; row < HEIGHT; row++) {
                        String source = reader.readLine();
                        if (source == null || source.length() != WIDTH)
                            throw new IOException("Invalid collider row at " + aisle + ", " + row);
                        for (int column = 0; column < WIDTH; column++) {
                            char symbol = source.charAt(column);
                            if (" @#ABDEFGIJKLN".indexOf(symbol) < 0)
                                throw new IOException("Invalid collider symbol: " + symbol);
                            if (symbol != ' ') blocks++;
                            if (symbol == '@') controllers++;
                        }
                        result[row][LENGTH - aisle - 1] = source.replace('@', '~');
                    }
                }
                if (
                    reader.readLine() != null || blocks != BLOCK_COUNT
                        || controllers != 1
                        || result[OFFSET_Y][OFFSET_Z].charAt(OFFSET_X) != '~'
                ) throw new IOException("Invalid collider geometry or controller");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load collider structure", exception);
        }
        return result;
    }
}
