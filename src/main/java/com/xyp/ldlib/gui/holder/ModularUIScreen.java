package com.xyp.ldlib.gui.holder;

import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.xyp.ldlib.gui.texture.PixelFontScope;
import com.xyp.ldlib.gui.ui.UIElement;
import com.xyp.ldlib.gui.ui.UIInput;

/**
 * Reusable 1.7.10 screen host for the LDLib2 subset. Owns keyboard-repeat lifetime, focus,
 * pointer capture and scaled input coordinates. It does not create an inventory container.
 */
public class ModularUIScreen extends GuiScreen {

    protected final UIElement root;
    protected final UIInput input;
    private boolean previousRepeat, opened;

    public ModularUIScreen(UIElement root) {
        this.root = root;
        input = new UIInput(root);
    }

    @Override
    public void initGui() {
        if (!opened) {
            previousRepeat = Keyboard.areRepeatEventsEnabled();
            opened = true;
            Keyboard.enableRepeatEvents(true);
        }
        try (PixelFontScope ignored = new PixelFontScope()) {
            root.setPosition((width - root.getWidth()) / 2, (height - root.getHeight()) / 2);
            root.layout();
            input.validate();
        }
    }

    @Override
    public void updateScreen() {
        try (PixelFontScope ignored = new PixelFontScope()) {
            root.layout();
            input.validate();
            root.tick();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        try (PixelFontScope ignored = new PixelFontScope()) {
            root.layout();
            input.validate();
            root.draw(mouseX, mouseY, 0, 0);
        }
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        try (PixelFontScope ignored = new PixelFontScope()) {
            root.layout();
            input.mouseDown(x, y, button);
        }
    }

    @Override
    protected void mouseMovedOrUp(int x, int y, int button) {
        try (PixelFontScope ignored = new PixelFontScope()) {
            if (button >= 0) input.mouseUp(x, y, button);
            else input.mouseMove(x, y);
        }
    }

    @Override
    protected void mouseClickMove(int x, int y, int button, long elapsed) {
        try (PixelFontScope ignored = new PixelFontScope()) {
            input.mouseMove(x, y);
        }
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int delta = Mouse.getEventDWheel();
        if (delta != 0) {
            try (PixelFontScope ignored = new PixelFontScope()) {
                input.mouseWheel(
                    Mouse.getEventX() * width / mc.displayWidth,
                    height - Mouse.getEventY() * height / mc.displayHeight - 1,
                    delta);
            }
        }
    }

    @Override
    protected void keyTyped(char character, int keyCode) {
        boolean handled;
        try (PixelFontScope ignored = new PixelFontScope()) {
            handled = input
                .keyTyped(character, keyCode, isShiftKeyDown(), isCtrlKeyDown(), keyCode == Keyboard.KEY_TAB);
        }
        if (!handled) {
            super.keyTyped(character, keyCode);
        }
    }

    @Override
    public void onGuiClosed() {
        input.clear();
        if (opened) {
            Keyboard.enableRepeatEvents(previousRepeat);
            opened = false;
        }
        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
