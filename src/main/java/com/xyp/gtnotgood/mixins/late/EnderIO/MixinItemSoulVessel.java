package com.xyp.gtnotgood.mixins.late.EnderIO;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import crazypants.enderio.item.ItemSoulVessel;

/** Removes the Soul Vial entity blacklist; boss capture is enabled by {@link MixinSoulVesselConfig}. */
@Mixin(value = ItemSoulVessel.class, remap = false)
public abstract class MixinItemSoulVessel {

    @Inject(method = "isBlackListed", at = @At("HEAD"), cancellable = true)
    private void gtnc$removeBlacklist(String entityId, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
