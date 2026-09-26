package com.xyp.ldlib.integration.modularui;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.xyp.ldlib.gui.texture.PixelFontScope;

import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import me.eigenraven.lwjgl3ify.api.InputEvents;

/**
 * MUI2 host using the library's bitmap Latin font with Unicode fallback.
 * Covers layout, popup creation, drawing and text input because each can measure glyph widths.
 * The shared font state is restored after every callback, including failures, so other screens keep their font.
 */
@SideOnly(Side.CLIENT)
public class PixelFontModularScreen extends ModularScreen {

    public PixelFontModularScreen(String owner, ModularPanel panel) {
        super(owner, panel);
    }

    @Override
    public void onResize(int width, int height) {
        try (PixelFontScope ignored = new PixelFontScope()) {
            super.onResize(width, height);
        }
    }

    @Override
    public void onUpdate() {
        try (PixelFontScope ignored = new PixelFontScope()) {
            super.onUpdate();
        }
    }

    @Override
    public void onFrameUpdate() {
        try (PixelFontScope ignored = new PixelFontScope()) {
            super.onFrameUpdate();
        }
    }

    @Override
    public void drawScreen() {
        try (PixelFontScope ignored = new PixelFontScope()) {
            super.drawScreen();
        }
    }

    @Override
    public void drawForeground() {
        try (PixelFontScope ignored = new PixelFontScope()) {
            super.drawForeground();
        }
    }

    @Override
    public boolean onMousePressed(int mouseButton) {
        try (PixelFontScope ignored = new PixelFontScope()) {
            return super.onMousePressed(mouseButton);
        }
    }

    @Override
    public boolean onMouseRelease(int mouseButton) {
        try (PixelFontScope ignored = new PixelFontScope()) {
            return super.onMouseRelease(mouseButton);
        }
    }

    @Override
    public boolean onMouseDrag(int mouseButton, long timeSinceClick) {
        try (PixelFontScope ignored = new PixelFontScope()) {
            return super.onMouseDrag(mouseButton, timeSinceClick);
        }
    }

    @Override
    public boolean onMouseScroll(UpOrDown scrollDirection, int amount) {
        try (PixelFontScope ignored = new PixelFontScope()) {
            return super.onMouseScroll(scrollDirection, amount);
        }
    }

    @Override
    public boolean onKeyPressed(char typedChar, int keyCode) {
        try (PixelFontScope ignored = new PixelFontScope()) {
            return super.onKeyPressed(typedChar, keyCode);
        }
    }

    @Override
    public boolean onKeyRelease(char typedChar, int keyCode) {
        try (PixelFontScope ignored = new PixelFontScope()) {
            return super.onKeyRelease(typedChar, keyCode);
        }
    }

    @Override
    @Optional.Method(modid = com.cleanroommc.modularui.ModularUI.ModIds.LWJGL3IFY)
    public void onKeyEvent(InputEvents.KeyEvent event) {
        try (PixelFontScope ignored = new PixelFontScope()) {
            super.onKeyEvent(event);
        }
    }

    @Override
    @Optional.Method(modid = com.cleanroommc.modularui.ModularUI.ModIds.LWJGL3IFY)
    public void onTextEvent(InputEvents.TextEvent event) {
        try (PixelFontScope ignored = new PixelFontScope()) {
            super.onTextEvent(event);
        }
    }

}
