package com.xyp.gtnotgood.mixins.late.appliedenergistics;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.common.blocks.largeinterface.LargeInterfaceHost;
import com.xyp.gtnotgood.common.blocks.largeinterface.LargeInterfacePatternInventory;
import com.xyp.gtnotgood.common.blocks.largeinterface.LargeInterfaceSupport;

import appeng.helpers.DualityInterface;
import appeng.helpers.IInterfaceHost;
import appeng.me.helpers.AENetworkProxy;
import appeng.tile.inventory.AppEngInternalAEInventory;
import appeng.tile.inventory.AppEngInternalInventory;

/** Changes private native inventories only for this mod's enlarged interface hosts. */
@Mixin(value = DualityInterface.class, remap = false)
public abstract class LargeInterfaceDualityMixin {

    @Shadow
    @Final
    private IInterfaceHost iHost;

    @Shadow
    @Final
    @Mutable
    private AppEngInternalInventory patterns;

    @Shadow
    @Final
    @Mutable
    private AppEngInternalAEInventory config;

    @Shadow
    private boolean hasConfig;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void largeinterface$configureInventories(AENetworkProxy proxy, IInterfaceHost host, CallbackInfo ci) {
        if (!(host instanceof LargeInterfaceHost)) return;
        patterns = new LargeInterfacePatternInventory((DualityInterface) (Object) this);
        config = LargeInterfaceSupport.emptyItemConfig(DualityInterface.NUMBER_OF_CONFIG_SLOTS);
    }

    @Inject(method = "setHasConfig", at = @At("HEAD"), cancellable = true, remap = false)
    private void largeinterface$rejectStockMode(boolean value, CallbackInfoReturnable<Boolean> cir) {
        if (iHost instanceof LargeInterfaceHost) {
            hasConfig = false;
            cir.setReturnValue(false);
        }
    }

    @ModifyConstant(method = "addToCraftingList", constant = @Constant(intValue = 36), remap = false)
    private int largeinterface$patternPriorityRange(int slots) {
        return iHost instanceof LargeInterfaceHost ? LargeInterfaceHost.PATTERN_COUNT : slots;
    }
}
