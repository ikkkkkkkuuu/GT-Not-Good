package com.xyp.gtnotgood.common.items.toolbelt.client.radial;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;

public interface IRadialMenuHost {

    void renderTooltip(ItemStack stack, int mouseX, int mouseY);

    GuiScreen getScreen();

    net.minecraft.client.gui.FontRenderer getFontRenderer();
}
