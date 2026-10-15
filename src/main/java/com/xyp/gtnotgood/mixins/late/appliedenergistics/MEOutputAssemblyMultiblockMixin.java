package com.xyp.gtnotgood.mixins.late.appliedenergistics;

import java.util.ArrayList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.common.machines.hatch.me.MEOutputAssembly;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchOutput;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

@Mixin(value = MTEMultiBlockBase.class, remap = false)
public abstract class MEOutputAssemblyMultiblockMixin {

    @Shadow
    public ArrayList<MTEHatchOutputBus> mOutputBusses;

    @Shadow
    public ArrayList<MTEHatchOutput> mOutputHatches;

    @Inject(
        method = { "addToMachineList", "addOutputBusToMachineList", "addOutputHatchToMachineList" },
        at = @At("HEAD"),
        cancellable = true)
    private void gtnotgood$registerAssemblyOutputs(IGregTechTileEntity tile, int casing,
        CallbackInfoReturnable<Boolean> cir) {
        if (tile == null || !(tile.getMetaTileEntity() instanceof MEOutputAssembly assembly)) return;
        MTEMultiBlockBase controller = (MTEMultiBlockBase) (Object) this;
        MTEHatchOutput fluidOutput = assembly.getFluidOutput();
        assembly.updateTexture(casing);
        assembly.updateCraftingIcon(controller.getMachineCraftingIcon());
        fluidOutput.updateTexture(casing);
        fluidOutput.updateCraftingIcon(controller.getMachineCraftingIcon());
        if (!mOutputBusses.contains(assembly)) {
            controller.addIfSmartInput(assembly);
            mOutputBusses.add(assembly);
        }
        if (!mOutputHatches.contains(fluidOutput)) mOutputHatches.add(fluidOutput);
        cir.setReturnValue(true);
    }
}
