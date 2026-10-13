// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.mixins.late.appliedenergistics.wireless;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.xyp.gtnotgood.common.wirelessfrequency.IWirelessPathingState;

import appeng.me.cache.PathGridCache;

@Mixin(value = PathGridCache.class, remap = false)
public abstract class MixinWirelessPathGridCache implements IWirelessPathingState {

    @Shadow
    private boolean updateNetwork;

    @Shadow
    private boolean recalculateControllerNextTick;

    @Override
    public boolean gtng$isRepathPending() {
        return updateNetwork || recalculateControllerNextTick;
    }
}
