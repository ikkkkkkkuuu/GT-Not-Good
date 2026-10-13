// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.mixins.late.appliedenergistics.wireless;

import java.util.Map;
import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.xyp.gtnotgood.common.wirelessfrequency.ChannelBudget;
import com.xyp.gtnotgood.common.wirelessfrequency.IWirelessPathingAccess;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessChannelManager;

import appeng.api.networking.IGrid;
import appeng.me.GridConnection;
import appeng.me.GridNode;
import appeng.me.pathfinding.PathingCalculation;

@Mixin(value = PathingCalculation.class, remap = false)
public abstract class MixinWirelessPathingCalculation implements IWirelessPathingAccess {

    @Unique
    private ChannelBudget gtng$wirelessBudget;

    @Unique
    private IGrid gtng$wirelessGrid;

    @WrapOperation(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)"
                + "Ljava/lang/Object;"),
        require = 2,
        allow = 2)
    private Object gtng$independentEntrance(Map<Object, Object> faces, Object direction,
        Function<Object, Object> factory, Operation<Object> original, @Local(name = "gc") GridConnection connection) {
        return WirelessChannelManager.isOwned(connection) ? factory.apply(direction)
            : original.call(faces, direction, factory);
    }

    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void gtng$initializeBudget(IGrid grid, CallbackInfo ci) {
        gtng$wirelessGrid = grid;
        WirelessChannelManager.invalidateAllocation(grid);
        if (WirelessChannelManager.channelsEnabled() && WirelessChannelManager.manages(grid)) {
            gtng$wirelessBudget = new ChannelBudget(WirelessChannelManager.capacity(grid));
        }
    }

    @Inject(method = "tryUseChannel", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtng$checkBudget(GridNode start, CallbackInfoReturnable<Boolean> cir) {
        if (gtng$wirelessBudget != null && !gtng$wirelessBudget.hasRemaining()) cir.setReturnValue(false);
    }

    @Inject(method = "tryUseChannel", at = @At("RETURN"), require = 1)
    private void gtng$countAcceptedChannel(GridNode start, CallbackInfoReturnable<Boolean> cir) {
        if (gtng$wirelessBudget != null && cir.getReturnValueZ()) gtng$wirelessBudget.recordAllocation();
    }

    @Inject(method = "compute", at = @At("RETURN"), require = 1)
    private void gtng$publishAllocation(CallbackInfo ci) {
        WirelessChannelManager.recordAllocation(gtng$wirelessGrid,
            gtng$wirelessBudget == null ? WirelessChannelManager.capacity(gtng$wirelessGrid)
                : gtng$wirelessBudget.capacity(),
            gtng$wirelessBudget == null ? ((PathingCalculation) (Object) this).getChannelsInUse()
                : gtng$wirelessBudget.allocated());
    }
}
