package com.xyp.gtnotgood.mixins.late.gregtech.wireless;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.common.machines.hatch.WirelessMultiAmpEnergyHatch;

import tectech.thing.metaTileEntity.hatch.MTEHatchWirelessMulti;

@Mixin(value = MTEHatchWirelessMulti.class, remap = false)
public abstract class NativeWirelessMultiAmpMixin {

    @Inject(method = "getHatchType", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void gtnotgood$useMultiAmpType(CallbackInfoReturnable<Integer> cir) {
        if (WirelessMultiAmpEnergyHatch.isNativeCatalogInput((MTEHatchWirelessMulti) (Object) this))
            cir.setReturnValue(1);
    }

    @Inject(method = "getLocalName", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void gtnotgood$useMultiAmpName(CallbackInfoReturnable<String> cir) {
        MTEHatchWirelessMulti hatch = (MTEHatchWirelessMulti) (Object) this;
        if (WirelessMultiAmpEnergyHatch.isNativeCatalogInput(hatch))
            cir.setReturnValue(WirelessMultiAmpEnergyHatch.formatName(hatch.mTier, hatch.maxAmperes));
    }
}
