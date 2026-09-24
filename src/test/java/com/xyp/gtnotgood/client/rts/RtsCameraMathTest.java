package com.xyp.gtnotgood.client.rts;

import static org.junit.Assert.*;

import org.junit.Test;

/** Tests view direction, camera bounds and frame-independent scroll consumption without a game window. */
public class RtsCameraMathTest {

    @Test
    public void forwardAndDollyFollowViewDirection() {
        CameraMotionSolver.Bounds bounds = new CameraMotionSolver.Bounds(0, 64, 0, 128);
        CameraMotionSolver.Pose start = new CameraMotionSolver.Pose(0, 82, 0, 18, 90, 0);
        CameraMotionSolver.Pose moved = CameraMotionSolver
            .solve(start, bounds, new CameraMotionSolver.Input(1, 0, 0, 0, 0, 0, 0, 1, 0, false));
        assertEquals(-3.05, moved.x(), 1e-6);
        assertEquals(82, moved.y(), 1e-6);
        assertEquals(0, moved.z(), 1e-6);
        CameraMotionSolver.Pose down = CameraMotionSolver.solve(
            new CameraMotionSolver.Pose(0, 82, 0, 18, 0, 90),
            bounds,
            new CameraMotionSolver.Input(0, 0, 0, 0, 0, 0, 0, 1, 0, false));
        assertEquals(79.4, down.y(), 1e-6);
    }

    @Test
    public void largeInputStaysWithinBoundsAndQuarterTurnsSnap() {
        CameraMotionSolver.Pose pose = CameraMotionSolver.solve(
            new CameraMotionSolver.Pose(10, 82, -5, 18, 13, 70),
            new CameraMotionSolver.Bounds(10, 64, -5, 16),
            new CameraMotionSolver.Input(10000, 10000, 10000, 0, 0, 0, 1000, 10000, 1, true));
        assertTrue(pose.x() >= -6 && pose.x() <= 26);
        assertTrue(pose.z() >= -21 && pose.z() <= 11);
        assertEquals(29, pose.y(), 1e-6);
        assertEquals(90, pose.yawDeg(), 0);
        assertEquals(90, pose.pitchDeg(), 0);
    }

    @Test
    public void scrollIsConservedAcrossFrameRates() {
        for (int fps : new int[] { 30, 60, 144 }) {
            float remaining = 8;
            float consumed = 0;
            for (int frame = 0; frame < fps; frame++) {
                RtsCameraSmoothingMath.DecayStep step = RtsCameraSmoothingMath
                    .consumeRemaining(remaining, 1f / fps, .045f, .0005f);
                consumed += step.consumed();
                remaining = step.remaining();
            }
            assertEquals(8, consumed, 1e-5);
            assertEquals(0, remaining, 0);
        }
    }

    @Test
    public void smoothingTakesShortestAngleAndRejectsInvalidTime() {
        assertEquals(180, RtsCameraSmoothingMath.interpolateAngleDegrees(179, -179, .5f), 0);
        assertEquals(0, RtsCameraSmoothingMath.exponentialAlpha(Float.NaN, .05f), 0);
        assertEquals(0, RtsCameraSmoothingMath.exponentialAlpha(-1, .05f), 0);
        float axis = 1;
        for (int i = 0; i < 20; i++) {
            axis = RtsCameraSmoothingMath.approachAxis(axis, 0, .05f, .055f, .05f, .002f);
        }
        assertEquals(0, axis, 0);
    }
}
