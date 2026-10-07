package com.xyp.gtnotgood.mixins.late.gregtech.wireless;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.common.wireless.WirelessRecipeAttempt;

import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.check.CheckRecipeResult;

@Mixin(value = ProcessingLogic.class, remap = false)
public abstract class CrossRecipeProcessingMixin {

    @Inject(method = "process", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtng$process(CallbackInfoReturnable<CheckRecipeResult> cir) {
        CheckRecipeResult result = WirelessRecipeAttempt.intercept((ProcessingLogic) (Object) this);
        if (result != null) cir.setReturnValue(result);
    }
}
