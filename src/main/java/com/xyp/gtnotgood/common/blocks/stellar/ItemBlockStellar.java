package com.xyp.gtnotgood.common.blocks.stellar;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

public final class ItemBlockStellar extends ItemBlock {

    public ItemBlockStellar(Block block) {
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
        String key = ((BlockStellar) field_150939_a).getDisplayVariant(stack.getItemDamage()).getNameKey();
        return key.substring(0, key.length() - ".name".length());
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        BlockStellar block = (BlockStellar) field_150939_a;
        if (block.getVariant(stack.getItemDamage()) == StellarBlockVariant.EddingtonWallCasing) {
            // #tr tooltip.stellar.eddington
            // # Vault structure casing.
            // # zh_CN 保险库结构外壳。
            tooltip.add(StatCollector.translateToLocal("tooltip.stellar.eddington"));
        } else if (block.isGlass()) {
            // #tr tooltip.stellar.glass
            // # Decorative glass.
            // # zh_CN 装饰玻璃。
            tooltip.add(StatCollector.translateToLocal("tooltip.stellar.glass"));
        } else {
            // #tr tooltip.stellar.block
            // # Decorative block.
            // # zh_CN 装饰方块。
            tooltip.add(StatCollector.translateToLocal("tooltip.stellar.block"));
        }
    }
}
