package com.xyp.gtnotgood.mixins.late.AppliedEnergistics;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputSlave;
import com.xyp.gtnotgood.common.machines.hatch.me.PatternMEOutput;
import com.xyp.gtnotgood.common.machines.hatch.me.PatternMEOutputViews;

import gregtech.api.interfaces.IOutputBus;
import gregtech.api.interfaces.IOutputHatch;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatchOutput;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.common.tileentities.machines.IDualInputHatch;

/**
 * Exposes this mod's accepted pattern inputs as both item and fluid output destinations.
 * Concrete output views satisfy structure counters; the getters expose their canonical ME destinations for ejection
 * and void protection. Mirrors resolve their current master on every call,
 * and duplicate destinations are removed so a master and its mirrors never multiply simulated cache capacity.
 * Machines with dedicated output-layer overrides retain their own routing rules.
 */
@Mixin(value = MTEMultiBlockBase.class, remap = false)
public abstract class PatternMEOutputMultiblockMixin {

    @Shadow
    public ArrayList<IDualInputHatch> mDualInputHatches;

    @Shadow
    public ArrayList<MTEHatchOutputBus> mOutputBusses;

    @Shadow
    public ArrayList<MTEHatchOutput> mOutputHatches;

    @Shadow
    public abstract boolean addInputBusToMachineList(IGregTechTileEntity tile, int casing);

    /** Adds both concrete output roles before structure count predicates inspect the lists. */
    @Inject(
        method = { "addToMachineList", "addInputBusToMachineList", "addInputHatchToMachineList" },
        at = @At("RETURN"))
    private void gtnotgood$registerOutputRoles(IGregTechTileEntity tile, int casing,
        CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || tile == null) return;
        if (!(tile.getMetaTileEntity() instanceof SuperMTEHatchCraftingInputME)
            && !(tile.getMetaTileEntity() instanceof SuperMTEHatchCraftingInputSlave)) return;
        MetaTileEntity input = (MetaTileEntity) tile.getMetaTileEntity();
        if (mOutputBusses.stream()
            .noneMatch(bus -> bus instanceof PatternMEOutputViews.ItemView view && view.input == input)) {
            mOutputBusses.add(new PatternMEOutputViews.ItemView(input));
        }
        if (mOutputHatches.stream()
            .noneMatch(hatch -> hatch instanceof PatternMEOutputViews.FluidView view && view.input == input)) {
            mOutputHatches.add(new PatternMEOutputViews.FluidView(input));
        }
    }

    /** Output-only adders must also register the real input role and respect the controller's input support. */
    @Inject(
        method = { "addOutputBusToMachineList", "addOutputHatchToMachineList" },
        at = @At("HEAD"),
        cancellable = true)
    private void gtnotgood$acceptOutputRole(IGregTechTileEntity tile, int casing, CallbackInfoReturnable<Boolean> cir) {
        if (tile == null) return;
        if (!(tile.getMetaTileEntity() instanceof SuperMTEHatchCraftingInputME)
            && !(tile.getMetaTileEntity() instanceof SuperMTEHatchCraftingInputSlave)) return;
        if (mDualInputHatches.contains(tile.getMetaTileEntity())) {
            cir.setReturnValue(true);
        } else {
            cir.setReturnValue(addInputBusToMachineList(tile, casing));
        }
    }

    @Unique
    private PatternMEOutput gtnotgood$getOutput(IDualInputHatch input) {
        SuperMTEHatchCraftingInputME master = input instanceof SuperMTEHatchCraftingInputME hatch ? hatch
            : input instanceof SuperMTEHatchCraftingInputSlave mirror && mirror.isValid() ? mirror.getMaster() : null;
        return master != null && master.isValid() ? master.getMEOutput() : null;
    }

    @Inject(method = "getOutputBusses", at = @At("RETURN"), cancellable = true)
    private void gtnotgood$addItemOutputs(CallbackInfoReturnable<List<IOutputBus>> cir) {
        List<IOutputBus> result = new ArrayList<>(cir.getReturnValue());
        result.removeIf(bus -> bus instanceof PatternMEOutputViews.ItemView);
        for (IDualInputHatch input : mDualInputHatches) {
            PatternMEOutput output = gtnotgood$getOutput(input);
            if (output != null && !result.contains(output.itemOutput)) result.add(output.itemOutput);
        }
        cir.setReturnValue(result);
    }

    @Inject(method = "getOutputHatches()Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private void gtnotgood$addFluidOutputs(CallbackInfoReturnable<List<IOutputHatch>> cir) {
        List<IOutputHatch> result = new ArrayList<>(cir.getReturnValue());
        result.removeIf(hatch -> hatch instanceof PatternMEOutputViews.FluidView);
        for (IDualInputHatch input : mDualInputHatches) {
            PatternMEOutput output = gtnotgood$getOutput(input);
            if (output != null && !result.contains(output.fluidOutput)) result.add(output.fluidOutput);
        }
        cir.setReturnValue(result);
    }
}
