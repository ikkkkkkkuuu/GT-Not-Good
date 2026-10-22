package com.xyp.gtnotgood.common.blocks.kerrnewman;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

public final class ItemBlockKerrNewmanCasing extends ItemBlock {

    public ItemBlockKerrNewmanCasing(Block block) {
        super(block);
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    @Override
    public int getMetadata(int damage) {
        return ((BlockKerrNewmanCasing) field_150939_a).getDisplayVariant(damage).getMetadata();
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        String key = ((BlockKerrNewmanCasing) field_150939_a).getDisplayVariant(stack.getItemDamage()).getNameKey();
        return key.substring(0, key.length() - ".name".length());
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        // #tr tooltip.kerr_newman_casing.structure
        // # Multiblock structure component.
        // # zh_CN 多方块机器的结构部件。
        tooltip.add(StatCollector.translateToLocal("tooltip.kerr_newman_casing.structure"));
    }
}
