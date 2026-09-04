package com.xyp.gtnotgood.ae2thing.nei;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.ae2thing.quickterminal.client.GuiQuickEncodingTerminal;

import codechicken.nei.api.INEIGuiAdapter;

public class AE2TH_NEIGuiHandler extends INEIGuiAdapter {

    @Override
    public boolean hideItemPanelSlot(GuiContainer gui, int x, int y, int w, int h) {
        if (gui instanceof GuiQuickEncodingTerminal terminal) {
            return terminal.hideItemPanelSlot(x, y, w, h);
        }
        return false;
    }

    @Override
    public boolean handleDragNDrop(GuiContainer gui, int mouseX, int mouseY, ItemStack draggedStack, int button) {

        return super.handleDragNDrop(gui, mouseX, mouseY, draggedStack, button);
    }
}
