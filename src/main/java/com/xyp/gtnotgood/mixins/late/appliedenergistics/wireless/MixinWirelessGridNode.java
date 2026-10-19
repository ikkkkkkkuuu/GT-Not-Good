// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.mixins.late.appliedenergistics.wireless;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnotgood.common.wirelessfrequency.WirelessChannelManager;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessClusterManager;

import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.me.GridNode;

@Mixin(value = GridNode.class, remap = false)
public abstract class MixinWirelessGridNode {

    @Inject(
        method = { "addConnection", "removeConnection" },
        at = @At(value = "INVOKE", target = "Lappeng/api/networking/IGridConnection;hasDirection()Z"))
    private void gtng$connectionsChanged(IGridConnection connection, CallbackInfo ci) {
        WirelessChannelManager.connectionsChanged((IGridNode) this, connection);
    }

    @Inject(method = "updateState", at = @At("RETURN"))
    private void gtng$nodeUpdated(CallbackInfo ci) {
        WirelessClusterManager.nodeChanged((IGridNode) this);
    }

    @Inject(method = "destroy", at = @At("HEAD"))
    private void gtng$nodeDestroyed(CallbackInfo ci) {
        WirelessClusterManager.nodeChanged((IGridNode) this);
    }
}
