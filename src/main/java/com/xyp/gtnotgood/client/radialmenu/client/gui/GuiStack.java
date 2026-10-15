package com.xyp.gtnotgood.client.radialmenu.client.gui;

import java.util.ArrayDeque;
import java.util.Deque;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public final class GuiStack {

    private static final Deque<GuiScreen> STACK = new ArrayDeque<>();

    private GuiStack() {}

    public static void push(GuiScreen screen) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) {
            STACK.push(mc.currentScreen);
        }
        mc.displayGuiScreen(screen);
    }

    public static void pop() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.displayGuiScreen(STACK.isEmpty() ? null : STACK.pop());
    }

    public static void closeAll() {
        STACK.clear();
        Minecraft.getMinecraft().displayGuiScreen(null);
    }

    public static boolean hasParent() {
        return !STACK.isEmpty();
    }

    private static GuiScreen requested;

    public static void requestOpen(GuiScreen screen) {
        requested = screen;
    }

    public static void openRequested() {
        if (requested == null) {
            return;
        }
        GuiScreen screen = requested;
        requested = null;
        STACK.clear();
        Minecraft.getMinecraft().displayGuiScreen(screen);
    }
}
