package com.xyp.gtnotgood.client.radialmenu.client.action;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import com.xyp.gtnotgood.client.radialmenu.client.gui.GuiRadialWheel;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSpec;

public final class WithoutScreen {

    private WithoutScreen() {}

    private static boolean armed;

    private static GuiScreen hiddenScreen;
    private static boolean hiddenFocus;

    public static boolean wheelIsUp() {
        return Minecraft.getMinecraft().currentScreen instanceof GuiRadialWheel;
    }

    public static void runAction(ActionSpec spec) {
        Minecraft mc = Minecraft.getMinecraft();

        GuiScreen screen = mc.currentScreen;
        boolean hadFocus = mc.inGameHasFocus;

        mc.currentScreen = null;
        mc.inGameHasFocus = true;
        try {
            ActionExecutors.execute(spec);
        } finally {
            // Preserve screens opened by the action instead of restoring the wheel over them.
            if (mc.currentScreen == null) {
                mc.currentScreen = screen;
                mc.inGameHasFocus = hadFocus;
            }
        }
    }

    public static void armNextTick() {
        armed = true;
    }

    public static void beginTick() {
        if (!armed || hiddenScreen != null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiRadialWheel)) {
            armed = false;
            return;
        }
        hiddenScreen = mc.currentScreen;
        hiddenFocus = mc.inGameHasFocus;
        mc.currentScreen = null;
        mc.inGameHasFocus = true;
    }

    public static void endTick(boolean startPhase) {
        if (hiddenScreen == null) {
            return;
        }
        if (startPhase) {
            armed = false;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen == null) {
            mc.currentScreen = hiddenScreen;
            mc.inGameHasFocus = hiddenFocus;
        }
        hiddenScreen = null;
    }
}
