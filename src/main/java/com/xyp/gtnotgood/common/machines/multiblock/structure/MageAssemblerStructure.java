// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.common.machines.multiblock.structure;

// GTLCore MAGE_ASSEMBLER, 6443c848264fb15c90cf50e031f146522616a8b9.
// Source and modifications: reference/ASSEMBLY_ELECTROMAGNETIC_STRUCTURES.md.
public final class MageAssemblerStructure {

    public static final int WIDTH = 9;
    public static final int HEIGHT = 10;
    public static final int LENGTH = 33;
    public static final int OFFSET_X = 4;
    public static final int OFFSET_Y = 5;
    public static final int OFFSET_Z = 0;

    private static final String[][] SLICES = {
        { "aaaaaaaaa", "aaaaaaaaa", "aaaaaaaaa", "aaaaaaaaa", "aaaa-aaaa", "aaaaaaaaa", "aaaaaaaaa", " aaaaaaa ",
            "         ", "         " },
        { "aaaaaaaaa", "c       c", "c  aaa  c", "c       c", "c       c", "c       c", "a       a", "d       d",
            " daaaaad ", "         " },
        { "aaaaaaaaa", "c       c", "c  aaa  c", "c e   e c", "cf     fc", "cf     fc", "af     fa", "d       d",
            " da   ad ", "   aaa   " },
        { "aaaaaaaaa", "c  g g  c", "c  aaa  c", "c       c", "c       c", "c       c", "a       a", "d       d",
            " da   ad ", "   aha   " },
        { "aaaaaaaaa", "ci     ic", "ci aaa ic", "ci     ic", "ci     ic", "ci  f  ic", "aii e iia", "d iieii d",
            " da   ad ", "   aha   " },
        { "aaaaaaaaa", "c       c", "c  aaa  c", "c       c", "c       c", "c       c", "a       a", "d       d",
            " da   ad ", "   aha   " },
        { "aaaaaaaaa", "c       c", "c  aaa  c", "c e   e c", "cf     fc", "cf     fc", "af     fa", "d       d",
            " da   ad ", "   aha   " },
        { "aaaaaaaaa", "ai     ia", "ai aaa ia", "ai     ia", "ai     ia", "ai  f  ia", "aii e iia", "d iieii d",
            " da   ad ", "   aha   " },
        { "aaaaaaaaa", "aaaaaaaaa", "aaaaaaaaa", "aaaaaaaaa", "aaaa~aaaa", "aaaaaaaaa", "aaaaaaaaa", " aaaaaaa ",
            "         ", "         " } };
    private static final int[] SLICE_ORDER = { 0, 1, 2, 3, 4, 5, 6, 5, 7, 5, 6, 5, 4, 5, 6, 5, 7, 5, 6, 5, 4, 5, 6, 5,
        7, 5, 6, 5, 4, 3, 2, 1, 8 };

    private MageAssemblerStructure() {}

    /** Horizontal layers for StructureUtility.transpose; spaces remain unconstrained. */
    public static String[][] shape() {
        String[][] shape = new String[HEIGHT][LENGTH];
        for (int depth = 0; depth < LENGTH; depth++) {
            String[] slice = SLICES[SLICE_ORDER[LENGTH - depth - 1]];
            for (int row = 0; row < HEIGHT; row++) shape[row][depth] = slice[HEIGHT - row - 1];
        }
        return shape;
    }
}
