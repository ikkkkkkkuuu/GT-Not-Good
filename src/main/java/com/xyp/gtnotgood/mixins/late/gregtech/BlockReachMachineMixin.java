package com.xyp.gtnotgood.mixins.late.gregtech;

import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.xyp.gtnotgood.config.InteractionConfig;

import gregtech.api.metatileentity.BaseMetaTileEntity;

@Mixin(value = BaseMetaTileEntity.class, remap = false)
public abstract class BlockReachMachineMixin {

    @ModifyConstant(
        method = { "isUseableByPlayer(Lnet/minecraft/entity/player/EntityPlayer;)Z",
            "func_70300_a(Lnet/minecraft/entity/player/EntityPlayer;)Z" },
        constant = @Constant(doubleValue = 64.0D),
        require = 1,
        expect = 1,
        allow = 1)
    private double gtng$containerDistance(double original, EntityPlayer player) {
        return InteractionConfig.getContainerDistanceSquared(player);
    }
}
