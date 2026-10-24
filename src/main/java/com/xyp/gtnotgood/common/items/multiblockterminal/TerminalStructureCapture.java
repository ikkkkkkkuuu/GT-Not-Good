// SPDX-License-Identifier: GPL-3.0-only
// StructureLib 1.4.45 StructureUtility.iterateV2; adapted for terminal position capture.
package com.xyp.gtnotgood.common.items.multiblockterminal;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.World;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.IStructureElement;

public final class TerminalStructureCapture {

    private TerminalStructureCapture() {}

    /** Mirrors the captured layout around the controller while retaining its existing rotation and flip. */
    public static List<Position> collect(Object context, IStructureElement<?>[] elements, World world,
        ExtendedFacing facing, int baseX, int baseY, int baseZ, int offsetA, int offsetB, int offsetC, boolean mirror) {
        int startA = -offsetA;
        int startB = -offsetB;
        int startC = -offsetC;
        int[] abc = { startA, startB, startC };
        int[] point = new int[3];
        int[] xyz = new int[3];
        List<Position> positions = new ArrayList<>();
        for (IStructureElement<?> element : elements) {
            if (element.isNavigating()) {
                abc[0] = (element.resetA() ? startA : abc[0]) + element.getStepA();
                abc[1] = (element.resetB() ? startB : abc[1]) + element.getStepB();
                abc[2] = (element.resetC() ? startC : abc[2]) + element.getStepC();
                continue;
            }
            point[0] = mirror ? -abc[0] : abc[0];
            point[1] = abc[1];
            point[2] = abc[2];
            facing.getWorldOffset(point, xyz);
            int x = baseX + xyz[0];
            int y = baseY + xyz[1];
            int z = baseZ + xyz[2];
            if (world.blockExists(x, y, z)) positions.add(new Position(context, element, world, x, y, z));
            abc[0]++;
        }
        return positions;
    }

    public record Position(Object context, IStructureElement<?> element, World world, int x, int y, int z) {}
}
