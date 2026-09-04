package com.xyp.gtnotgood.common.torcherino.item;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.xyp.gtnotgood.config.Config;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * ItemBlock tooltip for wireless Torcherinos, describing the GT Data Stick binding flow.
 */
public class ItemBlockWirelessTorcherino extends ItemBlock {

    /**
     * Creates the ItemBlock wrapper for a wireless Torcherino block.
     *
     * @param block wrapped block
     */
    public ItemBlockWirelessTorcherino(Block block) {
        super(block);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean extraInformation) {
        list.add("§7§m--------------------§r");
        // #tr gtnotgood.torcherino.tooltip.wireless.line1
        // # Left-click a machine with a GT Data Stick to save it.
        // # zh_CN 手持 GT 数据棒左键机器:保存机器坐标
        list.add(StatCollector.translateToLocal("gtnotgood.torcherino.tooltip.wireless.line1"));
        // #tr gtnotgood.torcherino.tooltip.wireless.line2
        // # Right-click this torch with the Data Stick to bind it.
        // # zh_CN 手持该数据棒右键火把:绑定机器
        list.add(StatCollector.translateToLocal("gtnotgood.torcherino.tooltip.wireless.line2"));
        // #tr gtnotgood.torcherino.tooltip.wireless.line3
        // # Right-click this torch empty-handed or normally to open the GUI.
        // # zh_CN 普通右键火把:打开配置界面
        list.add(StatCollector.translateToLocal("gtnotgood.torcherino.tooltip.wireless.line3"));
        int rangeX = Config.wirelessTorcherinoRadius * 2 + 1;
        int rangeZ = Config.wirelessTorcherinoRadius * 2 + 1;
        // #tr gtnotgood.torcherino.tooltip.wireless.range
        // # Range: %dx%dx%d
        // # zh_CN 范围: %dx%dx%d
        list.add(
            StatCollector
                .translateToLocalFormatted("gtnotgood.torcherino.tooltip.wireless.range", rangeX, 255, rangeZ));
        list.add("§7§m--------------------§r");
    }
}
