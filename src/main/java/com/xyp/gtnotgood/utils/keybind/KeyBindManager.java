package com.xyp.gtnotgood.utils.keybind;

import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;

/**
 * Registers and exposes client key bindings for the personal tool belt.
 */
public final class KeyBindManager {

    public static KeyBinding openToolMenuKeybind;
    public static KeyBinding cycleToolMenuLeft;
    public static KeyBinding cycleToolMenuRight;

    private static final String CATEGORY = "key.categories.gtnotgood";

    private KeyBindManager() {}

    /**
     * Registers all tool belt key bindings with the Minecraft controls menu.
     */
    public static void registerAllKeyBinds() {
        // #tr key.toolbelt.open
        // # Open Tool Belt Menu
        // # zh_CN 打开工具腰带菜单
        openToolMenuKeybind = new KeyBinding("key.toolbelt.open", Keyboard.KEY_R, CATEGORY);

        // #tr key.toolbelt.cycle.left
        // # Cycle Left
        // # zh_CN 左切换
        cycleToolMenuLeft = new KeyBinding("key.toolbelt.cycle.left", Keyboard.KEY_NONE, CATEGORY);

        // #tr key.toolbelt.cycle.right
        // # Cycle Right
        // # zh_CN 右切换
        cycleToolMenuRight = new KeyBinding("key.toolbelt.cycle.right", Keyboard.KEY_NONE, CATEGORY);

        ClientRegistry.registerKeyBinding(openToolMenuKeybind);
        ClientRegistry.registerKeyBinding(cycleToolMenuLeft);
        ClientRegistry.registerKeyBinding(cycleToolMenuRight);
    }

    public static boolean isKeyDown(KeyBinding key) {
        return key != null && Keyboard.isKeyDown(key.getKeyCode());
    }

    public static void consumeKey(KeyBinding key) {
        while (key != null && key.isPressed()) {
            // Consume queued key events.
        }
    }
}
