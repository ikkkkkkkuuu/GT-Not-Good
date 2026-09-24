// SPDX-License-Identifier: LGPL-3.0-only
// Camera input and smoothing adapted from Hcrab/RTSbuilding, JerryLunar and contributors.
package com.xyp.gtnotgood.client.rts;

import net.minecraft.world.World;

import com.xyp.gtnotgood.common.rts.session.RtsSessionLease;

/**
 * Owns the detached camera and upstream movement timing. All calls belong to the client thread.
 * Inputs come from the RTS screen after its widgets have consumed events, never from the player.
 * A camera position is visual state only and grants no permission to modify the world.
 */
public final class RtsCameraController {

    private final RtsCameraEntity entity;
    private final CameraMotionSolver.Bounds bounds;
    private CameraMotionSolver.Pose pose;
    private CameraMotionSolver.Pose visual;
    private boolean smooth;
    private boolean fast;
    private float forward, strafe, vertical;
    private float targetForward, targetStrafe, targetVertical;
    private float panX, panY, scroll, remainingScroll;
    private float accumulatedRotateX, accumulatedRotateY;
    private int quarterTurns;
    private long lastFrame;

    RtsCameraController(World world, RtsSessionLease lease, float yaw) {
        bounds = new CameraMotionSolver.Bounds(lease.anchorX, lease.anchorY, lease.anchorZ, lease.radius);
        pose = new CameraMotionSolver.Pose(
            lease.anchorX,
            lease.anchorY + 18,
            lease.anchorZ,
            18,
            Math.round(yaw / 90f) * 90f,
            70);
        visual = pose;
        entity = new RtsCameraEntity(world);
        entity.apply(pose);
    }

    RtsCameraEntity entity() {
        return entity;
    }

    /** Supplies axes after keyboard/search-field handling; zero all axes when the screen loses focus. */
    public void movement(float forward, float strafe, float vertical, boolean fast) {
        targetForward = bounded(forward, 5);
        targetStrafe = bounded(strafe, 5);
        targetVertical = bounded(vertical, 4);
        this.fast = fast;
    }

    /** Upstream drag signs and height scaling; callers pass raw mouse deltas and chosen sensitivity. */
    public void pan(float dx, float dy, float sensitivity, boolean invertX, boolean invertY) {
        float x = bounded((invertX ? dx : -dx) * sensitivity, 4096);
        float y = bounded((invertY ? dy : -dy) * sensitivity, 4096);
        if (smooth) move(0, 0, 0, x, y, 0, 0, 0, 0);
        else {
            panX = bounded(panX + x, 4096);
            panY = bounded(panY + y, 4096);
        }
    }

    /** Rotation is immediate even in smooth mode, so releasing the drag stops rotation at once. */
    public void rotate(float dx, float dy, float sensitivity) {
        float x = bounded(accumulatedRotateX + bounded(dx * sensitivity, 20), 160);
        float y = bounded(accumulatedRotateY + bounded(dy * sensitivity, 20), 160);
        move(0, 0, 0, 0, 0, x - accumulatedRotateX, y - accumulatedRotateY, 0, 0);
        accumulatedRotateX = x;
        accumulatedRotateY = y;
        visual = pose;
        entity.apply(visual);
    }

    public void scroll(float steps) {
        if (smooth) remainingScroll = bounded(remainingScroll + steps, 16);
        else scroll = bounded(scroll + steps, 16);
    }

    public void quarterTurn(int steps) {
        int boundedSteps = steps % 4;
        if (smooth) move(0, 0, 0, 0, 0, 0, 0, 0, boundedSteps);
        else quarterTurns = (quarterTurns + boundedSteps) % 4;
    }

    public void setSmooth(boolean enabled) {
        if (smooth == enabled) return;
        smooth = enabled;
        lastFrame = 0;
        forward = strafe = vertical = 0;
        if (enabled) {
            remainingScroll = bounded(remainingScroll + scroll, 16);
            scroll = 0;
            move(0, 0, 0, panX, panY, 0, 0, 0, quarterTurns);
            panX = panY = 0;
            quarterTurns = 0;
        } else {
            scroll = bounded(scroll + remainingScroll, 16);
            remainingScroll = 0;
        }
        visual = pose;
    }

    /** Clears queued input as well as inertia on focus loss; no stale movement resumes on return. */
    public void resetInput() {
        targetForward = targetStrafe = targetVertical = forward = strafe = vertical = 0;
        panX = panY = scroll = remainingScroll = accumulatedRotateX = accumulatedRotateY = 0;
        quarterTurns = 0;
        fast = false;
        lastFrame = 0;
        visual = pose;
    }

    void tick() {
        forward = axis(forward, targetForward);
        strafe = axis(strafe, targetStrafe);
        vertical = axis(vertical, targetVertical);
        if (!smooth) {
            move(forward, strafe, vertical, panX, panY, 0, 0, scroll, quarterTurns);
            visual = pose;
            entity.apply(visual);
        }
        panX = panY = scroll = accumulatedRotateX = accumulatedRotateY = 0;
        quarterTurns = 0;
    }

    /** Uses a capped monotonic frame clock, avoiding a large jump after a paused or stalled frame. */
    void frame(long now) {
        float seconds = lastFrame == 0 ? 0 : Math.max(0, Math.min(.1f, (now - lastFrame) / 1_000_000_000f));
        lastFrame = now;
        if (smooth) {
            RtsCameraSmoothingMath.DecayStep decay = RtsCameraSmoothingMath
                .consumeRemaining(remainingScroll, seconds, .045f, .0005f);
            remainingScroll = decay.remaining();
            float ticks = seconds / .05f;
            move(forward * ticks, strafe * ticks, vertical * ticks, 0, 0, 0, 0, decay.consumed(), 0);
            float positionAlpha = RtsCameraSmoothingMath.exponentialAlpha(seconds, .018f);
            float rotationAlpha = RtsCameraSmoothingMath.exponentialAlpha(seconds, .014f);
            double x = visual.x() + (pose.x() - visual.x()) * positionAlpha;
            double y = visual.y() + (pose.y() - visual.y()) * positionAlpha;
            double z = visual.z() + (pose.z() - visual.z()) * positionAlpha;
            visual = new CameraMotionSolver.Pose(
                x,
                y,
                z,
                y - bounds.anchorY(),
                RtsCameraSmoothingMath.interpolateAngleDegrees(visual.yawDeg(), pose.yawDeg(), rotationAlpha),
                RtsCameraSmoothingMath.interpolateAngleDegrees(visual.pitchDeg(), pose.pitchDeg(), rotationAlpha));
        } else visual = pose;
        entity.apply(visual);
    }

    private float axis(float current, float target) {
        return smooth ? RtsCameraSmoothingMath.approachAxis(current, target, .05f, .055f, .050f, .002f) : target;
    }

    private void move(float f, float s, float v, float px, float py, float rx, float ry, float wheel, int turns) {
        pose = CameraMotionSolver
            .solve(pose, bounds, new CameraMotionSolver.Input(f, s, v, px, py, rx, ry, wheel, turns, fast));
    }

    private static float bounded(float value, float limit) {
        return Float.isFinite(value) ? Math.max(-limit, Math.min(limit, value)) : 0;
    }
}
