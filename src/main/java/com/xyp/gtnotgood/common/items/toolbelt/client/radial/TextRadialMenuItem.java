package com.xyp.gtnotgood.common.items.toolbelt.client.radial;

import net.minecraft.client.Minecraft;

public class TextRadialMenuItem extends RadialMenuItem {

    private final String text;

    public TextRadialMenuItem(GenericRadialMenu menu, String text) {
        super(menu);
        this.text = text;
    }

    @Override
    public void draw(DrawingContext context) {
        String displayText = text;
        Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(
            displayText,
            (int) context.x - Minecraft.getMinecraft().fontRenderer.getStringWidth(displayText) / 2,
            (int) context.y - 4,
            0xFFFFFF);
    }

    @Override
    public void drawTooltip(DrawingContext context) {
        // No tooltip for text items
    }

    @Override
    public boolean onClick() {
        // Handled by anonymous subclass
        return false;
    }
}
