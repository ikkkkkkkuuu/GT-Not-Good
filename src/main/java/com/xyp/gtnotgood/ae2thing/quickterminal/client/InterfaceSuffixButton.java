package com.xyp.gtnotgood.ae2thing.quickterminal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.xyp.gtnotgood.ae2thing.nei.ButtonConstants;
import com.xyp.gtnotgood.ae2thing.nei.NEI_TH_Config;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.client.gui.ScreenColor;
import appeng.client.gui.widgets.GuiTabButton;

final class InterfaceSuffixButton extends GuiTabButton {

    private static final ResourceLocation ICONS = ModList.GTNotGood
        .getResourceLocation("textures/gui/quick_terminal/suffix_toggle.png");

    InterfaceSuffixButton() {
        super(0, 0, -1, "");
        width = 16;
        height = 16;
    }

    private boolean appendsSuffix() {
        return NEI_TH_Config.getConfigValue(ButtonConstants.DUAL_INTERFACE_TERMINAL_APPEND_CIRCUIT_DAMAGE);
    }

    @Override
    public String getMessage() {
        // #tr gtnotgood.terminal.quick.append_suffix
        // # Append NC item suffixes
        // # zh_CN 追加不消耗物品后缀
        String title = StatCollector.translateToLocal("gtnotgood.terminal.quick.append_suffix");
        // #tr gtnotgood.terminal.quick.append_suffix.on
        // # Enabled
        // # zh_CN 已开启
        String on = StatCollector.translateToLocal("gtnotgood.terminal.quick.append_suffix.on");
        // #tr gtnotgood.terminal.quick.append_suffix.off
        // # Disabled
        // # zh_CN 已关闭
        String off = StatCollector.translateToLocal("gtnotgood.terminal.quick.append_suffix.off");
        // #tr gtnotgood.terminal.quick.append_suffix.detail
        // # Append non-consumed item numbers when autofilling the interface name search
        // # zh_CN 自动填充接口名称搜索框时，追加不消耗物品的编号
        String detail = StatCollector.translateToLocal("gtnotgood.terminal.quick.append_suffix.detail");
        String status = appendsSuffix() ? EnumChatFormatting.GREEN + on : EnumChatFormatting.RED + off;
        return title + "\n" + status + "\n" + EnumChatFormatting.GRAY + detail;
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

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        minecraft.renderEngine.bindTexture(ICONS);
        func_146110_a(xPosition, yPosition, appendsSuffix() ? 16 : 0, QuickTerminalButtonTextures.isDark() ? 16 : 0, 16,
            16, 32, 32);
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
