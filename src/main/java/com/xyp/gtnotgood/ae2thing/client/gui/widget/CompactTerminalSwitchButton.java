package com.xyp.gtnotgood.ae2thing.client.gui.widget;

import net.minecraft.client.Minecraft;

import com.xyp.gtnotgood.ae2thing.quickterminal.client.QuickTerminalButtonTextures;

import appeng.client.gui.ScreenColor;
import appeng.client.gui.widgets.GuiTabButton;

public final class CompactTerminalSwitchButton extends GuiTabButton {

    public CompactTerminalSwitchButton(String message) {
        super(0, 0, -1, message);
        width = 16;
        height = 16;
    }

    @Override
    public void drawButton(Minecraft minecraft, int mouseX, int mouseY) {
        if (!visible) return;
        field_146123_n = mouseX >= xPosition && mouseY >= yPosition
            && mouseX < xPosition + width
            && mouseY < yPosition + height;
        ScreenColor.setGuiColor();
        minecraft.renderEngine.bindTexture(QuickTerminalButtonTextures.states());
        drawTexturedModalRect(xPosition, yPosition, 240, 240, 16, 16);
        ScreenColor.resetGuiColor();

        boolean dark = QuickTerminalButtonTextures.isDark();
        int border = dark ? 0xFF9CA6BB : 0xFF42495B;
        int cell = dark ? 0xFFE7E9EF : 0xFFF6F7FB;
        drawRect(xPosition + 3, yPosition + 3, xPosition + 13, yPosition + 13, border);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int x = xPosition + 4 + column * 3;
                int y = yPosition + 4 + row * 3;
                drawRect(x, y, x + 2, y + 2, row == 1 && column == 1 ? 0xFF71C7F4 : cell);
            }
        }
        mouseDragged(minecraft, mouseX, mouseY);
    }

    @Override
    public int getWidth() {
        return 16;
    }

    @Override
    public int getHeight() {
        return 16;
    }
}
