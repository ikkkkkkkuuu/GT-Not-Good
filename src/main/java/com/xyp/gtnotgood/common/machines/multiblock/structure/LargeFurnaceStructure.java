// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.common.machines.multiblock.structure;

public final class LargeFurnaceStructure {

    public static final int WIDTH = 11, HEIGHT = 13, LENGTH = 12;
    public static final int OFFSET_X = 5, OFFSET_Y = 11, OFFSET_Z = 0;
    private static final String[][] SHAPE = {
        { "           ", "           ", "           ", "    JJJ    ", "    JCJ    ", "    JJJ    ", "           ",
            "    JJJ    ", "    JCJ    ", "    JJJ    ", "           ", "           " },
        { "           ", "           ", "           ", "    DDD    ", "    D D    ", "    DDD    ", "           ",
            "    DDD    ", "    D D    ", "    DDD    ", "           ", "           " },
        { "           ", "           ", "           ", "    DDD    ", "    D D    ", "    DDD    ", "           ",
            "    DDD    ", "    D D    ", "    DDD    ", "           ", "           " },
        { "           ", "           ", "           ", "    DDD    ", "    D D    ", "    DDD    ", "           ",
            "    DDD    ", "    D D    ", "    DDD    ", "           ", "           " },
        { "           ", " JJJJJJJJJ ", " JLLLLLLLJ ", " JLEEEEELJ ", " JLEEEEELJ ", " JLEEEEELJ ", " JLCLCLCLJ ",
            " JLEEEEELJ ", " JLEEEEELJ ", " JLEEEEELJ ", " JJJJJJJJJ ", "           " },
        { "           ", " JLLLLLLLJ ", " LEEEEEEEL ", " LE     EL ", " LE     EL ", " LE     EL ", " LE     EL ",
            " LE     EL ", " LE     EL ", " LEEEEEEEL ", " JLLLLLLLJ ", "           " },
        { "           ", " JLMMMMMLJ ", " LD     DL ", "JL C C C LJ", " LCCCCCCCL ", " L C C C L ", "JLEC C CELJ",
            " L C C C L ", " LCCCCCCCL ", "JLD     DLJ", " JLLLLLLLJ ", "           " },
        { "           ", " JLMMMMMLJ ", " LD     DL ", "JG       GJ", " GE     EG ", " G       G ", "JGE     EGJ",
            " G       G ", " GE     EG ", "JLD     DLJ", " JLLLLLLLJ ", "           " },
        { "           ", " JLLLLLLLJ ", " LD     DL ", "LG       GL", " GE     EG ", " G       G ", "LGE     EGL",
            " G       G ", " GE     EG ", "LLD     DLL", " JLLLLLLLJ ", "           " },
        { "   LLLLL   ", " JLLLLLLLJ ", " LD     DL ", "JG       GJ", " GE     EG ", " G       G ", "JGE     EGJ",
            " G       G ", " GE     EG ", "JLD     DLJ", " JLLLLLLLJ ", "           " },
        { "   LLLLL   ", " JLLLLLLLJ ", " LEEEEEEEL ", "JKE     EBJ", " KE     EB ", " KE     EB ", "JKE     EBJ",
            " KE     EB ", " KE     EB ", "JLEEEEEEELJ", " JLLLLLLLJ ", "           " },
        { "   LL~LL   ", " JLJJJJJLJ ", " LHHHIHHHL ", "JLHHHHHHHLJ", " LHHHHHHHL ", " LHHHHHHHL ", "JLHHHHHHHLJ",
            " LHHHHHHHL ", " LHHHHHHHL ", "JLHHHHHHHLJ", " JLLLLLLLJ ", "           " },
        { "   LLLLL   ", " JLLLLLLLJ ", " JLLLLLLLJ ", "JJLLLLLLLJJ", " JLLLLLLLJ ", " JLLLLLLLJ ", "JJLLLLLLLJJ",
            " JLLLLLLLJ ", " JLLLLLLLJ ", "JJLLLLLLLJJ", " JJJJJJJJJ ", "           " } };

    private LargeFurnaceStructure() {}

    public static String[][] shape() {
        String[][] result = new String[HEIGHT][];
        for (int y = 0; y < HEIGHT; y++) result[y] = SHAPE[y].clone();
        return result;
    }
}
