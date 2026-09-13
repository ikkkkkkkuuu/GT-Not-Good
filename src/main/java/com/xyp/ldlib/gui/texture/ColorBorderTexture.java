package com.xyp.ldlib.gui.texture;

import net.minecraft.client.gui.Gui;

/** Inset color border adapted from LDLib2; thickness clamps to small destination bounds. */
public final class ColorBorderTexture implements IGuiTexture {

    private final int border, color;

    public ColorBorderTexture(int border, int color) {
        if (border < 0) throw new IllegalArgumentException("Negative border");
        this.border = border;
        this.color = color;
    }

    @Override
    public void draw(int mouseX, int mouseY, int x, int y, int width, int height) {
        int b = Math.min(border, Math.min(width, height) / 2);
        if (b <= 0) return;
        Gui.drawRect(x, y, x + width, y + b, color);
        Gui.drawRect(x, y + height - b, x + width, y + height, color);
        Gui.drawRect(x, y + b, x + b, y + height - b, color);
        Gui.drawRect(x + width - b, y + b, x + width, y + height - b, color);
    }
}
