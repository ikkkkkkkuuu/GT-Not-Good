// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.mixins.late.gregtech.wireless;

import java.util.List;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessControllerContext;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessControllerVersion;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessHatchOwner;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.structure.error.StructureError;

@Mixin(value = MTEMultiBlockBase.class, remap = false)
public abstract class MixinWirelessControllerVersion implements WirelessControllerVersion {

    @Unique
    private long gtng$wirelessStructureVersion;

    @Unique
    private long gtng$recordedStructureVersion = Long.MIN_VALUE;

    @Override
    public long wirelessFrequency$version() {
        return gtng$wirelessStructureVersion;
    }

    @Inject(method = "clearHatches", at = @At("HEAD"))
    private void gtng$invalidateHatchOwners(CallbackInfo ci) {
        gtng$wirelessStructureVersion++;
    }

    @Inject(method = "checkStructure", at = @At("RETURN"))
    private void gtng$recordCheckedHatches(boolean forceReset, IGregTechTileEntity base,
        CallbackInfoReturnable<Boolean> cir) {
        if (!base.isServerSide() || gtng$recordedStructureVersion == gtng$wirelessStructureVersion) return;
        MTEMultiBlockBase controller = (MTEMultiBlockBase) (Object) this;
        // Registered hatches still belong to the controller while the rest of its structure is incomplete.
        gtng$recordHatches(controller.mInputHatches, controller);
        gtng$recordHatches(controller.mOutputHatches, controller);
        gtng$recordHatches(controller.mInputBusses, controller);
        gtng$recordHatches(controller.mOutputBusses, controller);
        gtng$recordHatches(controller.mDualInputHatches, controller);
        gtng$recordedStructureVersion = gtng$wirelessStructureVersion;
    }

    @Unique
    private static void gtng$recordHatches(Iterable<?> hatches, MTEMultiBlockBase controller) {
        for (Object hatch : hatches) {
            if (hatch instanceof WirelessHatchOwner owner) owner.wirelessFrequency$setController(controller);
        }
    }

    @WrapOperation(
        method = { "checkStructure", "checkMachine_TT" },
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/metatileentity/implementations/MTEMultiBlockBase;checkMachine("
                + "Lgregtech/api/interfaces/tileentity/IGregTechTileEntity;Lnet/minecraft/item/ItemStack;"
                + "Ljava/util/List;)V"))
    private void gtng$trackControllerDuringStructureCheck(MTEMultiBlockBase controller, IGregTechTileEntity base,
        ItemStack stack, List<StructureError> errors, Operation<Void> original) {
        MTEMultiBlockBase previous = WirelessControllerContext.enter(controller);
        try {
            original.call(controller, base, stack, errors);
        } finally {
            WirelessControllerContext.restore(previous);
        }
    }
}
