// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.common.machines.multiblock.structure;

// GTLCore SUPER_PARTICLE_COLLIDER, 6443c848264fb15c90cf50e031f146522616a8b9.
// Source and modifications: reference/SUPER_PARTICLE_COLLIDER_STRUCTURE.md.
public final class SuperParticleColliderStructure {

    public static final int WIDTH = 47;
    public static final int HEIGHT = 7;
    public static final int LENGTH = 47;
    public static final int OFFSET_X = 23;
    public static final int OFFSET_Y = 3;
    public static final int OFFSET_Z = 0;

    private static final String[][] SLICES = {
        { "                                               ", "                                               ",
            "                    fcccccf                    ", "                    fcccccf                    ",
            "                    fcccccf                    ", "                                               ",
            "                                               " },
        { "                                               ", "                    fcccccf                    ",
            "                   cceeeeecc                   ", "                   cceeeeecc                   ",
            "                   cceeeeecc                   ", "                    fcccccf                    ",
            "                                               " },
        { "                    fcccccf                    ", "                   cceeeeecc                   ",
            "                ccccceeeeeccccc                ", "                cccdddddddddccc                ",
            "                ccccceeeeeccccc                ", "                   cceeeeecc                   ",
            "                    fcccccf                    " },
        { "                    fcccccf                    ", "                ccccceeeeeccccc                ",
            "              cccccdddddddddccccc              ", "              ccdddddddddddddddcc              ",
            "              cccccdddddddddccccc              ", "                ccccceeeeeccccc                ",
            "                    fcccccf                    " },
        { "                    fcccccf                    ", "              ccccccceeeeeccccccc              ",
            "            ccccdddcceeeeeccdddcccc            ", "            ccdddddddddddddddddddcc            ",
            "            ccccdddcceeeeeccdddcccc            ", "              ccccccceeeeeccccccc              ",
            "                    fcccccf                    " },
        { "                                               ", "            ccccccc fcccccf ccccccc            ",
            "           cccddccccceeeeecccccddccc           ", "           cdddddddcceeeeeccdddddddc           ",
            "           cccddccccceeeeecccccddccc           ", "            ccccccc fcccccf ccccccc            ",
            "                                               " },
        { "                                               ", "           ccccc               ccccc           ",
            "          ccddccccc fcccccf cccccddcc          ", "          cdddddccc fcccccf cccdddddc          ",
            "          ccddccccc fcccccf cccccddcc          ", "           ccccc               ccccc           ",
            "                                               " },
        { "                                               ", "          cccc                   cccc          ",
            "         ccdcccc               ccccdcc         ", "         cddddcc               ccddddc         ",
            "         ccdcccc               ccccdcc         ", "          cccc                   cccc          ",
            "                                               " },
        { "                                               ", "         ccc                       ccc         ",
            "        ccdccc                   cccdcc        ", "        cdddcc                   ccdddc        ",
            "        ccdccc                   cccdcc        ", "         ccc                       ccc         ",
            "                                               " },
        { "                                               ", "        ccc                         ccc        ",
            "       ccdcc                       ccdcc       ", "       cdddc                       cdddc       ",
            "       ccdcc                       ccdcc       ", "        ccc                         ccc        ",
            "                                               " },
        { "                                               ", "       ccc                           ccc       ",
            "      ccdcc                         ccdcc      ", "      cdddc                         cdddc      ",
            "      ccdcc                         ccdcc      ", "       ccc                           ccc       ",
            "                                               " },
        { "                                               ", "      ccc                             ccc      ",
            "     ccdcc                           ccdcc     ", "     cdddc                           cdddc     ",
            "     ccdcc                           ccdcc     ", "      ccc                             ccc      ",
            "                                               " },
        { "                                               ", "     ccc                               ccc     ",
            "    ccdcc                             ccdcc    ", "    cdddc                             cdddc    ",
            "    ccdcc                             ccdcc    ", "     ccc                               ccc     ",
            "                                               " },
        { "                                               ", "    ccc                                 ccc    ",
            "   ccdcc                               ccdcc   ", "   cdddc                               cdddc   ",
            "   ccdcc                               ccdcc   ", "    ccc                                 ccc    ",
            "                                               " },
        { "                                               ", "   ccc                                   ccc   ",
            "  ccdcc                                 ccdcc  ", "  cdddc                                 cdddc  ",
            "  ccdcc                                 ccdcc  ", "   ccc                                   ccc   ",
            "                                               " },
        { "                                               ", "  ccc                                     ccc  ",
            " ccdcc                                   ccdcc ", " cdddc                                   cdddc ",
            " ccdcc                                   ccdcc ", "  ccc                                     ccc  ",
            "                                               " },
        { "  fff                                     fff  ", " fcccf                                   fcccf ",
            "fccdccf                                 fccdccf", "fcdddcf                                 fcdddcf",
            "fccdccf                                 fccdccf", " fcccf                                   fcccf ",
            "  fff                                     fff  " },
        { "  ccc                                     ccc  ", " ceeec                                   ceeec ",
            "ceedeec                                 ceedeec", "cedddec                                 cedddec",
            "ceedeec                                 ceedeec", " ceeec                                   ceeec ",
            "  ccc                                     ccc  " },
        { "                                               ", "                                               ",
            "                    fbbbbbf                    ", "                    fbb~bbf                    ",
            "                    fbbbbbf                    ", "                                               ",
            "                                               " } };
    private static final int[] SLICE_ORDER = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 12, 13, 13, 14, 14, 14, 15, 16,
        17, 17, 17, 17, 17, 16, 15, 14, 14, 14, 13, 13, 12, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 18 };

    private SuperParticleColliderStructure() {}

    /** Horizontal layers, front to back; pass to StructureUtility.transpose. Spaces remain unconstrained. */
    public static String[][] shape() {
        String[][] shape = new String[HEIGHT][LENGTH];
        for (int depth = 0; depth < LENGTH; depth++) {
            String[] slice = SLICES[SLICE_ORDER[LENGTH - depth - 1]];
            for (int row = 0; row < HEIGHT; row++) shape[row][depth] = slice[row];
        }
        return shape;
    }
}
