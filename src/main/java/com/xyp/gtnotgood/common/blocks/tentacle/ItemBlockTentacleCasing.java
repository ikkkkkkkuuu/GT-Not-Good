package com.xyp.gtnotgood.common.blocks.tentacle;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

public final class ItemBlockTentacleCasing extends ItemBlock {

    public ItemBlockTentacleCasing(Block block) {
        super(block);
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    @Override
    public int getMetadata(int metadata) {
        return metadata;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return ((BlockTentacleCasing) field_150939_a).getDisplayVariant(stack.getItemDamage()).getNameKey();
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return StatCollector.translateToLocal(getUnlocalizedName(stack));
    }
}
