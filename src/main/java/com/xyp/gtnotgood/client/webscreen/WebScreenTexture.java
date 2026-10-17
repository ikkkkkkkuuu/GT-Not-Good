package com.xyp.gtnotgood.client.webscreen;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class WebScreenTexture extends AbstractTexture {

    private final int width;
    private final int height;
    private int[] pixels = new int[0];

    public WebScreenTexture(int width, int height) {
        this.width = width;
        this.height = height;
        int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            TextureUtil.allocateTexture(getGlTextureId(), width, height);
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previous);
        }
    }

    public boolean uploadFrame(BufferedImage image) {
        if (image.getWidth() != width || image.getHeight() != height) return false;
        int[] next = image.getType() == BufferedImage.TYPE_INT_ARGB
            ? ((DataBufferInt) image.getRaster().getDataBuffer()).getData()
            : image.getRGB(0, 0, width, height, null, 0, width);
        int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            TextureUtil.uploadTexture(getGlTextureId(), next, width, height);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            pixels = next;
            return true;
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previous);
        }
    }

    public int[] getTextureData() {
        return pixels;
    }

    @Override
    public void loadTexture(IResourceManager resourceManager) {}

    @Override
    public void deleteGlTexture() {
        super.deleteGlTexture();
        pixels = new int[0];
    }
}
