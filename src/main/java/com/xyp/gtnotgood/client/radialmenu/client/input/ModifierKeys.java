package com.xyp.gtnotgood.client.radialmenu.client.input;

import org.lwjgl.input.Keyboard;

import com.xyp.gtnotgood.client.radialmenu.core.action.Modifiers;

public final class ModifierKeys {

    private ModifierKeys() {}

    public static void hold(Modifiers modifiers) {
        if (modifiers == null || !modifiers.any()) {
            return;
        }
        if (modifiers.shift) {
            KeyBuffer.set(Keyboard.KEY_LSHIFT, true);
        }
        if (modifiers.ctrl) {
            KeyBuffer.set(Keyboard.KEY_LCONTROL, true);
        }
        if (modifiers.alt) {
            KeyBuffer.set(Keyboard.KEY_LMENU, true);
        }
    }

    public static void release(Modifiers modifiers) {
        if (modifiers == null || !modifiers.any()) {
            return;
        }
        if (modifiers.shift) {
            KeyBuffer.clearUnlessHeld(Keyboard.KEY_LSHIFT, Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
        }
        if (modifiers.ctrl) {
            KeyBuffer.clearUnlessHeld(Keyboard.KEY_LCONTROL, Keyboard.isKeyDown(Keyboard.KEY_RCONTROL));
        }
        if (modifiers.alt) {
            KeyBuffer.clearUnlessHeld(Keyboard.KEY_LMENU, Keyboard.isKeyDown(Keyboard.KEY_RMENU));
        }
    }
}
