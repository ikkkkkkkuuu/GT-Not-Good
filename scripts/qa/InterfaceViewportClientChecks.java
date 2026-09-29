package com.xyp.gtnotgood.ae2thing.quickterminal.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

import appeng.api.storage.data.IAEStackType;
import appeng.client.gui.implementations.GuiInterfaceTerminal;
import appeng.tile.inventory.AppEngInternalInventory;

/** Invokes the transformed renderer under a real GL context and counts which inventory slots it touches. */
final class InterfaceViewportClientChecks {

    static void run(Object terminal) throws Exception {
        Class<?> entryType = Class
            .forName("appeng.client.gui.implementations.GuiInterfaceTerminal$InterfaceTerminalEntry");
        Constructor<?> constructor = entryType.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Object entry = constructor
            .newInstance(terminal, 9876L, "Viewport QA", "", 32, 8, 256, true, false, new IAEStackType<?>[0], 0);
        CountingInventory inventory = new CountingInventory();
        Field inv = entryType.getDeclaredField("inv");
        inv.setAccessible(true);
        inv.set(entry, inventory);
        Field height = GuiInterfaceTerminal.class.getDeclaredField("viewHeight");
        height.setAccessible(true);
        int previousHeight = height.getInt(terminal);
        Method draw = GuiInterfaceTerminal.class
            .getDeclaredMethod("drawEntry", entryType, int.class, int.class, int.class, int.class);
        draw.setAccessible(true);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            height.setInt(terminal, 36);
            require((int) draw.invoke(terminal, entry, 0, 0, -10000, -10000) == 577, "full scroll height retained");
            checkReads(inventory, 0, 16);
            Arrays.fill(inventory.reads, 0);
            draw.invoke(terminal, entry, -540, 0, -10000, -10000);
            checkReads(inventory, 240, 256);
            Arrays.fill(inventory.reads, 0);
            draw.invoke(terminal, entry, -9, 0, -10000, -10000);
            checkReads(inventory, 0, 24);
        } finally {
            height.setInt(terminal, previousHeight);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
        System.out.println("TERMINAL_VIEWPORT_QA: 256 slots -> 16 visible reads; bottom and partial rows PASS");
    }

    private static void checkReads(CountingInventory inventory, int first, int end) {
        for (int slot = 0; slot < inventory.reads.length; slot++) {
            require(inventory.reads[slot] == (slot >= first && slot < end ? 1 : 0), "slot visibility " + slot);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class CountingInventory extends AppEngInternalInventory {

        private final int[] reads = new int[256];

        private CountingInventory() {
            super(null, 256, 1);
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            reads[slot]++;
            return null;
        }
    }
}
