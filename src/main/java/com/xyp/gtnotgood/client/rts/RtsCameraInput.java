package com.xyp.gtnotgood.client.rts;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import com.xyp.gtnotgood.config.Config;

/**
 * LWJGL 2 input adapter for an owning RTS screen. Widgets must dispatch consumed mouse events
 * with overWorld=false, and suppress keyboard movement while a text field or modal has focus.
 * This adapter never presses gameplay KeyBindings or changes the player's MovementInput.
 */
public final class RtsCameraInput {

    private boolean rotating;
    private boolean panning;
    private int lastX, lastY;

    /** Samples rebound movement keys directly, because GuiScreen does not update KeyBinding state. */
    public void tick(boolean inputBlocked) {
        RtsCameraController camera = RtsClientState.INSTANCE.camera();
        if (camera == null) return;
        if (inputBlocked || !Display.isActive()) {
            reset();
            camera.resetInput();
            return;
        }
        GameSettings keys = Minecraft.getMinecraft().gameSettings;
        float speed = (float) Config.rtsCameraSpeed;
        camera.movement(
            (value(keys.keyBindForward) - value(keys.keyBindBack)) * speed,
            (value(keys.keyBindRight) - value(keys.keyBindLeft)) * speed,
            (value(keys.keyBindJump) - value(keys.keyBindSneak)) * speed,
            down(keys.keyBindSprint));
    }

    /**
     * Dispatches the current LWJGL mouse event after widgets. Drags start only over world space;
     * leaving that space cancels the gesture so re-entry cannot jump across a panel.
     *
     * @param overWorld whether the event belongs to unobstructed world space
     * @return whether the camera consumed this event
     */
    public boolean mouseEvent(boolean overWorld) {
        RtsCameraController camera = RtsClientState.INSTANCE.camera();
        if (camera == null || !Display.isActive() || !overWorld) {
            reset();
            return false;
        }
        int x = Mouse.getEventX();
        int y = Mouse.getEventY();
        int button = Mouse.getEventButton();
        boolean consumed = rotating || panning;
        if (button == 1) {
            rotating = Mouse.getEventButtonState();
            consumed = true;
        }
        if (button == 2) {
            panning = Mouse.getEventButtonState();
            consumed = true;
        }
        if (button == -1) {
            if (rotating) camera.rotate(x - lastX, lastY - y, 5);
            else if (panning) camera.pan(x - lastX, lastY - y, 1, false, false);
        }
        lastX = x;
        lastY = y;
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            camera.scroll(wheel / 120f * (float) Config.rtsZoomSpeed);
            consumed = true;
        }
        return consumed;
    }

    public void reset() {
        rotating = false;
        panning = false;
    }

    private static float value(KeyBinding binding) {
        return down(binding) ? 1 : 0;
    }

    private static boolean down(KeyBinding binding) {
        int code = binding.getKeyCode();
        if (code < 0) {
            int button = code + 100;
            return button >= 0 && button < Mouse.getButtonCount() && Mouse.isButtonDown(button);
        }
        return code > 0 && code < Keyboard.KEYBOARD_SIZE && Keyboard.isKeyDown(code);
    }
}
