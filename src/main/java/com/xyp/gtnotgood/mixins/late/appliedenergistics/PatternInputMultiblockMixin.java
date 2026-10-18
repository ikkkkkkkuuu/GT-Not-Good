package com.xyp.gtnotgood.mixins.late.appliedenergistics;

import java.util.ArrayList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputSlave;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.common.tileentities.machines.IDualInputHatch;

@Mixin(value = MTEMultiBlockBase.class, remap = false)
public abstract class PatternInputMultiblockMixin {

    @Shadow
    public ArrayList<IDualInputHatch> mDualInputHatches;

    @Shadow
    public abstract boolean addInputBusToMachineList(IGregTechTileEntity tile, int casing);

    @Inject(method = "addInputHatchToMachineList", at = @At("HEAD"), cancellable = true)
    private void gtnotgood$acceptMirror(IGregTechTileEntity tile, int casing, CallbackInfoReturnable<Boolean> cir) {
        if (tile == null || !(tile.getMetaTileEntity() instanceof SuperMTEHatchCraftingInputSlave mirror)) return;
        cir.setReturnValue(mDualInputHatches.contains(mirror) || addInputBusToMachineList(tile, casing));
    }
}
