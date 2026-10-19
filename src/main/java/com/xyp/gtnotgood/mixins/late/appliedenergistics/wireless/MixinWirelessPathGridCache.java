// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.mixins.late.appliedenergistics.wireless;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnotgood.common.wirelessfrequency.IWirelessPathingState;

import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.me.cache.PathGridCache;

@Mixin(value = PathGridCache.class, remap = false)
public abstract class MixinWirelessPathGridCache implements IWirelessPathingState {

    @Shadow
    private boolean updateNetwork;

    @Shadow
    private boolean recalculateControllerNextTick;

    @Unique
    private long gtng$topologyRevision;

    @Override
    public boolean gtng$isRepathPending() {
        return updateNetwork || recalculateControllerNextTick;
    }

    @Override
    public long gtng$topologyRevision() {
        return gtng$topologyRevision;
    }

    @Override
    public void gtng$topologyChanged() {
        gtng$topologyRevision++;
    }

    @Inject(method = { "addNode", "removeNode" }, at = @At("HEAD"))
    private void gtng$membershipChanged(IGridNode node, IGridHost machine, CallbackInfo ci) {
        gtng$topologyRevision++;
    }
}
