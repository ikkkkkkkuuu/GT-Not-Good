package com.xyp.gtnotgood.common.blocks.particlecollider;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

public final class ItemBlockParticleColliderCasing extends ItemBlock {

    public ItemBlockParticleColliderCasing(Block block) {
        super(block);
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    @Override
    public int getMetadata(int damage) {
        return ((BlockParticleColliderCasing) field_150939_a).getDisplayVariant(damage).getMetadata();
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        String key = ((BlockParticleColliderCasing) field_150939_a).getDisplayVariant(stack.getItemDamage())
            .getNameKey();
        return key.substring(0, key.length() - ".name".length());
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        // #tr tooltip.particle_collider_casing.structure
        // # Structure block for the Super Particle Collider.
        // # zh_CN 超级粒子对撞机的结构方块。
        tooltip.add(StatCollector.translateToLocal("tooltip.particle_collider_casing.structure"));
    }
}
