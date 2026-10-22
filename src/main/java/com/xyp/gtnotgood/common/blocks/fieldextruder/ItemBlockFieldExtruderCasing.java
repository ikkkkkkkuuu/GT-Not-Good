package com.xyp.gtnotgood.common.blocks.fieldextruder;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

public final class ItemBlockFieldExtruderCasing extends ItemBlock {

    public ItemBlockFieldExtruderCasing(Block block) {
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
        String key = ((BlockFieldExtruderCasing) field_150939_a).getDisplayVariant(stack.getItemDamage()).getNameKey();
        return key.substring(0, key.length() - ".name".length());
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        // #tr tooltip.field_extruder_casing.structure
        // # Structure block for the Field Extruder Factory.
        // # zh_CN 力场压模工厂的结构方块。
        tooltip.add(StatCollector.translateToLocal("tooltip.field_extruder_casing.structure"));
    }
}
