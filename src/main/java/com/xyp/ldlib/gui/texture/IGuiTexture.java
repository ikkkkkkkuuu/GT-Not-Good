package com.xyp.ldlib.gui.texture;

/**
 * Client-only texture contract adapted from LDLib for Minecraft 1.7.10.
 * See the bundled LDLib notice for upstream attribution and the scope of this port.
 */
@FunctionalInterface
public interface IGuiTexture {

    void draw(int mouseX, int mouseY, int x, int y, int width, int height);
}
