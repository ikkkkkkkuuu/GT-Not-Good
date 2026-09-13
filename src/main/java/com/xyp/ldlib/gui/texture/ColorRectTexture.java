package com.xyp.ldlib.gui.texture;

import net.minecraft.client.gui.Gui;

/** Solid-color texture adapted from LDLib2, using vanilla 1.7.10 drawing. */
public final class ColorRectTexture implements IGuiTexture {

    private final int color;

    public ColorRectTexture(int color) {
        this.color = color;
    }

    public ColorRectTexture(java.awt.Color color) {
        this(color.getRGB());
    }

    @Override
    public void draw(int mouseX, int mouseY, int x, int y, int width, int height) {
        if (width > 0 && height > 0) Gui.drawRect(x, y, x + width, y + height, color);
    }
}
