package com.xyp.gtnotgood.client.webscreen;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import org.lwjgl.input.Keyboard;

import com.xyp.gtnotgood.common.blocks.webscreen.TileWebScreen;
import com.xyp.gtnotgood.common.blocks.webscreen.WebScreenResolution;

import cpw.mods.fml.client.config.GuiSlider;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class GuiWebScreenSettings extends GuiScreen {

    private final GuiWebScreen parent;
    private final TileWebScreen tile;
    private final WebScreenSettings draft;
    private GuiButton resolution;
    private GuiButton muted;
    private int panelX;
    private int panelY;
    private int panelWidth;

    public GuiWebScreenSettings(GuiWebScreen parent, TileWebScreen tile) {
        this.parent = parent;
        this.tile = tile;
        draft = ClientWebScreens.getSettings(tile).copy();
    }

    @Override
    public void initGui() {
        buttonList.clear();
        panelWidth = Math.min(368, width - 24);
        panelX = (width - panelWidth) / 2;
        panelY = Math.max(8, (height - 208) / 2);
        int x = panelX + 12;
        int controlWidth = panelWidth - 24;
        resolution = new GuiButton(0, x, panelY + 36, controlWidth, 20, "");
        buttonList.add(resolution);
        // #tr gui.web_screen.settings.frame_rate
        // # Refresh limit:
        // # zh_CN 刷新上限：
        String frameRate = I18n.format("gui.web_screen.settings.frame_rate") + " ";
        buttonList.add(new GuiSlider(1, x, panelY + 64, controlWidth, 20, frameRate, " FPS", 15, 100,
            draft.getFrameRate(), false, true, slider -> draft.setFrameRate(slider.getValueInt())));
        // #tr gui.web_screen.settings.quality
        // # Image quality:
        // # zh_CN 画质：
        String quality = I18n.format("gui.web_screen.settings.quality") + " ";
        buttonList.add(new GuiSlider(2, x, panelY + 92, controlWidth, 20, quality, "", 60, 100, draft.getJpegQuality(),
            false, true, slider -> draft.setJpegQuality(slider.getValueInt())));
        muted = new GuiButton(3, x, panelY + 120, controlWidth, 20, "");
        buttonList.add(muted);
        int actionWidth = (controlWidth - 8) / 2;
        // #tr gui.web_screen.settings.apply
        // # Apply
        // # zh_CN 应用
        buttonList
            .add(new GuiButton(4, x, panelY + 172, actionWidth, 20, I18n.format("gui.web_screen.settings.apply")));
        // #tr gui.web_screen.settings.cancel
        // # Cancel
        // # zh_CN 取消
        buttonList.add(new GuiButton(5, x + actionWidth + 8, panelY + 172, actionWidth, 20,
            I18n.format("gui.web_screen.settings.cancel")));
        updateLabels();
    }

    private void updateLabels() {
        WebScreenResolution selected = draft.getResolution();
        String name = switch (selected) {
            case FullHd -> "1080P";
            case QuadHd -> "2K";
            case UltraHd -> "4K";
        };
        // #tr gui.web_screen.settings.resolution
        // # Resolution: %s (%s x %s)
        // # zh_CN 分辨率：%s（%s × %s）
        resolution.displayString = I18n.format("gui.web_screen.settings.resolution", name, selected.width,
            selected.height);
        // #tr gui.web_screen.settings.muted
        // # Muted: %s
        // # zh_CN 静音：%s
        muted.displayString = I18n.format("gui.web_screen.settings.muted",
            I18n.format(draft.isMuted() ? "options.on" : "options.off"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTick) {
        drawRect(0, 0, width, height, 0xF013171C);
        drawRect(panelX, panelY, panelX + panelWidth, panelY + 208, 0xFF202830);
        // #tr gui.web_screen.settings.title
        // # Screen Settings
        // # zh_CN 显示屏设置
        drawCenteredString(fontRendererObj, I18n.format("gui.web_screen.settings.title"), width / 2, panelY + 14,
            0xE0E5EB);
        // #tr gui.web_screen.settings.scope
        // # Applies to this screen; reloads the page
        // # zh_CN 仅当前显示屏，应用后重载网页
        drawCenteredString(fontRendererObj, I18n.format("gui.web_screen.settings.scope"), width / 2, panelY + 152,
            0xBBC6D2);
        super.drawScreen(mouseX, mouseY, partialTick);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            WebScreenResolution[] resolutions = WebScreenResolution.values();
            draft.setResolution(resolutions[(draft.getResolution().ordinal() + 1) % resolutions.length]);
            updateLabels();
        } else if (button.id == 3) {
            draft.setMuted(!draft.isMuted());
            updateLabels();
        } else if (button.id == 4) {
            ClientWebScreens.applySettings(tile, draft);
            mc.displayGuiScreen(parent);
        } else if (button.id == 5) mc.displayGuiScreen(parent);
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (key == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
