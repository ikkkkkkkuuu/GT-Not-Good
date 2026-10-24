package com.xyp.gtnotgood.mixins.early.interaction;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntityEnderChest;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.xyp.gtnotgood.config.InteractionConfig;

@Mixin(TileEntityEnderChest.class)
public abstract class BlockReachEnderChestMixin {

    @ModifyConstant(
        method = "func_145971_a(Lnet/minecraft/entity/player/EntityPlayer;)Z",
        constant = @Constant(doubleValue = 64.0),
        require = 1,
        expect = 1,
        allow = 1)
    private double gtnotgood$containerDistance(double original, EntityPlayer player) {
        return Math.max(original, InteractionConfig.getContainerDistanceSquared(player));
    }
}
