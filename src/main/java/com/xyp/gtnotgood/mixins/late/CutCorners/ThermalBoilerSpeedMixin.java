package com.xyp.gtnotgood.mixins.late.CutCorners;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.xyp.gtnotgood.config.Config;

import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.common.tileentities.machines.multi.MTEThermalBoiler;
import gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.production.MTEThermalBoilerLegacy;

/**
 * Boiler recipes already receive global duration scaling, but batch processing can extend a one-tick recipe to
 * 128 ticks. Enforce fixed duration after base processing, before the boiler derives warmup and steam rate from it.
 * Multiplier mode is already applied to recipes and must not be applied a second time here.
 */
@Mixin(value = { MTEThermalBoiler.class, MTEThermalBoilerLegacy.class }, remap = false)
public abstract class ThermalBoilerSpeedMixin {

    @ModifyExpressionValue(
        method = "checkProcessing",
        at = @At(value = "INVOKE", target = "checkProcessing()Lgregtech/api/recipe/check/CheckRecipeResult;"),
        require = 1,
        allow = 1)
    private CheckRecipeResult gtnotgood$limitBoilerBatchDuration(CheckRecipeResult result) {
        if (!result.wasSuccessful()) return result;
        Config.ensureLoaded();
        MTEMultiBlockBase boiler = (MTEMultiBlockBase) (Object) this;
        boiler.mMaxProgresstime = limitBatchDuration(boiler.mMaxProgresstime);
        return result;
    }

    /**
     * Caps successful batch cycles in fixed mode while preserving recipe scaling in multiplier mode.
     *
     * @param duration duration after ordinary recipe and batch calculations
     * @return duration limited to the configured fixed time, or unchanged outside fixed mode
     */
    private static int limitBatchDuration(int duration) {
        if (Config.recipeSpeedMode != 1 || duration <= 0) return duration;
        return Math.min(duration, Config.getModifiedRecipeDuration(duration));
    }
}
