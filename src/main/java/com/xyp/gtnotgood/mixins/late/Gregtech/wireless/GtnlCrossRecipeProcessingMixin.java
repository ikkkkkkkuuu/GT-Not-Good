package com.xyp.gtnotgood.mixins.late.Gregtech.wireless;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.common.wireless.WirelessRecipeAttempt;

import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.check.CheckRecipeResult;

@Pseudo
@Mixin(targets = "com.science.gtnl.utils.recipes.GTNLProcessingLogic", remap = false)
public abstract class GtnlCrossRecipeProcessingMixin {

    @Inject(
        method = "process()Lgregtech/api/recipe/check/CheckRecipeResult;",
        at = @At("HEAD"),
        cancellable = true,
        require = 0)
    private void gtng$process(CallbackInfoReturnable<CheckRecipeResult> cir) {
        CheckRecipeResult result = WirelessRecipeAttempt.intercept((ProcessingLogic) (Object) this);
        if (result != null) cir.setReturnValue(result);
    }
}
