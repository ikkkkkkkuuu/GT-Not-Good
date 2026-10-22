// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.common.machines.multiblock.structure;

// GTLCore FIELD_EXTRUDER_FACTORY, 6443c848264fb15c90cf50e031f146522616a8b9.
// Source and modifications: META-INF/field-extruder-port/NOTICE.md.
public final class FieldExtruderStructure {

    public static final int WIDTH = 15;
    public static final int HEIGHT = 9;
    public static final int LENGTH = 63;
    public static final int OFFSET_X = 7;
    public static final int OFFSET_Y = 1;
    public static final int OFFSET_Z = 0;

    private static final String[] REAR = { "AAB   AAA   BAA", "CAB   AAA   BAC", "CAB   AAA   BAC", "C             C",
        "C             C", "C             C", "C     BBB     C", "C     AAA     C", "CCCCCCCCCCCCCCC" };
    private static final String[] CONTROLLER = { "AAB   AAA   BAA", "CAB   A~A   BAC", "CAB   AAA   BAC",
        "C             C", "C             C", "C             C", "C     BBB     C", "C     AAA     C",
        "CCCCCCCCCCCCCCC" };
    private static final String[] FRAME = { "AAB   AAA   BAA", "CAB   GGG   BAC", "CAB   GGG   BAC", "C             C",
        "C             C", "C             C", "C     BBB     C", "C     AAA     C", "CCCCCCCCCCCCCCC" };
    private static final String[] SHAFT = { "AAB   AAA   BAA", " ABDDDEFEDDDBA ", " AB   GEG   BA ", "       D       ",
        "       D       ", "       D       ", "      BBB      ", "      AAA      ", "               " };
    private static final String[] BRIDGE = { "      AAA      ", "      GGG      ", "      GGG      ", "               ",
        "               ", "               ", "               ", "               ", "               " };

    private FieldExtruderStructure() {}

    /** Horizontal layers, front to back; pass to StructureUtility.transpose. Spaces remain unconstrained. */
    public static String[][] shape() {
        String[][] shape = new String[HEIGHT][LENGTH];
        for (int depth = 0; depth < LENGTH; depth++) {
            int sourceDepth = LENGTH - depth - 1;
            String[] slice = sourceDepth == 0 ? REAR
                : sourceDepth == LENGTH - 1 ? CONTROLLER
                    : sourceDepth % 4 == 1 ? SHAFT : sourceDepth % 4 == 3 ? BRIDGE : FRAME;
            for (int row = 0; row < HEIGHT; row++) shape[row][depth] = slice[row];
        }
        return shape;
    }
}
