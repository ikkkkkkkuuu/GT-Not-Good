package com.xyp.gtnotgood.common.blocks.heart;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

public final class ItemBlockHeartCasing extends ItemBlock {

    public ItemBlockHeartCasing(Block block) {
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
        String key = ((BlockHeartCasing) field_150939_a).getDisplayVariant(stack.getItemDamage()).getNameKey();
        return key.substring(0, key.length() - ".name".length());
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        // #tr tooltip.heart_casing.structure
        // # Multiblock structure component.
        // # zh_CN 多方块机器的结构部件。
        tooltip.add(StatCollector.translateToLocal("tooltip.heart_casing.structure"));
    }
}
