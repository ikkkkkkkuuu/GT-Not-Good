package com.xyp.ldlib.integration.modularui;

import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.ModularUI;
import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.xyp.ldlib.gui.texture.PixelFontScope;
import com.xyp.ldlib.gui.ui.UIElement;
import com.xyp.ldlib.gui.ui.UIInput;

import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import me.eigenraven.lwjgl3ify.api.InputEvents;

/**
 * Client host combining a native LDLib control region with MUI2's synchronized real inventory.
 * The LDLib root is panel-relative and covers only the upper control region. Input outside it remains
 * owned by MUI2. Host applications must route LDLib callbacks through registered server sync handlers.
 * Modal LDLib children consume input before the inventory, and capture survives dragging outside the region.
 */
@SideOnly(Side.CLIENT)
public class LDLibModularScreen extends PixelFontModularScreen {

    private static final int SHIFT_MODIFIERS = 0x0003;
    private static final int CONTROL_MODIFIERS = 0x00C0;
    protected final UIElement controls;
    protected final UIInput input;
    private final LegacyInputQueue legacyInput = new LegacyInputQueue();
    private boolean captured;

    public LDLibModularScreen(String owner, ModularPanel panel, UIElement controls) {
        super(owner, panel);
        this.controls = controls;
        input = new UIInput(controls);
    }

    private void layoutControls() {
        controls.setPosition(getMainPanel().getArea().x, getMainPanel().getArea().y);
        controls.layout();
        input.validate();
    }

    private boolean modal() {
        for (UIElement child : controls.getChildren()) if (child.isModal() && child.isInteractive()) return true;
        return false;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        try (PixelFontScope ignored = new PixelFontScope()) {
            layoutControls();
            controls.tick();
        }
    }

    @Override
    public void drawScreen() {
        super.drawScreen();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
        try (PixelFontScope ignored = new PixelFontScope()) {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            layoutControls();
            controls.draw(getContext().getMouseX(), getContext().getMouseY(), 0, 0);
        } finally {
            GL11.glPopAttrib();
        }
    }

    @Override
    public boolean onMousePressed(int button) {
        try (PixelFontScope ignored = new PixelFontScope()) {
            layoutControls();
            int x = getContext().getMouseX(), y = getContext().getMouseY();
            if (modal() || controls.hitTest(x, y) != null) {
                captured = input.mouseDown(x, y, button);
                return true;
            }
            input.focus(null);
        }
        return super.onMousePressed(button);
    }

    @Override
    public boolean onMouseRelease(int button) {
        if (captured) {
            try (PixelFontScope ignored = new PixelFontScope()) {
                input.mouseUp(getContext().getMouseX(), getContext().getMouseY(), button);
                captured = false;
                return true;
            }
        }
        return super.onMouseRelease(button);
    }

    @Override
    public boolean onMouseDrag(int button, long elapsed) {
        if (captured) {
            input.mouseMove(getContext().getMouseX(), getContext().getMouseY());
            return true;
        }
        return super.onMouseDrag(button, elapsed);
    }

    @Override
    public boolean onMouseScroll(UpOrDown direction, int amount) {
        if (controls.hitTest(getContext().getMouseX(), getContext().getMouseY()) != null || modal()) {
            input.mouseWheel(getContext().getMouseX(), getContext().getMouseY(), direction.modifier);
            return true;
        }
        return super.onMouseScroll(direction, amount);
    }

    @Override
    public boolean onKeyPressed(char character, int key) {
        // LWJGL3ify also queues legacy events after dispatching its raw events.
        if (legacyInput.consume(character, key)) return true;
        try (PixelFontScope ignored = new PixelFontScope()) {
            if (
                input.keyTyped(character, key, GuiScreen.isShiftKeyDown(), GuiScreen.isCtrlKeyDown(),
                    key == Keyboard.KEY_TAB)
            ) return true;
        }
        return super.onKeyPressed(character, key);
    }

    @Override
    @Optional.Method(modid = ModularUI.ModIds.LWJGL3IFY)
    public void onTextEvent(InputEvents.TextEvent event) {
        try (PixelFontScope ignored = new PixelFontScope()) {
            layoutControls();
            if (input.textInput(event.text) || input.getFocused() != null || modal()) {
                legacyInput.recordText(event.text);
                return;
            }
        }
        super.onTextEvent(event);
    }

    @Override
    @Optional.Method(modid = ModularUI.ModIds.LWJGL3IFY)
    public void onKeyEvent(InputEvents.KeyEvent event) {
        try (PixelFontScope ignored = new PixelFontScope()) {
            layoutControls();
            boolean ownsFocus = input.getFocused() != null || modal();
            if (event.action == InputEvents.KeyAction.RELEASED) {
                if (ownsFocus) return;
            } else if (ownsFocus) {
                boolean shift = (event.sdlKeyModifiers & SHIFT_MODIFIERS) != 0;
                boolean control = (event.sdlKeyModifiers & CONTROL_MODIFIERS) != 0;
                if (
                    input.keyTyped('\0', event.lwjgl2KeyCode, shift, control, event.lwjgl2KeyCode == Keyboard.KEY_TAB)
                ) {
                    legacyInput.recordKey(event.lwjgl2KeyCode);
                    return;
                }
            }
        }
        super.onKeyEvent(event);
    }

    @Override
    public void onClose() {
        input.clear();
        legacyInput.clear();
        super.onClose();
    }
}
