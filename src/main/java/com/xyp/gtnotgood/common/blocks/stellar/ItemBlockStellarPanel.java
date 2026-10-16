package com.xyp.gtnotgood.common.blocks.stellar;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

public final class ItemBlockStellarPanel extends ItemBlock {

    public ItemBlockStellarPanel(Block block) {
        super(block);
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        String key = ((BlockStellarPanel) field_150939_a).getDisplayVariant(stack.getItemDamage()).getNameKey();
        return key.substring(0, key.length() - ".name".length());
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        // #tr tooltip.stellar.panel
        // # Decorative block. The front always faces north.
        // # zh_CN 装饰方块，正面固定朝北。
        tooltip.add(StatCollector.translateToLocal("tooltip.stellar.panel"));
    }
}
