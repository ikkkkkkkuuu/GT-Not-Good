package com.xyp.gtnotgood.client.radialmenu.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

public class UiTabButton extends GuiButton {

    public static final int HEIGHT = 16;

    public boolean selected;

    public UiTabButton(int id, int x, int y, int width, String label, boolean selected) {
        super(id, x, y, width, HEIGHT, label);
        this.selected = selected;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) {
            return;
        }
        this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition
            && mouseX < this.xPosition + this.width
            && mouseY < this.yPosition + this.height;

        int left = this.xPosition;
        int right = this.xPosition + this.width;
        int top = this.yPosition;
        int bottom = this.yPosition + this.height;

        int fill = selected ? 0xFF2A2A2A : (this.field_146123_n ? 0xFF1C1C1C : 0xFF141414);
        Ui.frame(left, top, right, bottom, fill, selected ? 0xFF7A7A7A : Ui.PANEL_BORDER);

        if (selected) {
            // Erase the bottom edge so the tab joins the panel underneath.
            drawRect(left + 1, bottom - 1, right - 1, bottom, fill);
        }

        String text = Ui.fit(this.displayString, this.width - 8);
        mc.fontRenderer.drawString(text, left + (this.width - mc.fontRenderer.getStringWidth(text)) / 2,
            top + (HEIGHT - 8) / 2, selected ? Ui.TEXT : Ui.TEXT_MUTED);
    }
}
