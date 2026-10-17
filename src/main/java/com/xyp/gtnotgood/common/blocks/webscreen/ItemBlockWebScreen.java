package com.xyp.gtnotgood.common.blocks.webscreen;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.xyp.gtnotgood.client.GTNGCreativeTabs;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class ItemBlockWebScreen extends ItemBlock {

    public ItemBlockWebScreen(Block block) {
        super(block);
        setCreativeTab(GTNGCreativeTabs.GTNGItemBlock);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        // #tr tooltip.web_screen.browse
        // # Right-click to browse the web
        // # zh_CN 右键浏览网页
        lines.add(StatCollector.translateToLocal("tooltip.web_screen.browse"));
        // #tr tooltip.web_screen.stop
        // # Shift-right-click with an empty hand to stop this screen's local browser
        // # zh_CN 空手 Shift + 右键停止此屏幕的本机浏览器
        lines.add(StatCollector.translateToLocal("tooltip.web_screen.stop"));
        // #tr tooltip.web_screen.browser
        // # Requires Microsoft Edge or Google Chrome on this computer
        // # zh_CN 需要本机安装 Microsoft Edge 或 Google Chrome
        lines.add(StatCollector.translateToLocal("tooltip.web_screen.browser"));
        // #tr tooltip.web_screen.local
        // # Browsing and login sessions are local to each player
        // # zh_CN 网页及登录状态由每位玩家的本机独立保存
        lines.add(StatCollector.translateToLocal("tooltip.web_screen.local"));
        if (GuiScreen.isShiftKeyDown()) {
            // #tr tooltip.web_screen.structure
            // # Place same-facing screens in one filled rectangle
            // # zh_CN 将朝向相同的显示屏拼成无空缺的平面矩形
            lines.add(StatCollector.translateToLocal("tooltip.web_screen.structure"));
            // #tr tooltip.web_screen.size
            // # Maximum size: 16 blocks wide, 9 blocks high
            // # zh_CN 最大尺寸：宽 16 格，高 9 格
            lines.add(StatCollector.translateToLocal("tooltip.web_screen.size"));
            // #tr tooltip.web_screen.irregular
            // # Incomplete or oversized screens display each block separately
            // # zh_CN 不完整或超出尺寸时，每个方块独立显示
            lines.add(StatCollector.translateToLocal("tooltip.web_screen.irregular"));
        } else {
            // #tr tooltip.web_screen.shift
            // # Hold Shift for screen assembly
            // # zh_CN 按住 Shift 查看拼接说明
            lines.add(StatCollector.translateToLocal("tooltip.web_screen.shift"));
        }
    }
}
