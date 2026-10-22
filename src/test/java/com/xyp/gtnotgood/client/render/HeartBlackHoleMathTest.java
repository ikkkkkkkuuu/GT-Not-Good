package com.xyp.gtnotgood.client.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HeartBlackHoleMathTest {

    @Test
    public void volumeSphereKeepsTheUpstreamMeshAndWorldRadius() {
        float[] vertices = HeartBlackHoleMath.sphereVertices(24, 48);
        assertEquals(24 * 48 * 6 * 3, vertices.length);
        assertEquals(29, HeartBlackHoleMath.BLACK_HOLE_AND_DISK_RADIUS, 0);
        assertEquals(36.25F, HeartBlackHoleMath.BLACK_HOLE_AND_DISK_RADIUS * HeartBlackHoleMath.SPACE_FADE_RADIUS,
            0.00001F);
        assertTrue(HeartBlackHoleMath.CULLING_RADIUS > 36.25F);
        int outwardFaces = 0;
        for (int index = 0; index < vertices.length; index += 3) {
            double length = Math.sqrt(vertices[index] * vertices[index] + vertices[index + 1] * vertices[index + 1]
                + vertices[index + 2] * vertices[index + 2]);
            assertEquals(1, length, 0.000001);
        }
        for (int index = 0; index < vertices.length; index += 9) {
            float ax = vertices[index + 3] - vertices[index];
            float ay = vertices[index + 4] - vertices[index + 1];
            float az = vertices[index + 5] - vertices[index + 2];
            float bx = vertices[index + 6] - vertices[index];
            float by = vertices[index + 7] - vertices[index + 1];
            float bz = vertices[index + 8] - vertices[index + 2];
            float nx = ay * bz - az * by;
            float ny = az * bx - ax * bz;
            float nz = ax * by - ay * bx;
            if (nx * nx + ny * ny + nz * nz < 0.00000001F) continue;
            assertTrue(nx * vertices[index] + ny * vertices[index + 1] + nz * vertices[index + 2] > 0);
            outwardFaces++;
        }
        assertTrue(outwardFaces >= 2100);
    }

    @Test
    public void rotationKeepsAStablePositionSeedAndBoundedVerticalTilt() {
        long seed = HeartBlackHoleMath.positionSeed(-137, 88, 256);
        assertEquals(seed, HeartBlackHoleMath.positionSeed(-137, 88, 256));
        assertFalse(seed == HeartBlackHoleMath.positionSeed(-136, 88, 256));
        for (int tick = 0; tick <= 100000; tick += 137) {
            float pitch = HeartBlackHoleMath.rotation(seed, tick, 0);
            assertTrue(Math.abs(pitch) <= 0.5235988F);
            assertEquals(pitch, HeartBlackHoleMath.rotation(seed, tick, 0), 0);
            assertFalse(Float.isNaN(HeartBlackHoleMath.rotation(seed, tick, 1)));
            assertFalse(Float.isNaN(HeartBlackHoleMath.rotation(seed, tick, 2)));
        }
        float firstStep = HeartBlackHoleMath.rotation(seed, 101, 1) - HeartBlackHoleMath.rotation(seed, 100, 1);
        float laterStep = HeartBlackHoleMath.rotation(seed, 10001, 1) - HeartBlackHoleMath.rotation(seed, 10000, 1);
        assertEquals(firstStep, laterStep, 0.00001F);
        assertTrue(Math.abs(firstStep) >= 0.00049F && Math.abs(firstStep) <= 0.00121F);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidSphereDetailIsRejected() {
        HeartBlackHoleMath.sphereVertices(1, 48);
    }

    @Test
    public void cameraRayOriginTracksEyeHeightAndThirdPersonTranslation() {
        float[] view = { 0, 0, -1, 0, 0, 1, 0, 0, 1, 0, 0, 0, -4, -1.62F, 0, 1 };
        float[] origin = HeartBlackHoleMath.cameraOffset(view);
        assertEquals(0, origin[0], 0.00001F);
        assertEquals(1.62F, origin[1], 0.00001F);
        assertEquals(4, origin[2], 0.00001F);
    }
}
