// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.common.machines.multiblock.structure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import com.xyp.gtnotgood.utils.enums.ModList;

// GTLCore DIMENSIONAL_FOCUS_ENGRAVING_ARRAY, 6443c848264fb15c90cf50e031f146522616a8b9.
// Source and modifications: META-INF/dimensional-focus-port/NOTICE.md.
public final class DimensionalFocusEngravingStructure {

    public static final int WIDTH = 89;
    public static final int HEIGHT = 89;
    public static final int LENGTH = 89;
    public static final int OFFSET_X = 44;
    public static final int OFFSET_Y = 26;
    public static final int OFFSET_Z = 22;

    private static final String RESOURCE = "/assets/" + ModList.ModIds.GT_NOT_GOOD
        + "/structures/dimensional_focus_engraving_array.shape.gz";
    private static final String[][] SHAPE = loadShape();

    private DimensionalFocusEngravingStructure() {}

    /** Horizontal layers, top to bottom; pass to StructureUtility.transpose. Spaces remain unconstrained. */
    public static String[][] shape() {
        String[][] shape = new String[HEIGHT][];
        for (int row = 0; row < HEIGHT; row++) shape[row] = SHAPE[row].clone();
        return shape;
    }

    private static String[][] loadShape() {
        char[][][] shape = new char[HEIGHT][LENGTH][WIDTH];
        try (InputStream stream = DimensionalFocusEngravingStructure.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing engraving array structure: " + RESOURCE);
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new GZIPInputStream(stream), StandardCharsets.US_ASCII))) {
                // Upstream axes are BACK, UP, RIGHT; StructureLib axes are LEFT, DOWN, BACK.
                for (int aisle = 0; aisle < WIDTH; aisle++) {
                    for (int row = 0; row < HEIGHT; row++) {
                        String source = reader.readLine();
                        if (source == null || source.length() != LENGTH)
                            throw new IOException("Invalid engraving array structure row");
                        for (int depth = 0; depth < LENGTH; depth++) {
                            char symbol = source.charAt(depth);
                            if (symbol != ' ' && (symbol < 'A' || symbol > 'I'))
                                throw new IOException("Invalid engraving array structure symbol: " + symbol);
                            shape[HEIGHT - row - 1][depth][WIDTH - aisle - 1] = symbol == 'I' ? '~' : symbol;
                        }
                    }
                }
                if (reader.readLine() != null) throw new IOException("Unexpected engraving array structure data");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load engraving array structure", exception);
        }
        String[][] result = new String[HEIGHT][LENGTH];
        for (int row = 0; row < HEIGHT; row++) {
            for (int depth = 0; depth < LENGTH; depth++) result[row][depth] = new String(shape[row][depth]);
        }
        return result;
    }
}
