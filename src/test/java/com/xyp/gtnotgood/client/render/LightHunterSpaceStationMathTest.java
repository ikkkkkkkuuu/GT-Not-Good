package com.xyp.gtnotgood.client.render;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class LightHunterSpaceStationMathTest {

    @Test
    public void starAndBeamRemainBehindEveryHorizontalFacing() {
        assertArrayEquals(new double[] { 0.5, 0.5, 206.5 }, star(2), 0);
        assertArrayEquals(new double[] { 0.5, 0.5, -205.5 }, star(3), 0);
        assertArrayEquals(new double[] { 206.5, 0.5, 0.5 }, star(4), 0);
        assertArrayEquals(new double[] { -205.5, 0.5, 0.5 }, star(5), 0);
        for (int facing = 2; facing <= 5; facing++) {
            double[] star = star(facing);
            double[] beam = LightHunterSpaceStationMath.rotatedOffset(-174, 0, 0, facing);
            assertEquals(32, Math.hypot(star[0] - beam[0], star[2] - beam[2]), 0);
        }
    }

    @Test
    public void everyPlanetFollowsItsTiltedRingWithinTheRenderBounds() {
        LightHunterSpaceStationMath.Profile profile = LightHunterSpaceStationMath.profile(733);
        assertEquals(13, profile.orbits.length);
        for (int index = 0; index < profile.orbits.length; index++) {
            LightHunterSpaceStationMath.Orbit orbit = profile.orbits[index];
            assertTrue(orbit.radius >= 22 && orbit.radius <= 53);
            assertTrue(orbit.tilt >= 70 && orbit.tilt <= 90);
            assertTrue(profile.planetSizes[index] >= 1.3 && profile.planetSizes[index] <= 2.7);
            for (int tick = 0; tick < 10000; tick += 197) {
                double[] point = orbit.position(tick);
                double distance = Math.sqrt(point[0] * point[0] + point[1] * point[1] + point[2] * point[2]);
                assertEquals(orbit.radius, distance, 0.00001);
                assertTrue(distance + profile.planetSizes[index] * Math.sqrt(3) / 2
                    < LightHunterSpaceStationMath.CULLING_RADIUS);
            }
        }
    }

    @Test
    public void positionSeedAndAnimationRemainStableAcrossReloads() {
        long seed = LightHunterSpaceStationMath.positionSeed(-206, 120, 384);
        assertEquals(-56624847257480L, seed);
        LightHunterSpaceStationMath.Profile first = LightHunterSpaceStationMath.profile(seed);
        LightHunterSpaceStationMath.Profile second = LightHunterSpaceStationMath.profile(seed);
        for (int index = 0; index < first.orbits.length; index++) {
            assertArrayEquals(first.orbits[index].position(2430.5F), second.orbits[index].position(2430.5F), 0);
            LightHunterSpaceStationMath.Rotation rotation = first.planetRotations[index];
            assertEquals(1, Math.sqrt(rotation.x * rotation.x + rotation.y * rotation.y + rotation.z * rotation.z),
                0.000001);
            assertTrue(rotation.speed >= 0.1 && rotation.speed <= 0.5);
        }
        assertEquals(first.starRotation.angle(2430.5F), second.starRotation.angle(2430.5F), 0);
    }

    @Test
    public void originalStarAndSpaceMeshesKeepTheirFullGeometry() throws Exception {
        verifyMesh("star", 960);
        verifyMesh("space", 3840);
    }

    private static double[] star(int facing) {
        return LightHunterSpaceStationMath.rotatedOffset(-206, 0, 0, facing);
    }

    private static void verifyMesh(String name, int expectedFaces) throws Exception {
        var stream = LightHunterSpaceStationMathTest.class
            .getResourceAsStream("/assets/gtnotgood/models/lighthunter/" + name + ".obj");
        assertNotNull(stream);
        int faces = 0;
        double radius = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("f ")) faces++;
                if (line.startsWith("v ")) {
                    String[] coordinates = line.split("\\s+");
                    double x = Double.parseDouble(coordinates[1]), y = Double.parseDouble(coordinates[2]),
                        z = Double.parseDouble(coordinates[3]);
                    radius = Math.max(radius, Math.sqrt(x * x + y * y + z * z));
                }
            }
        }
        assertEquals(expectedFaces, faces);
        assertEquals(74, radius, 0.00001);
        assertEquals(14.8, radius * LightHunterSpaceStationMath.STAR_SCALE, 0.00001);
    }
}
