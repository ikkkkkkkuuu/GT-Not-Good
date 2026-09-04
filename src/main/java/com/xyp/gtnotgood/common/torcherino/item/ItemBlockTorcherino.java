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
 * ItemBlock tooltip for area Torcherinos.
 */
public class ItemBlockTorcherino extends ItemBlock {

    /**
     * Creates the ItemBlock wrapper for an area Torcherino block.
     *
     * @param block wrapped block
     */
    public ItemBlockTorcherino(Block block) {
        super(block);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean extraInformation) {
        list.add("§7§m--------------------§r");
        // #tr gtnotgood.torcherino.tooltip.area.line1
        // # Right-click to configure speed and X/Y/Z range.
        // # zh_CN 右键打开界面,配置速度和 X/Y/Z 范围
        list.add(StatCollector.translateToLocal("gtnotgood.torcherino.tooltip.area.line1"));
        // #tr gtnotgood.torcherino.tooltip.area.line2
        // # Redstone signal disables acceleration.
        // # zh_CN 红石信号会关闭加速
        list.add(StatCollector.translateToLocal("gtnotgood.torcherino.tooltip.area.line2"));
        int rangeX = Config.torcherinoMaxXRadius * 2 + 1;
        int rangeY = Config.torcherinoMaxYRadius * 2 + 1;
        int rangeZ = Config.torcherinoMaxZRadius * 2 + 1;
        // #tr gtnotgood.torcherino.tooltip.area.range
        // # Max range: %dx%dx%d
        // # zh_CN 最大范围: %dx%dx%d
        list.add(
            StatCollector.translateToLocalFormatted("gtnotgood.torcherino.tooltip.area.range", rangeX, rangeY, rangeZ));
        list.add("§7§m--------------------§r");
    }
}
