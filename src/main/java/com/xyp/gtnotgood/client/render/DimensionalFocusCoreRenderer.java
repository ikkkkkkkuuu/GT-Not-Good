// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.client.render;

import static com.gtnewhorizon.gtnhlib.client.opengl.GLCaps.FBO;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.I3DGeometryRenderer;
import com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.PostProcessingManager;
import com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.shaders.BloomShader;
import com.xyp.gtnotgood.GTNotGood;

public final class DimensionalFocusCoreRenderer {

    private static final float[] VERTICES = DimensionalFocusCoreGeometry.vertices();
    private static final I3DGeometryRenderer BLOOM_RENDERER = DimensionalFocusCoreRenderer::renderBloom;
    private static BloomShader bloom;
    private static boolean bloomUnavailable;

    private DimensionalFocusCoreRenderer() {}

    public static void render(double centerX, double centerY, double centerZ, int coilRgb, long offsetTimer,
        float partialTicks) {
        int color = DimensionalFocusCoreGeometry.color(coilRgb, offsetTimer, partialTicks);
        if (canBloom()) {
            if (bloom == null) {
                GlState state = new GlState();
                try {
                    bloom = BloomShader.getInstance();
                } catch (RuntimeException | LinkageError failure) {
                    disableBloom(failure);
                } finally {
                    state.restore();
                }
            }
            if (bloom != null) {
                PostProcessingManager.getInstance().addDelayedRenderer(BLOOM_RENDERER, centerX, centerY, centerZ,
                    Integer.valueOf(color));
                return;
            }
        }
        drawGeometry(centerX, centerY, centerZ, color);
    }

    private static boolean canBloom() {
        return !bloomUnavailable && OpenGlHelper.isFramebufferEnabled()
            && OpenGlHelper.shadersSupported
            && GLContext.getCapabilities().OpenGL20;
    }

    private static void renderBloom(Object data) {
        int color = (Integer) data;
        boolean rendered = false;
        GlState state = new GlState();
        try {
            bloom.bindFramebuffer();
            drawGeometry(0, 0, 0, color);
            rendered = true;
        } catch (RuntimeException | LinkageError failure) {
            disableBloom(failure);
        } finally {
            state.restore();
        }
        if (!rendered) drawGeometry(0, 0, 0, color);
    }

    private static void disableBloom(Throwable failure) {
        if (!bloomUnavailable) GTNotGood.LOG.warn("Dimensional focus core bloom is unavailable", failure);
        bloomUnavailable = true;
        bloom = null;
    }

    private static void drawGeometry(double centerX, double centerY, double centerZ, int color) {
        GlState state = new GlState();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        try {
            if (state.shaders) GL20.glUseProgram(0);
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_FOG);
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(true);
            GL11.glTranslated(centerX, centerY, centerZ);
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawing(GL11.GL_TRIANGLE_STRIP);
            tessellator.setColorRGBA_I(color & 0xFFFFFF, color >>> 24);
            for (int offset = 0; offset < VERTICES.length; offset += 3)
                tessellator.addVertex(VERTICES[offset], VERTICES[offset + 1], VERTICES[offset + 2]);
            tessellator.draw();
        } finally {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            state.restore();
        }
    }

    private static final class GlState {

        private final boolean shaders = GLContext.getCapabilities().OpenGL20;
        private final boolean framebuffers = OpenGlHelper.isFramebufferEnabled();
        private final boolean separateFramebuffers = framebuffers && GLContext.getCapabilities().OpenGL30;
        private final int program = shaders ? GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) : 0;
        private final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        private final int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        private final int framebuffer = framebuffers ? GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING) : 0;
        private final int readFramebuffer = separateFramebuffers ? GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)
            : 0;

        private GlState() {
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
        }

        private void restore() {
            GL11.glPopClientAttrib();
            GL11.glPopAttrib();
            if (framebuffers) {
                FBO.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
                if (separateFramebuffers) FBO.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
            }
            OpenGlHelper.setActiveTexture(activeTexture);
            if (shaders) GL20.glUseProgram(program);
            GL11.glMatrixMode(matrixMode);
        }
    }
}
