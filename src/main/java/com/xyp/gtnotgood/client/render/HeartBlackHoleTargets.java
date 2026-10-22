// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.client.render;

import static com.gtnewhorizon.gtnhlib.client.opengl.GLCaps.FBO;

import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.CustomFramebuffer;

final class HeartBlackHoleTargets implements AutoCloseable {

    final int width;
    final int height;
    final int blurWidth;
    final int blurHeight;
    final CustomFramebuffer effect;
    final CustomFramebuffer mask;
    final CustomFramebuffer effectBlurA;
    final CustomFramebuffer effectBlurB;
    final CustomFramebuffer maskBlurA;
    final CustomFramebuffer maskBlurB;

    HeartBlackHoleTargets(int width, int height) {
        this.width = width;
        this.height = height;
        blurWidth = Math.max(width / 4, 1);
        blurHeight = Math.max(height / 4, 1);
        effect = target(width, height);
        mask = target(width, height);
        effectBlurA = target(blurWidth, blurHeight);
        effectBlurB = target(blurWidth, blurHeight);
        maskBlurA = target(blurWidth, blurHeight);
        maskBlurB = target(blurWidth, blurHeight);
    }

    private static CustomFramebuffer target(int width, int height) {
        CustomFramebuffer target = new CustomFramebuffer(width, height, CustomFramebuffer.TEXTURE_LINEAR);
        target.bindFramebuffer();
        target.checkFramebufferComplete();
        return target;
    }

    static DepthAttachments captureDepth(int framebuffer) {
        if (framebuffer == 0) throw new IllegalStateException("Heart rendering requires a world framebuffer");
        FBO.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        return new DepthAttachments(attachment(GL30.GL_DEPTH_ATTACHMENT), attachment(GL30.GL_STENCIL_ATTACHMENT));
    }

    private static DepthAttachment attachment(int attachment) {
        return new DepthAttachment(query(attachment, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE),
            query(attachment, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME));
    }

    private static int query(int attachment, int parameter) {
        if (GLContext.getCapabilities().OpenGL30 || GLContext.getCapabilities().GL_ARB_framebuffer_object)
            return GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, attachment, parameter);
        return EXTFramebufferObject.glGetFramebufferAttachmentParameteriEXT(GL30.GL_FRAMEBUFFER, attachment, parameter);
    }

    static void bindVolumeTarget(CustomFramebuffer target, DepthAttachments depth) {
        target.bindFramebuffer(true);
        attach(GL30.GL_DEPTH_ATTACHMENT, depth.depth);
        attach(GL30.GL_STENCIL_ATTACHMENT, depth.stencil);
        target.checkFramebufferComplete();
        GL11.glColorMask(true, true, true, true);
        GL11.glClearColor(0, 0, 0, 0);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    }

    static void releaseDepth(CustomFramebuffer target) {
        target.bindFramebuffer();
        FBO.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, 0, 0);
        FBO.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_STENCIL_ATTACHMENT, GL11.GL_TEXTURE_2D, 0, 0);
    }

    private static void attach(int attachment, DepthAttachment depth) {
        if (depth.type == GL11.GL_TEXTURE) {
            FBO.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, attachment, GL11.GL_TEXTURE_2D, depth.name, 0);
        } else if (depth.type == GL30.GL_RENDERBUFFER) {
            FBO.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, attachment, GL30.GL_RENDERBUFFER, depth.name);
        } else {
            FBO.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, attachment, GL11.GL_TEXTURE_2D, 0, 0);
        }
    }

    @Override
    public void close() {
        effect.checkDeleteFramebuffer();
        mask.checkDeleteFramebuffer();
        effectBlurA.checkDeleteFramebuffer();
        effectBlurB.checkDeleteFramebuffer();
        maskBlurA.checkDeleteFramebuffer();
        maskBlurB.checkDeleteFramebuffer();
    }

    static final class DepthAttachments {

        private final DepthAttachment depth;
        private final DepthAttachment stencil;

        private DepthAttachments(DepthAttachment depth, DepthAttachment stencil) {
            this.depth = depth;
            this.stencil = stencil;
            if (depth.name == 0) throw new IllegalStateException("World framebuffer has no depth attachment");
        }
    }

    private static final class DepthAttachment {

        private final int type;
        private final int name;

        private DepthAttachment(int type, int name) {
            this.type = type;
            this.name = name;
        }
    }
}
