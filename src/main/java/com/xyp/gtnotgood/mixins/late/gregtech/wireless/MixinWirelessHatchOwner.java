// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.mixins.late.gregtech.wireless;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnotgood.common.wirelessfrequency.WirelessControllerContext;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessControllerVersion;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessHatchOwner;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

@Mixin(value = MTEHatch.class, remap = false)
public abstract class MixinWirelessHatchOwner implements WirelessHatchOwner {

    @Unique
    private Map<MTEMultiBlockBase, Long> gtng$owners;

    @Override
    public void wirelessFrequency$setController(MTEMultiBlockBase controller) {
        if (controller == null || !(controller instanceof WirelessControllerVersion versioned)) return;
        if (gtng$owners == null) gtng$owners = new WeakHashMap<>();
        gtng$owners.put(controller, versioned.wirelessFrequency$version());
    }

    @Override
    public Set<MTEMultiBlockBase> wirelessFrequency$getControllers() {
        if (gtng$owners == null || gtng$owners.isEmpty()) return Collections.emptySet();
        MTEHatch hatch = (MTEHatch) (Object) this;
        IGregTechTileEntity hatchBase = hatch.getBaseMetaTileEntity();
        if (hatchBase == null || !hatch.isValid() || hatchBase.getMetaTileEntity() != hatch) {
            gtng$owners.clear();
            return Collections.emptySet();
        }
        gtng$owners.entrySet().removeIf(entry -> {
            MTEMultiBlockBase controller = entry.getKey();
            if (
                controller == null || !controller.isValid()
                    || !(controller instanceof WirelessControllerVersion versioned)
                    || entry.getValue() != versioned.wirelessFrequency$version()
            ) return true;
            IGregTechTileEntity base = controller.getBaseMetaTileEntity();
            return base == null || base.getMetaTileEntity() != controller || base.getWorld() != hatchBase.getWorld();
        });
        return Collections.unmodifiableSet(new HashSet<>(gtng$owners.keySet()));
    }

    @Inject(method = "updateCraftingIcon(Lnet/minecraft/item/ItemStack;)V", at = @At("TAIL"))
    private void gtng$recordHatchOwner(ItemStack icon, CallbackInfo ci) {
        if (icon != null) wirelessFrequency$setController(WirelessControllerContext.current());
    }

    @Inject(method = "updateTexture", at = @At("HEAD"))
    private void gtng$recordHatchOwnerFromTexture(int id, CallbackInfo ci) {
        wirelessFrequency$setController(WirelessControllerContext.current());
    }
}
