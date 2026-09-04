package com.xyp.gtnotgood.mixins.late.EOH;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.config.MainConfig;

import tectech.thing.metaTileEntity.multi.MTEEyeOfHarmony;

@Mixin(value = MTEEyeOfHarmony.class, remap = false)
public class EyeOfHarmonySuccessRateControl {

    @Inject(method = "recipeChanceCalculator", at = @At("HEAD"), cancellable = true)
    private void onRecipeChanceCalculator(CallbackInfoReturnable<Double> cir) {

        if (!MainConfig.EOHSuccessRateControls) {
            return;
        }
        double customChance = MainConfig.RecipeChance;
        cir.setReturnValue(customChance);
    }
}
