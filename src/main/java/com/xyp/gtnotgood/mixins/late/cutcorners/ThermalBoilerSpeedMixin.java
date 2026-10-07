package com.xyp.gtnotgood.mixins.late.cutcorners;

import net.minecraftforge.fluids.FluidStack;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.xyp.gtnotgood.config.Config;

import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.common.tileentities.machines.multi.MTEThermalBoiler;
import gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.production.MTEThermalBoilerLegacy;

/**
 * Boiler recipes already receive global duration scaling, but batch processing can extend a one-tick recipe to
 * 128 ticks. Enforce fixed duration after base processing, before the boiler derives warmup and steam rate from it.
 * Multiplier mode is already applied to recipes and must not be applied a second time here.
 * Warmup follows the duration policy independently, and steam efficiency scaling uses long arithmetic for batches.
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

    @ModifyReturnValue(method = "getEfficiencyIncrease", at = @At("RETURN"), require = 1, allow = 1)
    private int gtnotgood$modifyWarmupRate(int increase) {
        Config.ensureLoaded();
        MTEMultiBlockBase boiler = (MTEMultiBlockBase) (Object) this;
        return scaledWarmupRate(increase, boiler.getMaxEfficiency(null));
    }

    @Redirect(
        method = "checkProcessing",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraftforge/fluids/FluidStack;amount:I",
            opcode = Opcodes.PUTFIELD,
            ordinal = 0),
        require = 1,
        allow = 1)
    private void gtnotgood$scaleSteamWithoutOverflow(FluidStack steam, int amount) {
        MTEMultiBlockBase boiler = (MTEMultiBlockBase) (Object) this;
        steam.amount = scaleSteamOutput(steam.amount, boiler.mEfficiency, boiler.getMaxEfficiency(null));
    }

    /**
     * Applies the duration policy to the full warmup, converting it back to a per-tick efficiency gain.
     *
     * @param increase original efficiency gain per tick
     * @param maximum  maximum boiler efficiency
     * @return gain needed to reach full efficiency within the configured warmup time
     */
    private static int scaledWarmupRate(int increase, int maximum) {
        if (Config.recipeSpeedMode == 0 || increase <= 0 || maximum <= 0) return increase;
        int warmupTicks = (int) (((long) maximum + increase - 1) / increase);
        int duration = Config.getModifiedRecipeDuration(warmupTicks);
        return (int) (((long) maximum + duration - 1) / duration);
    }

    /**
     * Preserves large steam batches when multiplying their output by efficiency.
     *
     * @param amount     steam output before efficiency scaling
     * @param efficiency current boiler efficiency
     * @param maximum    maximum boiler efficiency
     * @return scaled steam output, with the upstream minimum of one liter
     */
    private static int scaleSteamOutput(int amount, int efficiency, int maximum) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, (long) amount * efficiency / maximum));
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
