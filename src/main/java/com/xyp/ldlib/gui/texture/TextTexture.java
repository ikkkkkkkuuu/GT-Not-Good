package com.xyp.ldlib.gui.texture;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

/** Single-line supplied text, centered and trimmed to the destination rectangle. */
public final class TextTexture implements IGuiTexture {

    private final Supplier<String> text;
    private final int color;

    public TextTexture(Supplier<String> text, int color) {
        this.text = text;
        this.color = color;
    }

    @Override
    public void draw(int mouseX, int mouseY, int x, int y, int width, int height) {
        if (width <= 4 || height < 9) return;
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        String line = font.trimStringToWidth(text.get(), width - 4);
        font.drawString(line, x + (width - font.getStringWidth(line)) / 2, y + (height - 8) / 2, color);
    }
}
