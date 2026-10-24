package com.xyp.gtnotgood.mixins.early.interaction;

import net.minecraft.item.Item;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.xyp.gtnotgood.config.InteractionConfig;

@Mixin(Item.class)
public abstract class BlockReachItemMixin {

    @ModifyConstant(
        method = "getMovingObjectPositionFromPlayer(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;Z)Lnet/minecraft/util/MovingObjectPosition;",
        constant = @Constant(doubleValue = 5.0),
        require = 1,
        expect = 1,
        allow = 1)
    private double gtnotgood$itemBlockReach(double original) {
        return InteractionConfig.getBlockReachDistance();
    }
}
