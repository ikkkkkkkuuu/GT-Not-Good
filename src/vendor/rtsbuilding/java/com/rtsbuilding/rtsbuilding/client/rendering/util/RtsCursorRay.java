package com.rtsbuilding.rtsbuilding.client.rendering.util;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import com.rtsbuilding.rtsbuilding.platform.math.Vec3d;
import org.lwjgl.input.Mouse;

/**
 * RTS 世界交互与悬停高亮共享的鼠标射线。
 *
 * <p>使用实际世界绘制矩阵把 framebuffer 鼠标坐标转换成世界射线；不负责
 * 方块裁剪、实体选择或放置冻结。尚未绘制第一帧时才回退到相机姿态估算，
 * 避免原版眼高偏移、动态视野和插值导致显示位置与点击位置分离。</p>
 */
public final class RtsCursorRay {
    private static final java.nio.FloatBuffer MODEL = org.lwjgl.BufferUtils.createFloatBuffer(16);
    private static final java.nio.FloatBuffer PROJECTION = org.lwjgl.BufferUtils.createFloatBuffer(16);
    private static final java.nio.IntBuffer VIEWPORT = org.lwjgl.BufferUtils.createIntBuffer(16);
    private static final java.nio.FloatBuffer POINT = org.lwjgl.BufferUtils.createFloatBuffer(4);
    private static Entity renderedCamera;
    private static Object renderedWorld;
    private static Vec3d renderOrigin;
    private static int frameWidth, frameHeight;

    /** Captures the actual world projection before overlays alter OpenGL state. */
    public static void captureWorldFrame(Minecraft mc) {
        org.lwjgl.opengl.GL11.glGetFloat(org.lwjgl.opengl.GL11.GL_MODELVIEW_MATRIX, MODEL);
        org.lwjgl.opengl.GL11.glGetFloat(org.lwjgl.opengl.GL11.GL_PROJECTION_MATRIX, PROJECTION);
        org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_VIEWPORT, VIEWPORT);
        net.minecraft.client.renderer.entity.RenderManager manager = net.minecraft.client.renderer.entity.RenderManager.instance;
        renderOrigin = new Vec3d(manager.viewerPosX, manager.viewerPosY, manager.viewerPosZ);
        renderedCamera = mc.renderViewEntity;
        renderedWorld = mc.theWorld;
        frameWidth = mc.displayWidth;
        frameHeight = mc.displayHeight;
    }

    /** Unprojects framebuffer coordinates using the same matrices that drew the visible handles. */
    public static Snapshot fromRenderedFrame(Minecraft mc, float x, float y) {
        if (renderOrigin == null || renderedCamera != mc.renderViewEntity || renderedWorld != mc.theWorld
                || frameWidth != mc.displayWidth || frameHeight != mc.displayHeight) return null;
        if (!org.lwjgl.util.glu.GLU.gluUnProject(x, y, 0.0F, MODEL, PROJECTION, VIEWPORT, POINT)) return null;
        Vec3d near = new Vec3d(POINT.get(0), POINT.get(1), POINT.get(2)).add(renderOrigin);
        if (!org.lwjgl.util.glu.GLU.gluUnProject(x, y, 1.0F, MODEL, PROJECTION, VIEWPORT, POINT)) return null;
        Vec3d far = new Vec3d(POINT.get(0), POINT.get(1), POINT.get(2)).add(renderOrigin);
        return new Snapshot(near, far.subtract(near).normalize());
    }
    private RtsCursorRay() {
    }

    public static Snapshot capture(Minecraft minecraft) {
        Entity camera = minecraft == null ? null : minecraft.renderViewEntity;
        if (minecraft == null || camera == null) {
            return new Snapshot(Vec3d.ZERO, new Vec3d(0.0D, 0.0D, -1.0D));
        }
        if (Mouse.isCreated()) {
            Snapshot rendered = fromRenderedFrame(minecraft, Mouse.getX(), Mouse.getY());
            if (rendered != null) return rendered;
        }

        double width = Math.max(1.0D, minecraft.displayWidth);
        double height = Math.max(1.0D, minecraft.displayHeight);
        double normalizedX = Mouse.isCreated() ? Mouse.getX() / width * 2.0D - 1.0D : 0.0D;
        double normalizedY = Mouse.isCreated() ? Mouse.getY() / height * 2.0D - 1.0D : 0.0D;

        double yaw = Math.toRadians(camera.rotationYaw);
        double pitch = Math.toRadians(camera.rotationPitch);
        Vec3d forward = new Vec3d(
                -Math.sin(yaw) * Math.cos(pitch),
                -Math.sin(pitch),
                Math.cos(yaw) * Math.cos(pitch)).normalize();
        Vec3d screenRight = new Vec3d(-Math.cos(yaw), 0.0D, -Math.sin(yaw)).normalize();
        Vec3d screenUp = screenRight.crossProduct(forward).normalize();
        double tanY = Math.tan(Math.toRadians(minecraft.gameSettings.fovSetting) * 0.5D);
        double tanX = tanY * width / height;
        Vec3d direction = forward
                .add(screenRight.scale(normalizedX * tanX))
                .add(screenUp.scale(normalizedY * tanY))
                .normalize();
        return new Snapshot(com.rtsbuilding.rtsbuilding.platform.player.PlayerCompat.positionEyes(camera, 1.0F), direction);
    }

    public static final class Snapshot {
        private final Vec3d origin;
        private final Vec3d direction;

        private Snapshot(Vec3d origin, Vec3d direction) {
            this.origin = origin;
            this.direction = direction;
        }

        public Vec3d origin() {
            return origin;
        }

        public Vec3d direction() {
            return direction;
        }
    }
}
