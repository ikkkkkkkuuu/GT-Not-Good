package com.xyp.gtnotgood.mixins.late.Thaumcraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.config.Config;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.research.PlayerKnowledge;

/** Provides free research points and optional out-of-order aspect scanning. */
@Mixin(value = PlayerKnowledge.class, remap = false)
public abstract class MixinPlayerKnowledge {

    @Inject(
        method = "addAspectPool(Ljava/lang/String;Lthaumcraft/api/aspects/Aspect;S)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 1)
    private void gtnotgood$freeResearchAspects(String username, Aspect aspect, short amount,
        CallbackInfoReturnable<Boolean> cir) {
        if (Config.tcFreeResearchAspects && aspect != null && amount < 0) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
        method = "hasDiscoveredParentAspects(Ljava/lang/String;Lthaumcraft/api/aspects/Aspect;)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 1)
    private void gtnotgood$ignoreParentAspects(String player, Aspect aspect, CallbackInfoReturnable<Boolean> cir) {
        if (Config.tcScanIgnoreParentAspects) {
            cir.setReturnValue(true);
        }
    }
}
