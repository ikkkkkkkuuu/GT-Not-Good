// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.common.machines.multiblock.structure;

// GTLCore SUPERCONDUCTING_ELECTROMAGNETISM, 6443c848264fb15c90cf50e031f146522616a8b9.
// Source and modifications: reference/ASSEMBLY_ELECTROMAGNETIC_STRUCTURES.md.
public final class SuperconductingElectromagneticStructure {

    public static final int WIDTH = 15;
    public static final int HEIGHT = 21;
    public static final int LENGTH = 15;
    public static final int OFFSET_X = 7;
    public static final int OFFSET_Y = 20;
    public static final int OFFSET_Z = 0;

    private static final String[][] SLICES = {
        { "     AAAAA     ", "       A       ", "       A       ", "       A       ", "       A       ",
            "       A       ", "       A       ", "       A       ", "       A       ", "       A       ",
            "       A       ", "       A       ", "       A       ", "       A       ", "       A       ",
            "       A       ", "       A       ", "       A       ", "       A       ", "       A       ",
            "     AAAAA     " },
        { "   AAAAAAAAA   ", "     AAAAA     ", "     AAAAA     ", "     AAAAA     ", "     BBBBB     ",
            "     BBBBB     ", "     BBBBB     ", "     BBBBB     ", "     BBBBB     ", "     BBBBB     ",
            "     BBBBB     ", "     BBBBB     ", "     BBBBB     ", "     BBBBB     ", "     BBBBB     ",
            "     BBBBB     ", "     BBBBB     ", "     AAAAA     ", "     AAAAA     ", "     AAAAA     ",
            "   AAAAAAAAA   " },
        { "  AAAAAAAAAAA  ", "   AACCCCCAA   ", "   AACCCCCAA   ", "   AA     AA   ", "   AB     BA   ",
            "   AB     BA   ", "   AB     BA   ", "   AB     BA   ", "   AB     BA   ", "   AB     BA   ",
            "   AB     BA   ", "   AB     BA   ", "   AB     BA   ", "   AB     BA   ", "   AB     BA   ",
            "   AB     BA   ", "   AB     BA   ", "   AA     AA   ", "   AACCCCCAA   ", "   AACCCCCAA   ",
            "  AAAAAAAAAAA  " },
        { " AAAAAAAAAAAAA ", "  ACCCDDDCCCA  ", "  ACCC   CCCA  ", "  A         A  ", "  A         A  ",
            "  A         A  ", "  A         A  ", "  A         A  ", "  A         A  ", "  A         A  ",
            "  A         A  ", "  A         A  ", "  A         A  ", "  A         A  ", "  A         A  ",
            "  A         A  ", "  A         A  ", "  A         A  ", "  ACCC   CCCA  ", "  ACCCDDDCCCA  ",
            " AAAAAAAAAAAAA " },
        { " AAAAAAAAAAAAA ", "  ACEEEEEEECA  ", "  AC       CA  ", "  A         A  ", "  B   CCC   B  ",
            "  B         B  ", "  B         B  ", "  B   CCC   B  ", "  B         B  ", "  B         B  ",
            "  B   CCC   B  ", "  B         B  ", "  B         B  ", "  B   CCC   B  ", "  B         B  ",
            "  B         B  ", "  B   CCC   B  ", "  A         A  ", "  AC       CA  ", "  ACEEEEEEECA  ",
            " AAAAAAAAAAAAA " },
        { "AAAAAAAAAAAAAAA", " ACCEEEEEEECCA ", " ACC       CCA ", " A           A ", " B   C   C   B ",
            " B           B ", " B           B ", " B   C   C   B ", " B           B ", " B           B ",
            " B   C   C   B ", " B           B ", " B           B ", " B   C   C   B ", " B           B ",
            " B           B ", " B   C   C   B ", " A           A ", " ACC       CCA ", " ACCEEEEEEECCA ",
            "AAAAAAAFAAAAAAA" },
        { "AAAAAAAAAAAAAAA", " ACDEEECEEEDCA ", " AC   ECE   CA ", " A    ECE    A ", " B  C ECE C  B ",
            " B    ECE    B ", " B    ECE    B ", " B  C ECE C  B ", " B    ECE    B ", " B    ECE    B ",
            " B  C ECE C  B ", " B    ECE    B ", " B    ECE    B ", " B  C ECE C  B ", " B    ECE    B ",
            " B    ECE    B ", " B  C ECE C  B ", " A    ECE    A ", " AC   ECE   CA ", " ACDEEECEEEDCA ",
            "AAAAAAFFFAAAAAA" },
        { "AAAAAAAAAAAAAAA", "AACDEECHCEEDCAA", "AAC   CHC   CAA", "AA    CHC    AA", "AB  C CHC C  BA",
            "AB    CHC    BA", "AB    CHC    BA", "AB  C CHC C  BA", "AB    CHC    BA", "AB    CHC    BA",
            "AB  C CHC C  BA", "AB    CHC    BA", "AB    CHC    BA", "AB  C CHC C  BA", "AB    CHC    BA",
            "AB    CHC    BA", "AB  C CHC C  BA", "AA    CHC    AA", "AAC   CHC   CAA", "AACDEECHCEEDCAA",
            "AAAAAFFFFFAAAAA" },
        { "   AAAAAAAAA   ", "     FFAFF     ", "     FFAFF     ", "     FFAFF     ", "     BBBBB     ",
            "     BBBBB     ", "     BBBBB     ", "     BBBBB     ", "     BBBBB     ", "     BBBBB     ",
            "     BBBBB     ", "     BBBBB     ", "     BBBBB     ", "     BBBBB     ", "     BBBBB     ",
            "     BBBBB     ", "     BBBBB     ", "     AAAAA     ", "     AAAAA     ", "     AAAAA     ",
            "   AAAAAAAAA   " },
        { "     AA~AA     ", "       A       ", "       A       ", "       A       ", "       A       ",
            "       A       ", "       A       ", "       A       ", "       A       ", "       A       ",
            "       A       ", "       A       ", "       A       ", "       A       ", "       A       ",
            "       A       ", "       A       ", "       A       ", "       A       ", "       A       ",
            "     AAAAA     " } };
    private static final int[] SLICE_ORDER = { 0, 1, 2, 3, 4, 5, 6, 7, 6, 5, 4, 3, 2, 8, 9 };

    private SuperconductingElectromagneticStructure() {}

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
