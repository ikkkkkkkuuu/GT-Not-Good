package com.xyp.gtnotgood.mixins.early.interaction;

import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnotgood.config.InteractionConfig;

@Mixin(ItemInWorldManager.class)
public abstract class BlockReachWorldManagerMixin {

    @Shadow(remap = false)
    public abstract void setBlockReachDistance(double distance);

    @Inject(method = "<init>(Lnet/minecraft/world/World;)V", at = @At("RETURN"), require = 1, expect = 1, allow = 1)
    private void gtnotgood$initializeBlockReach(World world, CallbackInfo ci) {
        setBlockReachDistance(InteractionConfig.getBlockReachDistance());
    }
}
