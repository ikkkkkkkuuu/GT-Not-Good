package com.xyp.ldlib.gui.texture;

/** Draws LDLib2-style texture layers in insertion order. */
public final class GuiTextureGroup implements IGuiTexture {

    private final IGuiTexture[] textures;

    public GuiTextureGroup(IGuiTexture... textures) {
        this.textures = textures.clone();
    }

    @Override
    public void draw(int mouseX, int mouseY, int x, int y, int width, int height) {
        for (IGuiTexture texture : textures) texture.draw(mouseX, mouseY, x, y, width, height);
    }
}
