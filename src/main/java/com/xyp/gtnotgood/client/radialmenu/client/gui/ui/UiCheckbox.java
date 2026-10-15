package com.xyp.gtnotgood.client.radialmenu.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

public class UiCheckbox extends GuiButton {

    private static final int BOX = 11;

    public boolean checked;

    private final String label;

    public UiCheckbox(int id, int x, int y, int width, String label, boolean checked) {
        super(id, x, y, width, Ui.ROW, label);
        this.label = label;
        this.checked = checked;
    }

    public void toggle() {
        checked = !checked;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) {
            return;
        }
        this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition
            && mouseX < this.xPosition + this.width
            && mouseY < this.yPosition + this.height;

        int boxTop = this.yPosition + (Ui.ROW - BOX) / 2;
        Ui.frame(this.xPosition, boxTop, this.xPosition + BOX, boxTop + BOX, 0xFF101010,
            this.field_146123_n ? 0xFFFFFFFF : Ui.PANEL_BORDER);

        if (checked) {
            drawRect(this.xPosition + 3, boxTop + 3, this.xPosition + BOX - 3, boxTop + BOX - 3, Ui.TEXT_ACTIVE);
        }

        mc.fontRenderer.drawString(label, this.xPosition + BOX + 6, this.yPosition + (Ui.ROW - 8) / 2,
            this.enabled ? Ui.TEXT : Ui.TEXT_MUTED);
    }
}
