package com.xyp.gtnotgood.client.radialmenu.client.input;

import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import com.xyp.gtnotgood.utils.keybind.KeyBindManager;

import cpw.mods.fml.client.registry.ClientRegistry;

public final class WheelKeyBindings {

    public static final String CATEGORY = "key.categories.gtnotgood";

    public static KeyBinding openWheel;

    public static KeyBinding nextProfile;

    public static KeyBinding previousProfile;

    public static KeyBinding openEditor;

    private WheelKeyBindings() {}

    public static void register() {
        openWheel = KeyBindManager.openToolMenuKeybind;
        nextProfile = KeyBindManager.cycleToolMenuRight;
        previousProfile = KeyBindManager.cycleToolMenuLeft;
        openEditor = new KeyBinding("key.radialmenu.openEditor", Keyboard.KEY_NONE, CATEGORY);

        ClientRegistry.registerKeyBinding(openEditor);
    }
}
