package com.xyp.ldlib.gui.render;

import java.nio.IntBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * Scoped GUI clipping for the LWJGL2 compatibility API. Intersects an existing framebuffer
 * scissor and restores both its rectangle and enable state, including nested scroll views.
 */
public final class ScissorScope implements AutoCloseable {

    private final boolean wasEnabled;
    private final int[] previous = new int[4];

    public ScissorScope(int x, int y, int width, int height) {
        Minecraft mc = Minecraft.getMinecraft();
        int scale = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
        wasEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        IntBuffer buffer = BufferUtils.createIntBuffer(16);
        GL11.glGetInteger(GL11.GL_SCISSOR_BOX, buffer);
        for (int i = 0; i < 4; i++) previous[i] = buffer.get(i);
        int[] requested = intersect(
            x * scale,
            mc.displayHeight - (y + height) * scale,
            Math.max(0, width) * scale,
            Math.max(0, height) * scale,
            0,
            0,
            mc.displayWidth,
            mc.displayHeight);
        if (wasEnabled) requested = intersect(
            requested[0],
            requested[1],
            requested[2],
            requested[3],
            previous[0],
            previous[1],
            previous[2],
            previous[3]);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(requested[0], requested[1], requested[2], requested[3]);
    }

    public static int[] intersect(int x, int y, int w, int h, int bx, int by, int bw, int bh) {
        int left = Math.max(x, bx), bottom = Math.max(y, by);
        return new int[] { left, bottom, Math.max(0, Math.min(x + w, bx + bw) - left),
            Math.max(0, Math.min(y + h, by + bh) - bottom) };
    }

    @Override
    public void close() {
        GL11.glScissor(previous[0], previous[1], previous[2], previous[3]);
        if (!wasEnabled) GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
