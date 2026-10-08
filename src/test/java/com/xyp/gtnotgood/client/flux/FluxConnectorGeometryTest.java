package com.xyp.gtnotgood.client.flux;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Test;

/** Guards against inside-out cuboids and mirrored connection models without requiring an OpenGL context. */
public class FluxConnectorGeometryTest {

    @Test
    public void everyEmittedFacePointsOutwardInEveryOrientation() throws Exception {
        Method vertices = FluxConnectorRenderer.class.getDeclaredMethod("vertices", ForgeDirection.class,
            double[].class, double[].class);
        vertices.setAccessible(true);
        for (int rotation = -1; rotation < 6; rotation++) {
            for (ForgeDirection face : ForgeDirection.VALID_DIRECTIONS) {
                double[][] quad = (double[][]) vertices.invoke(null, face, new double[] { 0, 0, 0 },
                    new double[] { 1, 1, 1 });
                double[] a = rotate(quad[3], rotation), b = rotate(quad[2], rotation), c = rotate(quad[1], rotation);
                double[] normal = rotate(new double[] { face.offsetX, face.offsetY, face.offsetZ }, rotation);
                double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
                double vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
                double dot = (uy * vz - uz * vy) * normal[0] + (uz * vx - ux * vz) * normal[1]
                    + (ux * vy - uy * vx) * normal[2];
                assertTrue("Outward winding: " + face + " rotation " + rotation, dot > 0);
            }
        }
    }

    @Test
    public void connectorFootReachesTheRequestedNeighborFace() throws Exception {
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            double[] foot = rotate(new double[] { 0, -0.5, 0 }, side.ordinal());
            assertEquals(side.offsetX * 0.5, foot[0], 0.000001);
            assertEquals(side.offsetY * 0.5, foot[1], 0.000001);
            assertEquals(side.offsetZ * 0.5, foot[2], 0.000001);
        }
    }

    private static double[] rotate(double[] point, int side) throws Exception {
        Method method = FluxConnectorRenderer.class.getDeclaredMethod("rotate", double.class, double.class,
            double.class, int.class);
        method.setAccessible(true);
        return (double[]) method.invoke(null, point[0], point[1], point[2], side);
    }
}
