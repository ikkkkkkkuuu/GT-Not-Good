package com.xyp.gtnotgood.mixins.late.EnderIO;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import crazypants.enderio.config.Config;
import crazypants.enderio.item.ItemSoulVessel;

/** Allows every non-player living entity to be captured by an Ender IO Soul Vial. */
@Mixin(value = ItemSoulVessel.class, remap = false)
public abstract class MixinItemSoulVessel {

    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void gtnc$allowBossCapture(CallbackInfo ci) {
        Config.soulVesselCapturesBosses = true;
    }

    @Inject(method = "isBlackListed", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtnc$ignoreSoulVialBlacklist(String entityId, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
