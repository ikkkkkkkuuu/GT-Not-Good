package com.xyp.gtnotgood.mixins.early.interaction;

import net.minecraft.client.multiplayer.PlayerControllerMP;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.xyp.gtnotgood.config.InteractionConfig;

@Mixin(PlayerControllerMP.class)
public abstract class BlockReachPlayerControllerMixin {

    @Shadow
    public abstract boolean isInCreativeMode();

    @ModifyReturnValue(method = "getBlockReachDistance()F", at = @At("RETURN"), require = 1, expect = 1, allow = 1)
    private float gtnotgood$blockReach(float original) {
        return original + (float) InteractionConfig.getBlockReachDistance() - (isInCreativeMode() ? 5.0F : 4.5F);
    }
}
