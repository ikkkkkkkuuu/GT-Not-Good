package com.xyp.gtnotgood.client.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KerrNewmanBlackHoleMathTest {

    @Test
    public void animationGrowsAndShrinksUsingWorldTime() {
        KerrNewmanBlackHoleMath.Animation animation = new KerrNewmanBlackHoleMath.Animation();
        assertEquals(0, animation.update(false, 100, 0), 0);
        assertEquals(0, animation.update(true, 100, 0), 0);
        assertEquals(0.5F, animation.update(true, 130, 0), 0);
        assertEquals(1, animation.update(true, 160, 0), 0);
        assertEquals(1, animation.update(false, 200, 0), 0);
        assertEquals(0.5F, animation.update(false, 230, 0), 0);
        assertEquals(0, animation.update(false, 260, 0), 0);
        assertEquals(0, animation.update(true, 270, 0), 0);
        assertEquals(0.25F, animation.update(true, 285, 0), 0);
        assertEquals(0.25F, animation.update(false, 285, 0), 0);
        assertEquals(0, animation.update(false, 300, 0), 0);
        assertEquals(0, animation.update(true, 1, 0), 0);
        assertEquals(0.5F / 60, animation.update(true, 1, 0.5F), 0.00001F);
    }

    @Test
    public void sphereUsesOutwardFacesAndPreservesOriginalUnitRadius() {
        float[] vertices = KerrNewmanBlackHoleMath.sphereVertices(16, 32);
        assertEquals(16 * 32 * 6 * 3, vertices.length);
        assertEquals(0, vertices[0], 0);
        assertEquals(1, vertices[1], 0);
        assertEquals(0, vertices[2], 0);
        assertEquals((float) Math.sin(Math.PI / 16), vertices[6], 0.00001F);
        assertEquals((float) Math.cos(Math.PI / 16), vertices[7], 0.00001F);
        for (int offset = 0; offset < vertices.length; offset += 3) {
            float length = vertices[offset] * vertices[offset] + vertices[offset + 1] * vertices[offset + 1]
                + vertices[offset + 2] * vertices[offset + 2];
            assertEquals(1, length, 0.00001F);
        }
        int outwardFaces = 0;
        for (int offset = 0; offset < vertices.length; offset += 9) {
            float ax = vertices[offset + 3] - vertices[offset];
            float ay = vertices[offset + 4] - vertices[offset + 1];
            float az = vertices[offset + 5] - vertices[offset + 2];
            float bx = vertices[offset + 6] - vertices[offset];
            float by = vertices[offset + 7] - vertices[offset + 1];
            float bz = vertices[offset + 8] - vertices[offset + 2];
            float nx = ay * bz - az * by;
            float ny = az * bx - ax * bz;
            float nz = ax * by - ay * bx;
            if (nx * nx + ny * ny + nz * nz < 0.00000001F) continue;
            assertTrue(nx * vertices[offset] + ny * vertices[offset + 1] + nz * vertices[offset + 2] > 0);
            outwardFaces++;
        }
        assertTrue(outwardFaces > 900);
        assertEquals(0, KerrNewmanBlackHoleMath.detailLevel(99.9F));
        assertEquals(1, KerrNewmanBlackHoleMath.detailLevel(100));
        assertEquals(2, KerrNewmanBlackHoleMath.detailLevel(220));
        assertEquals(98.5F, KerrNewmanBlackHoleMath.coreMaskRadius(100), 0);
        assertEquals(994, KerrNewmanBlackHoleMath.coreMaskRadius(1000), 0);
    }

    @Test
    public void projectionTracksTheFramebufferAndRejectsBehindCamera() {
        float[] modelView = identity();
        modelView[14] = -20;
        float[] projection = new float[16];
        projection[0] = 1;
        projection[5] = 2;
        projection[10] = -1;
        projection[11] = -1;
        projection[14] = -0.1F;
        KerrNewmanBlackHoleMath.Projection result = KerrNewmanBlackHoleMath.projectSphere(modelView, projection, 1600,
            800, 2.5F, 4.5F);
        assertNotNull(result);
        assertEquals(800, result.centerX, 0);
        assertEquals(400, result.centerY, 0);
        assertEquals(100, result.innerRadius, 0.00001F);
        assertEquals(180, result.outerRadius, 0.00001F);
        assertTrue(result.innerRadius < result.outerRadius);
        modelView[14] = 20;
        assertNull(KerrNewmanBlackHoleMath.projectSphere(modelView, projection, 1600, 800, 2.5F, 4.5F));
    }

    private static float[] identity() {
        float[] matrix = new float[16];
        matrix[0] = matrix[5] = matrix[10] = matrix[15] = 1;
        return matrix;
    }
}
