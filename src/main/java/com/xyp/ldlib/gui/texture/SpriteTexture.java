package com.xyp.ldlib.gui.texture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

/**
 * LDLib2 SpriteTexture port: atlas regions, asymmetric nine-slice borders and tint.
 * Source dimensions are explicit to avoid resource IO during drawing. Border scaling preserves
 * source UVs when the destination is smaller than its corners. Wrap mode is stretch only.
 */
public final class SpriteTexture implements IGuiTexture {

    private final ResourceLocation resource;
    private final int imageWidth, imageHeight;
    private int spriteX, spriteY, spriteWidth, spriteHeight;
    private int left, top, right, bottom, color = -1;

    public SpriteTexture(ResourceLocation resource, int imageWidth, int imageHeight, int border) {
        if (imageWidth <= 0 || imageHeight <= 0) throw new IllegalArgumentException("Empty image");
        this.resource = resource;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        spriteWidth = imageWidth;
        spriteHeight = imageHeight;
        setBorder(border, border, border, border);
    }

    /** Selects a pixel rectangle from an atlas, as in the upstream setSprite API. */
    public SpriteTexture setSprite(int x, int y, int width, int height) {
        if (x < 0 || y < 0 || width <= 0 || height <= 0 || x + width > imageWidth || y + height > imageHeight) {
            throw new IllegalArgumentException("Sprite outside image");
        }
        spriteX = x;
        spriteY = y;
        spriteWidth = width;
        spriteHeight = height;
        return this;
    }

    public SpriteTexture setBorder(int left, int top, int right, int bottom) {
        if (Math.min(Math.min(left, right), Math.min(top, bottom)) < 0)
            throw new IllegalArgumentException("Negative border");
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        return this;
    }

    public SpriteTexture setColor(int color) {
        this.color = color;
        return this;
    }

    public SpriteTexture copy() {
        return new SpriteTexture(resource, imageWidth, imageHeight, 0)
            .setSprite(spriteX, spriteY, spriteWidth, spriteHeight)
            .setBorder(left, top, right, bottom)
            .setColor(color);
    }

    /** Returns source and destination splits for one nine-slice axis; pure math for regression tests. */
    public static float[][] splitAxis(int sourceStart, int sourceSize, int imageSize, int before, int after,
        int destinationStart, int destinationSize) {
        float sourceScale = before + (double) after > sourceSize ? sourceSize / (float) ((double) before + after) : 1;
        float a = before * sourceScale, b = after * sourceScale;
        float targetScale = a + b > destinationSize && a + b > 0 ? destinationSize / (a + b) : 1;
        return new float[][] {
            { destinationStart, destinationStart + a * targetScale,
                destinationStart + destinationSize - b * targetScale, destinationStart + destinationSize },
            { sourceStart / (float) imageSize, (sourceStart + a) / imageSize,
                (sourceStart + sourceSize - b) / imageSize, (sourceStart + sourceSize) / (float) imageSize } };
    }

    @Override
    public void draw(int mouseX, int mouseY, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        float[][] horizontal = splitAxis(spriteX, spriteWidth, imageWidth, left, right, x, width);
        float[][] vertical = splitAxis(spriteY, spriteHeight, imageHeight, top, bottom, y, height);
        float[] xs = horizontal[0], us = horizontal[1], ys = vertical[0], vs = vertical[1];
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_TEXTURE_BIT);
        try {
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(resource);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(
                (color >> 16 & 255) / 255f,
                (color >> 8 & 255) / 255f,
                (color & 255) / 255f,
                (color >>> 24) / 255f);
            Tessellator t = Tessellator.instance;
            t.startDrawingQuads();
            for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {
                if (xs[col] == xs[col + 1] || ys[row] == ys[row + 1]) continue;
                t.addVertexWithUV(xs[col], ys[row + 1], 0, us[col], vs[row + 1]);
                t.addVertexWithUV(xs[col + 1], ys[row + 1], 0, us[col + 1], vs[row + 1]);
                t.addVertexWithUV(xs[col + 1], ys[row], 0, us[col + 1], vs[row]);
                t.addVertexWithUV(xs[col], ys[row], 0, us[col], vs[row]);
            }
            t.draw();
        } finally {
            GL11.glPopAttrib();
        }
    }
}
