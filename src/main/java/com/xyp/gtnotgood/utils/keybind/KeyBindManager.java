package com.xyp.gtnotgood.utils.keybind;

import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;

/**
 * Registers and exposes this mod's client key bindings.
 */
public final class KeyBindManager {

    public static KeyBinding openToolMenuKeybind;
    public static KeyBinding cycleToolMenuLeft;
    public static KeyBinding cycleToolMenuRight;
    public static KeyBinding toggleWirelessMonitor;
    public static KeyBinding configureWirelessMonitor;
    public static KeyBinding moveWirelessMonitor;

    private static final String CATEGORY = "key.categories.gtnotgood";

    private KeyBindManager() {}

    /**
     * Registers client key bindings with the Minecraft controls menu.
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

        // #tr key.gtnotgood.wireless_monitor.toggle
        // # Toggle Wireless Network HUD
        // # zh_CN 开关无线电网 HUD
        toggleWirelessMonitor = new KeyBinding("key.gtnotgood.wireless_monitor.toggle", Keyboard.KEY_P, CATEGORY);

        // #tr key.gtnotgood.wireless_monitor.configure
        // # Configure Wireless HUD Colors
        // # zh_CN 配置无线 HUD 滚动颜色
        configureWirelessMonitor = new KeyBinding("key.gtnotgood.wireless_monitor.configure", Keyboard.KEY_NONE,
            CATEGORY);

        // #tr key.gtnotgood.wireless_monitor.move
        // # Move Wireless Network HUD
        // # zh_CN 调整无线电网 HUD 位置
        moveWirelessMonitor = new KeyBinding("key.gtnotgood.wireless_monitor.move", Keyboard.KEY_HOME, CATEGORY);

        ClientRegistry.registerKeyBinding(openToolMenuKeybind);
        ClientRegistry.registerKeyBinding(cycleToolMenuLeft);
        ClientRegistry.registerKeyBinding(cycleToolMenuRight);
        ClientRegistry.registerKeyBinding(toggleWirelessMonitor);
        ClientRegistry.registerKeyBinding(configureWirelessMonitor);
        ClientRegistry.registerKeyBinding(moveWirelessMonitor);
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
