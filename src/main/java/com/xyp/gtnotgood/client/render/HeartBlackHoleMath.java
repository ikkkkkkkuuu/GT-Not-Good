// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.client.render;

public final class HeartBlackHoleMath {

    public static final float BLACK_HOLE_AND_DISK_RADIUS = 29;
    public static final float SPACE_SOLID_RADIUS = 0.7F * 25 / 29;
    public static final float SPACE_FADE_RADIUS = 1.45F * 25 / 29;
    public static final float ROTATION_SPEED = 0.3F;
    public static final float CULLING_RADIUS = BLACK_HOLE_AND_DISK_RADIUS * SPACE_FADE_RADIUS + 4;
    private static final float TAU = 6.2831855F;
    private static final float MIN_ROTATION_SPEED = 0.010F;
    private static final float MAX_ROTATION_SPEED = 0.024F;
    private static final float VERTICAL_ROTATION_LIMIT = 0.5235987F;
    private static final long ROTATION_PHASE_SALT = 0x13579BDF2468ACE0L;
    private static final long ROTATION_SPEED_SALT = 0x2468ACE013579BDFL;
    private static final long ROTATION_DIRECTION_SALT = 0x5DEECE66DL;
    private static final long ROTATION_AXIS_SALT_STEP = 0x1F123BB5L;

    private HeartBlackHoleMath() {}

    public static long positionSeed(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | y & 0xFFFL;
    }

    public static float rotation(long seed, float tick, int axis) {
        long salt = ROTATION_AXIS_SALT_STEP * axis;
        float phase = hashUnit(seed, ROTATION_PHASE_SALT + salt) * TAU;
        float speed = MIN_ROTATION_SPEED
            + (MAX_ROTATION_SPEED - MIN_ROTATION_SPEED) * hashUnit(seed, ROTATION_SPEED_SALT + salt);
        float direction = hashUnit(seed, ROTATION_DIRECTION_SALT + salt) < 0.5F ? -1 : 1;
        float angle = phase + tick * 0.05F * speed * direction;
        return axis == 0 ? (float) Math.sin(angle) * VERTICAL_ROTATION_LIMIT : angle;
    }

    private static float hashUnit(long seed, long salt) {
        long value = seed ^ salt;
        value ^= value >>> 33;
        value *= -49064778989728563L;
        value ^= value >>> 33;
        value *= -4265267296055464877L;
        value ^= value >>> 33;
        return ((value >>> 40) & 0xFFFFFFL) / (float) 0x1000000;
    }

    public static float[] cameraOffset(float[] matrix) {
        if (matrix.length != 16) throw new IllegalArgumentException("Invalid view matrix");
        double a = matrix[0], b = matrix[4], c = matrix[8];
        double d = matrix[1], e = matrix[5], f = matrix[9];
        double g = matrix[2], h = matrix[6], i = matrix[10];
        double determinant = a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g);
        if (Math.abs(determinant) < 0.00000001) throw new IllegalArgumentException("Singular view matrix");
        double x = matrix[12], y = matrix[13], z = matrix[14];
        return new float[] {
            (float) -((e * i - f * h) * x + (c * h - b * i) * y + (b * f - c * e) * z) / (float) determinant,
            (float) -((f * g - d * i) * x + (a * i - c * g) * y + (c * d - a * f) * z) / (float) determinant,
            (float) -((d * h - e * g) * x + (b * g - a * h) * y + (a * e - b * d) * z) / (float) determinant };
    }

    public static float[] sphereVertices(int latitudes, int longitudes) {
        if (latitudes < 2 || longitudes < 3) throw new IllegalArgumentException("Invalid volume sphere detail");
        float[] vertices = new float[latitudes * longitudes * 18];
        int index = 0;
        for (int latitude = 0; latitude < latitudes; latitude++) {
            double latitude0 = -Math.PI / 2 + Math.PI * latitude / latitudes;
            double latitude1 = -Math.PI / 2 + Math.PI * (latitude + 1) / latitudes;
            for (int longitude = 0; longitude < longitudes; longitude++) {
                double longitude0 = 2 * Math.PI * longitude / longitudes;
                double longitude1 = 2 * Math.PI * (longitude + 1) / longitudes;
                index = vertex(vertices, index, latitude0, longitude0);
                index = vertex(vertices, index, latitude1, longitude0);
                index = vertex(vertices, index, latitude1, longitude1);
                index = vertex(vertices, index, latitude0, longitude0);
                index = vertex(vertices, index, latitude1, longitude1);
                index = vertex(vertices, index, latitude0, longitude1);
            }
        }
        return vertices;
    }

    private static int vertex(float[] vertices, int index, double latitude, double longitude) {
        double horizontalRadius = Math.cos(latitude);
        vertices[index++] = (float) (horizontalRadius * Math.cos(longitude));
        vertices[index++] = (float) Math.sin(latitude);
        vertices[index++] = (float) (horizontalRadius * Math.sin(longitude));
        return index;
    }
}
