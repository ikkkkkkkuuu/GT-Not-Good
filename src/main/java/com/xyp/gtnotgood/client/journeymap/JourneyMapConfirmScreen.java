package com.xyp.gtnotgood.client.journeymap;

import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;

import org.lwjgl.input.Keyboard;

final class JourneyMapConfirmScreen extends GuiYesNo {

    private final GuiYesNoCallback callback;
    private final String title;
    private final String detail;
    private boolean finished;

    JourneyMapConfirmScreen(GuiYesNoCallback callback, String title, String detail) {
        super(callback, title, detail, 0);
        this.callback = callback;
        this.title = title;
        this.detail = detail;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int y = Math.max(20, height / 6 - 10);
        for (String line : lines(title)) {
            drawCenteredString(fontRendererObj, line, width / 2, y, 0xFFFFFF);
            y += 12;
        }
        y += 12;
        for (String line : lines(detail)) {
            drawCenteredString(fontRendererObj, line, width / 2, y, 0xCCCCCC);
            y += 12;
        }
        for (Object entry : buttonList) ((GuiButton) entry).drawButton(mc, mouseX, mouseY);
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (key == Keyboard.KEY_ESCAPE) finish(false);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.enabled) finish(button.id == 0);
    }

    private void finish(boolean confirmed) {
        if (finished || mc.currentScreen != this) return;
        finished = true;
        callback.confirmClicked(confirmed, 0);
        if (mc.currentScreen == this) mc.displayGuiScreen(null);
    }

    @SuppressWarnings("unchecked")
    private List<String> lines(String text) {
        return fontRendererObj.listFormattedStringToWidth(text, Math.max(80, width - 40));
    }
}
