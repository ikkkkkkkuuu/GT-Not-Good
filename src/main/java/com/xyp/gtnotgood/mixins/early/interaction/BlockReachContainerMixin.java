package com.xyp.gtnotgood.mixins.early.interaction;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.inventory.ContainerWorkbench;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.xyp.gtnotgood.config.InteractionConfig;

@Mixin({ ContainerWorkbench.class, ContainerEnchantment.class, ContainerRepair.class })
public abstract class BlockReachContainerMixin {

    @ModifyConstant(
        method = "canInteractWith(Lnet/minecraft/entity/player/EntityPlayer;)Z",
        constant = @Constant(doubleValue = 64.0),
        require = 1,
        expect = 1,
        allow = 1)
    private double gtnotgood$containerDistance(double original, EntityPlayer player) {
        return Math.max(original, InteractionConfig.getContainerDistanceSquared(player));
    }
}
