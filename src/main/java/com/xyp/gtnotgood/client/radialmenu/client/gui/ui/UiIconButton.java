package com.xyp.gtnotgood.client.radialmenu.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;

public class UiIconButton extends GuiButton {

    public enum Icon {

        Cross,

        Up,

        Down,

        Copy,

        Right
    }

    /** Across the mark, in pixels. Even on purpose: an odd size cannot sit centred on an even button. */
    private static final int SIZE = 6;

    private final Icon icon;

    public UiIconButton(int id, int x, int y, int width, int height, Icon icon) {
        super(id, x, y, width, height, "");
        this.icon = icon;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        // The vanilla frame and its hover state, with no label: the mark goes on afterwards.
        super.drawButton(mc, mouseX, mouseY);
        if (!this.visible) {
            return;
        }

        int colour = !this.enabled ? 0xFF8A8A8A : this.field_146123_n ? 0xFFFFFFA0 : Ui.TEXT;
        int centerX = this.xPosition + this.width / 2;
        int centerY = this.yPosition + this.height / 2;

        switch (icon) {
            case Cross:
                drawCross(centerX, centerY, colour);
                break;
            case Up:
                drawTriangle(centerX, centerY, colour, true);
                break;
            case Down:
                drawTriangle(centerX, centerY, colour, false);
                break;
            case Copy:
                drawCopy(centerX, centerY, colour);
                break;
            case Right:
                drawRightTriangle(centerX, centerY, colour);
                break;
            default:
                break;
        }
    }

    private static void drawTriangle(int centerX, int centerY, int colour, boolean pointingUp) {
        int rows = SIZE / 2;
        for (int row = 0; row < rows; row++) {
            // Two pixels wider per row down, so the base is as wide as the mark and the tip is two pixels across.
            int halfWidth = row + 1;
            // Both directions occupy the same band, so an up and a down button side by side line up exactly.
            int y = pointingUp ? centerY - rows / 2 + row : centerY + rows / 2 - row;
            Gui.drawRect(centerX - halfWidth, y, centerX + halfWidth, y + 1, colour);
        }
    }

    private static void drawRightTriangle(int centerX, int centerY, int colour) {
        int columns = SIZE / 2;
        for (int column = 0; column < columns; column++) {
            int halfHeight = columns - column;
            int x = centerX - columns / 2 + column;
            Gui.drawRect(x, centerY - halfHeight, x + 1, centerY + halfHeight, colour);
        }
    }

    private static void drawCopy(int centerX, int centerY, int colour) {
        int side = SIZE - 1;
        outline(centerX - side / 2 - 1, centerY - side / 2 - 1, side, colour);
        outline(centerX - side / 2 + 2, centerY - side / 2 + 2, side, colour);
    }

    private static void outline(int x, int y, int side, int colour) {
        Gui.drawRect(x, y, x + side, y + 1, colour);
        Gui.drawRect(x, y + side - 1, x + side, y + side, colour);
        Gui.drawRect(x, y, x + 1, y + side, colour);
        Gui.drawRect(x + side - 1, y, x + side, y + side, colour);
    }

    private static void drawCross(int centerX, int centerY, int colour) {
        int half = SIZE / 2;
        for (int i = 0; i < SIZE; i++) {
            int x = centerX - half + i;
            int down = centerY - half + i;
            int up = centerY + half - 1 - i;
            Gui.drawRect(x, down, x + 1, down + 1, colour);
            Gui.drawRect(x, up, x + 1, up + 1, colour);
        }
    }
}
