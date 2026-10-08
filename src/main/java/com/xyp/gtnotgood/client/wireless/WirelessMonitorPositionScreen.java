package com.xyp.gtnotgood.client.wireless;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;

import com.xyp.gtnotgood.client.wireless.WirelessMonitorHud.Layout;
import com.xyp.gtnotgood.utils.keybind.KeyBindManager;

/**
 * Client-only drag editor. Moving updates the live HUD, and every exit path saves the position.
 * Opening it while the HUD is disabled shows a preview without enabling network polling.
 */
public final class WirelessMonitorPositionScreen extends GuiScreen {

    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiButton(0, width / 2 - 102, 48, 100, 20,
            // #tr gui.gtnotgood.wireless_position.reset
            // # Reset position
            // # zh_CN 恢复默认位置
            StatCollector.translateToLocal("gui.gtnotgood.wireless_position.reset")));
        buttonList.add(new GuiButton(1, width / 2 + 2, 48, 100, 20,
            // #tr gui.gtnotgood.wireless_position.done
            // # Save and close
            // # zh_CN 保存并退出
            StatCollector.translateToLocal("gui.gtnotgood.wireless_position.done")));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0x18000000);
        drawCenteredString(fontRendererObj,
            // #tr gui.gtnotgood.wireless_position.title
            // # Wireless HUD position
            // # zh_CN 调整无线电网 HUD 位置
            StatCollector.translateToLocal("gui.gtnotgood.wireless_position.title"), width / 2, 12, 0xFFFFFF);
        drawCenteredString(fontRendererObj,
            // #tr gui.gtnotgood.wireless_position.hint
            // # Drag the HUD with left mouse; press the edit key or Esc to save.
            // # zh_CN 左键拖动文字区域；再次按编辑键或 Esc 保存退出。
            StatCollector.translateToLocal("gui.gtnotgood.wireless_position.hint"), width / 2, 29, 0xDDDDDD);
        Layout layout = WirelessMonitorHud.INSTANCE.layout(width, height);
        int left = (int) (layout.x - 2 * layout.scale) - 2;
        int top = (int) (layout.y - 2 * layout.scale) - 2;
        int right = layout.x + layout.width + 2;
        int bottom = layout.y + layout.height + 2;
        int color = dragging || layout.contains(mouseX, mouseY) ? 0xFF55FFFF : 0xFFAAAAAA;
        drawRect(left, top, right, top + 1, color);
        drawRect(left, bottom - 1, right, bottom, color);
        drawRect(left, top, left + 1, bottom, color);
        drawRect(right - 1, top, right, bottom, color);
        WirelessMonitorHud.INSTANCE.draw(layout);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        if (mc.currentScreen != this || button != 0) return;
        Layout layout = WirelessMonitorHud.INSTANCE.layout(width, height);
        if (layout.contains(mouseX, mouseY)) {
            dragging = true;
            dragOffsetX = mouseX - layout.x;
            dragOffsetY = mouseY - layout.y;
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long elapsed) {
        if (!dragging || button != 0) return;
        WirelessMonitorPreferences.xOffset = mouseX - dragOffsetX - 4;
        WirelessMonitorPreferences.yOffset = height - 78 - (mouseY - dragOffsetY);
        // Store the clamped position too, so dragging past an edge does not leave a hidden offset behind.
        Layout layout = WirelessMonitorHud.INSTANCE.layout(width, height);
        WirelessMonitorPreferences.xOffset = layout.x - 4;
        WirelessMonitorPreferences.yOffset = height - 78 - layout.y;
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int button) {
        if (button == 0) dragging = false;
        super.mouseMovedOrUp(mouseX, mouseY, button);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            dragging = false;
            WirelessMonitorPreferences.xOffset = 0;
            WirelessMonitorPreferences.yOffset = 0;
        } else if (button.id == 1) mc.displayGuiScreen(null);
    }

    @Override
    protected void keyTyped(char character, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == KeyBindManager.moveWirelessMonitor.getKeyCode()) {
            mc.displayGuiScreen(null);
        } else super.keyTyped(character, keyCode);
    }

    @Override
    public void onGuiClosed() {
        dragging = false;
        WirelessMonitorPreferences.savePosition();
        KeyBindManager.consumeKey(KeyBindManager.moveWirelessMonitor);
        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
