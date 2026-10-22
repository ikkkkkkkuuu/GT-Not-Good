// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.client.render;

import net.minecraft.util.MathHelper;

public final class DimensionalFocusCoreGeometry {

    public static final int CYLINDERS = 6;
    public static final int SIDES = 10;
    public static final int VERTEX_COUNT = CYLINDERS * SIDES * 6;
    public static final float RADIUS = 0.5F;
    public static final float HEIGHT = 8F;
    private static final float[] VERTICES = createVertices();

    private DimensionalFocusCoreGeometry() {}

    public static float[] vertices() {
        return VERTICES.clone();
    }

    public static int color(int coilRgb, long offsetTimer, float partialTicks) {
        float distance = Math.abs((Math.abs(offsetTimer % 100) + partialTicks) - 50) / 50;
        float blend = 0.1F + distance * distance;
        return channel(coilRgb, 24, blend) << 24 | channel(coilRgb, 16, blend) << 16
            | channel(coilRgb, 8, blend) << 8
            | channel(coilRgb, 0, blend);
    }

    private static int channel(int color, int shift, float blend) {
        float start = (color >>> shift & 255) / 255F;
        return Math.min(255, (int) ((start + blend * (1 - start)) * 255));
    }

    private static float[] createVertices() {
        float[] vertices = new float[VERTEX_COUNT * 3];
        float step = (float) (2 * Math.PI / SIDES);
        int offset = 0;
        for (int cylinder = 0; cylinder < CYLINDERS; cylinder++) {
            for (int side = 0; side < SIDES; side++) {
                float firstAngle = side * step;
                float secondAngle = (side + 1) * step;
                float firstX = MathHelper.cos(firstAngle) * RADIUS;
                float firstZ = MathHelper.sin(firstAngle) * RADIUS;
                float secondX = MathHelper.cos(secondAngle) * RADIUS;
                float secondZ = MathHelper.sin(secondAngle) * RADIUS;
                offset = vertex(vertices, offset, cylinder, firstX, 0, firstZ);
                offset = vertex(vertices, offset, cylinder, secondX, 0, secondZ);
                offset = vertex(vertices, offset, cylinder, secondX, HEIGHT, secondZ);
                offset = vertex(vertices, offset, cylinder, firstX, 0, firstZ);
                offset = vertex(vertices, offset, cylinder, secondX, HEIGHT, secondZ);
                offset = vertex(vertices, offset, cylinder, firstX, HEIGHT, firstZ);
            }
        }
        return vertices;
    }

    private static int vertex(float[] vertices, int offset, int cylinder, float x, float y, float z) {
        switch (cylinder) {
            case 0 -> {
                vertices[offset++] = x;
                vertices[offset++] = y;
                vertices[offset++] = z;
            }
            case 1 -> {
                vertices[offset++] = x;
                vertices[offset++] = z;
                vertices[offset++] = -y;
            }
            case 2 -> {
                vertices[offset++] = x;
                vertices[offset++] = -y;
                vertices[offset++] = -z;
            }
            case 3 -> {
                vertices[offset++] = x;
                vertices[offset++] = -z;
                vertices[offset++] = y;
            }
            case 4 -> {
                vertices[offset++] = y;
                vertices[offset++] = -z;
                vertices[offset++] = -x;
            }
            case 5 -> {
                vertices[offset++] = -y;
                vertices[offset++] = -z;
                vertices[offset++] = x;
            }
            default -> throw new IllegalArgumentException("cylinder");
        }
        return offset;
    }
}
