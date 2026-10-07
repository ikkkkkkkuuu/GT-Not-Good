package com.xyp.gtnotgood.mixins.late.ae2fluidcraft;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.glodblock.github.inventory.AEFluidInventory;
import com.glodblock.github.util.DualityFluidInterface;
import com.xyp.gtnotgood.common.blocks.largeinterface.LargeInterfaceHost;
import com.xyp.gtnotgood.common.blocks.largeinterface.LargeInterfaceSupport;

import appeng.helpers.IInterfaceHost;
import appeng.me.helpers.AENetworkProxy;

/** Prevents imported fluid marks from changing the native direct ME output return path. */
@Mixin(value = DualityFluidInterface.class, remap = false)
public abstract class LargeInterfaceFluidDualityMixin {

    @Shadow
    @Final
    @Mutable
    private AEFluidInventory config;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void largeinterface$rejectFluidStockConfig(AENetworkProxy proxy, IInterfaceHost host, CallbackInfo ci) {
        if (host instanceof LargeInterfaceHost) {
            config = LargeInterfaceSupport.emptyFluidConfig(DualityFluidInterface.NUMBER_OF_TANKS);
        }
    }
}
