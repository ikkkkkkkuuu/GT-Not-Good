package com.xyp.gtnotgood.mixins.late.gregtech.wireless;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.common.wireless.WirelessRecipeAttempt;

import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

/** Supplies unlimited recipe voltage only inside the native wireless recipe-check transaction. */
@Mixin(value = { MTEMultiBlockBase.class, MTEExtendedPowerMultiBlockBase.class }, remap = false)
public abstract class CrossRecipeVoltageMixin {

    @Inject(
        method = { "getMaxInputVoltage", "getAverageInputVoltage", "getMaxInputEu" },
        at = @At("HEAD"),
        cancellable = true,
        require = 0)
    private void gtng$recipeVoltage(CallbackInfoReturnable<Long> cir) {
        WirelessRecipeAttempt attempt = WirelessRecipeAttempt.current();
        if (attempt != null && attempt.belongsTo((MTEMultiBlockBase) (Object) this)) cir.setReturnValue(Long.MAX_VALUE);
    }
}
