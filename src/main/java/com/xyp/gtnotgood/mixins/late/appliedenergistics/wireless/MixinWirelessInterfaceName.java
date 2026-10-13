// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.mixins.late.appliedenergistics.wireless;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessInterfaceNameOverride;

import appeng.helpers.DualityInterface;
import appeng.helpers.ICustomNameObject;

@Mixin(value = DualityInterface.class, remap = false)
public abstract class MixinWirelessInterfaceName implements WirelessInterfaceNameOverride {

    @Unique
    private boolean gtng$readingTargetName;

    @Override
    public <T> T wirelessFrequency$withoutCustomName(Supplier<T> reader) {
        boolean previous = gtng$readingTargetName;
        gtng$readingTargetName = true;
        try {
            return reader.get();
        } finally {
            gtng$readingTargetName = previous;
        }
    }

    @WrapOperation(
        method = { "getCrafterIcon", "getRawTermName", "getAdjacentNameSuffix" },
        at = @At(value = "INVOKE", target = "Lappeng/helpers/ICustomNameObject;hasCustomName()Z"))
    private boolean gtng$readTargetMetadata(ICustomNameObject host, Operation<Boolean> original) {
        return !gtng$readingTargetName && original.call(host);
    }
}
