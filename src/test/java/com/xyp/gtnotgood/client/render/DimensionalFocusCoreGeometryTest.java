package com.xyp.gtnotgood.client.render;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DimensionalFocusCoreGeometryTest {

    @Test
    public void retainsTheOriginalCylinderVertexStream() {
        float[] vertices = DimensionalFocusCoreGeometry.vertices();
        assertEquals(360, DimensionalFocusCoreGeometry.VERTEX_COUNT);
        assertEquals(1080, vertices.length);
        assertArrayEquals(new float[] { 0.5F, 0, 0 }, point(vertices, 0), 0);
        assertArrayEquals(point(vertices, 0), point(vertices, 3), 0);
        assertArrayEquals(point(vertices, 2), point(vertices, 4), 0);
        assertArrayEquals(new float[] { 0.5F, 8, 0 }, point(vertices, 5), 0);
        vertices[0] = 100;
        assertEquals(0.5F, DimensionalFocusCoreGeometry.vertices()[0], 0);
    }

    @Test
    public void preservesAllSixAxialTransformsAndOpenEnds() {
        float[] vertices = DimensionalFocusCoreGeometry.vertices();
        int[] axes = { 1, 2, 1, 2, 0, 0 };
        int[] signs = { 1, -1, -1, 1, 1, -1 };
        for (int cylinder = 0; cylinder < 6; cylinder++) {
            int axis = axes[cylinder];
            int sign = signs[cylinder];
            for (int vertex = cylinder * 60; vertex < (cylinder + 1) * 60; vertex++) {
                float[] point = point(vertices, vertex);
                float along = point[axis] * sign;
                assertEquals(along == 0 ? 0 : 8, along, 0);
                float radialSquared = 0;
                for (int coordinate = 0; coordinate < 3; coordinate++)
                    if (coordinate != axis) radialSquared += point[coordinate] * point[coordinate];
                assertEquals(0.25F, radialSquared, 0.00001F);
            }
        }
    }

    @Test
    public void preservesOriginalAlphaAndWhitePulseQuantization() {
        assertEquals(0x19191919, DimensionalFocusCoreGeometry.color(0, 50, 0));
        assertEquals(0x59595959, DimensionalFocusCoreGeometry.color(0, 25, 0));
        assertEquals(0x19FF1919, DimensionalFocusCoreGeometry.color(0xFF0000, 50, 0));
        assertEquals(0xFFFFFFFF, DimensionalFocusCoreGeometry.color(0x134785, 0, 0));
        assertEquals(DimensionalFocusCoreGeometry.color(0x134785, 37, 0.5F),
            DimensionalFocusCoreGeometry.color(0x134785, 137, 0.5F));
        assertEquals(DimensionalFocusCoreGeometry.color(0x134785, 37, 0.5F),
            DimensionalFocusCoreGeometry.color(0x134785, -37, 0.5F));
    }

    private static float[] point(float[] vertices, int vertex) {
        int offset = vertex * 3;
        return new float[] { vertices[offset], vertices[offset + 1], vertices[offset + 2] };
    }
}
