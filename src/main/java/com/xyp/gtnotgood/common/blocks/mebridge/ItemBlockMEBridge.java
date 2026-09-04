package com.xyp.gtnotgood.common.blocks.mebridge;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 跨维度 ME 网桥方块的 ItemBlock。
 * <p>
 * 仿 {@code ItemBlockBase}：tooltip 由 ItemBlock 的 {@link #addInformation} 提供(原版 ItemBlock 不会调用
 * {@code Block.addInformation}),逐行翻译方块通过 {@link BlockMEBridgeBase#getTooltipKeys()} 提供的语言键。
 */
public class ItemBlockMEBridge extends ItemBlock {

    public ItemBlockMEBridge(Block block) {
        super(block);
    }

    @SideOnly(Side.CLIENT)
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        if (this.field_150939_a instanceof BlockMEBridgeBase bridge) {
            for (String key : bridge.getTooltipKeys()) {
                list.add(StatCollector.translateToLocal(key));
            }
        }
    }
}
