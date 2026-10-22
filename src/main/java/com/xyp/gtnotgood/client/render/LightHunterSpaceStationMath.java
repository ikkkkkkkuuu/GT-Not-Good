// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.client.render;

import java.util.Random;

public final class LightHunterSpaceStationMath {

    public static final int PLANET_COUNT = 13;
    public static final int ORBIT_SEGMENTS = 128;
    public static final double STAR_DISTANCE = 206;
    public static final double BEAM_DISTANCE = 174;
    public static final float STAR_SCALE = 0.20F;
    public static final float BEAM_RADIUS = 0.85F;
    public static final int CULLING_RADIUS = 56;
    private static final float TAU = (float) (2 * Math.PI);
    private static final int TABLE_SIZE = 4096;
    private static final double ANGLE_TO_INDEX = TABLE_SIZE / (2 * Math.PI);
    private static final float[] SIN = new float[TABLE_SIZE];
    private static final float[] COS = new float[TABLE_SIZE];

    static {
        for (int index = 0; index < TABLE_SIZE; index++) {
            double angle = index * 2 * Math.PI / TABLE_SIZE;
            SIN[index] = (float) Math.sin(angle);
            COS[index] = (float) Math.cos(angle);
        }
    }

    private LightHunterSpaceStationMath() {}

    public static long positionSeed(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | y & 0xFFFL;
    }

    public static double[] rotatedOffset(double x, double y, double z, int facing) {
        return switch (facing) {
            case 3 -> new double[] { 0.5 - z, 0.5 + y, 0.5 + x };
            case 4 -> new double[] { 0.5 - x, 0.5 + y, 0.5 - z };
            case 2 -> new double[] { 0.5 + z, 0.5 + y, 0.5 - x };
            default -> new double[] { 0.5 + x, 0.5 + y, 0.5 + z };
        };
    }

    public static Profile profile(long seed) {
        Orbit[] orbits = new Orbit[PLANET_COUNT];
        Rotation[] rotations = new Rotation[PLANET_COUNT];
        float[] sizes = new float[PLANET_COUNT];
        for (int index = 0; index < PLANET_COUNT; index++) {
            rotations[index] = rotation(new Random(seed + index * 2000L), 0.1F, 0.5F);
            Random random = new Random(seed + index * 1000L);
            float radius = 22 + random.nextFloat() * 31;
            float speed = 0.5F + random.nextFloat();
            float offset = random.nextFloat() * 360;
            float tilt = 70 + random.nextFloat() * 20;
            float theta = random.nextFloat() * TAU;
            orbits[index] = new Orbit(radius, speed, offset, tilt, (float) Math.cos(theta), (float) Math.sin(theta));
            sizes[index] = 1.3F + new Random(seed + index * 500L).nextFloat() * (2.7F - 1.3F);
        }
        return new Profile(rotation(new Random(seed), 0.5F, 2), orbits, rotations, sizes);
    }

    private static Rotation rotation(Random random, float minSpeed, float maxSpeed) {
        float theta = random.nextFloat() * TAU;
        float phi = (float) Math.acos(2F * random.nextFloat() - 1F);
        float x = (float) (Math.sin(phi) * Math.cos(theta));
        float y = (float) (Math.sin(phi) * Math.sin(theta));
        float z = (float) Math.cos(phi);
        float speed = minSpeed + (float) Math.sqrt(random.nextFloat()) * (maxSpeed - minSpeed);
        return new Rotation(x, y, z, speed, random.nextFloat() * 360);
    }

    public static float fastSin(float radians) {
        return SIN[(int) (radians * ANGLE_TO_INDEX) & (TABLE_SIZE - 1)];
    }

    public static float fastCos(float radians) {
        return COS[(int) (radians * ANGLE_TO_INDEX) & (TABLE_SIZE - 1)];
    }

    public static final class Profile {

        public final Rotation starRotation;
        public final Orbit[] orbits;
        public final Rotation[] planetRotations;
        public final float[] planetSizes;

        private Profile(Rotation starRotation, Orbit[] orbits, Rotation[] planetRotations, float[] planetSizes) {
            this.starRotation = starRotation;
            this.orbits = orbits;
            this.planetRotations = planetRotations;
            this.planetSizes = planetSizes;
        }
    }

    public static final class Rotation {

        public final float x, y, z, speed, offset;

        private Rotation(float x, float y, float z, float speed, float offset) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.speed = speed;
            this.offset = offset;
        }

        public float angle(float tick) {
            return (offset + tick * speed) % 360;
        }
    }

    public static final class Orbit {

        public final float radius, speed, offset, tilt, axisX, axisZ;

        private Orbit(float radius, float speed, float offset, float tilt, float axisX, float axisZ) {
            this.radius = radius;
            this.speed = speed;
            this.offset = offset;
            this.tilt = tilt;
            this.axisX = axisX;
            this.axisZ = axisZ;
        }

        public float angle(float tick) {
            return (offset + tick * speed) % 360;
        }

        public double[] position(float tick) {
            double angle = Math.toRadians(offset + tick * speed);
            float x = (float) (radius * Math.cos(angle));
            float z = (float) (radius * Math.sin(angle));
            double length = Math.sqrt(axisX * axisX + axisZ * axisZ);
            double nx = axisX / length, nz = axisZ / length;
            double tilt = Math.toRadians(this.tilt), cos = Math.cos(tilt), sin = Math.sin(tilt);
            double dot = nx * x + nz * z;
            return new double[] { (float) (x * cos + nx * dot * (1 - cos)), (float) ((nz * x - nx * z) * sin),
                (float) (z * cos + nz * dot * (1 - cos)) };
        }
    }
}
