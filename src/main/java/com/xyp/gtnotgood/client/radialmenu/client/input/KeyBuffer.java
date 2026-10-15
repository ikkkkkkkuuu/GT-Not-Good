package com.xyp.gtnotgood.client.radialmenu.client.input;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import org.lwjgl.input.Keyboard;

import com.xyp.gtnotgood.client.radialmenu.RadialMenuMod;

public final class KeyBuffer {

    private static boolean resolved;

    private static Field store;

    private static Method scancodeOf;

    private KeyBuffer() {}

    public static boolean isAvailable() {
        resolve();
        return store != null;
    }

    public static boolean isKeyboardCode(int keyCode) {
        return keyCode > 0 && keyCode < Keyboard.KEYBOARD_SIZE;
    }

    /** Hardware polling overwrites this buffer, so held keys must be reapplied each tick. */
    public static void set(int keyCode, boolean down) {
        if (!isKeyboardCode(keyCode)) {
            return;
        }
        resolve();
        if (store == null) {
            return;
        }

        try {
            ByteBuffer keys = (ByteBuffer) store.get(null);
            if (keys == null) {
                return;
            }
            int index = index(keyCode);
            if (index <= 0 || index >= keys.limit()) {
                return;
            }
            keys.put(index, (byte) (down ? 1 : 0));
        } catch (Exception e) {
            store = null;
            RadialMenuMod.LOG.warn("The key state store cannot be written, so modifier keys will not be held: " + e);
        }
    }

    public static void clearUnlessHeld(int keyCode, boolean physicallyHeld) {
        if (!physicallyHeld) {
            set(keyCode, false);
        }
    }

    private static int index(int keyCode) throws Exception {
        if (scancodeOf == null) {
            return keyCode;
        }
        return ((Integer) scancodeOf.invoke(null, Integer.valueOf(keyCode))).intValue();
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;

        store = field("org.lwjgl.input.Keyboard", "keyDownBuffer");
        if (store != null) {
            RadialMenuMod.LOG.debug("Key states come from LWJGL 2's keyDownBuffer");
            return;
        }

        // lwjgl3ify indexes this buffer by SDL scancode, not LWJGL key code.
        store = field("org.lwjglx.input.Keyboard", "sdlKeyPressedArray");
        if (store == null) {
            RadialMenuMod.LOG.warn("No key state store found; modifier keys and sneak injection will not be held");
            return;
        }
        try {
            scancodeOf = Class.forName("org.lwjglx.input.KeyCodes").getDeclaredMethod("lwjglToSdlScancode", int.class);
            scancodeOf.setAccessible(true);
            RadialMenuMod.LOG.debug("Key states come from lwjgl3ify's sdlKeyPressedArray");
        } catch (Exception e) {
            store = null;
            RadialMenuMod.LOG.warn("lwjgl3ify's key store was found but its scancode mapping was not: " + e);
        }
    }

    private static Field field(String className, String fieldName) {
        try {
            Field field = Class.forName(className).getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            return null;
        }
    }
}
