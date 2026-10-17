package com.xyp.gtnotgood.mixins.late.treatedwater;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraftforge.fluids.FluidStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.xyp.gtnotgood.config.MainConfig;

import gregtech.api.enums.Materials;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.common.tileentities.machines.multi.purification.MTEPurificationUnitBase;
import gregtech.common.tileentities.machines.multi.purification.MTEPurificationUnitOzonation;

@Mixin(value = MTEPurificationUnitOzonation.class, remap = false)
public abstract class Grade2WaterPurificationMixin extends MTEPurificationUnitBase<MTEPurificationUnitOzonation>
    implements ISurvivalConstructable {

    protected Grade2WaterPurificationMixin(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    @Inject(method = "checkProcessing", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void injectCheckProcessing(CallbackInfoReturnable<CheckRecipeResult> cir) {
        if (MainConfig.grade2WaterPurificationEnabled) {
            cir.setReturnValue(super.checkProcessing());
        }
    }

    @Override
    public CheckRecipeResult overrideRecipeCheck() {
        if (!MainConfig.grade2WaterPurificationEnabled) return super.overrideRecipeCheck();

        FluidStack ozone = Materials.Ozone.getGas(Integer.MAX_VALUE);
        List<FluidStack> recipeFluids = new ArrayList<>(storedFluids);
        // Supply ozone only to recipe lookup; stored fluids and shared recipes stay unchanged.
        recipeFluids.removeIf(fluid -> fluid.isFluidEqual(ozone));
        recipeFluids.add(ozone);
        CheckRecipeResult result = findRecipeForInputs(recipeFluids.toArray(new FluidStack[0]));
        if (result.wasSuccessful()) {
            currentRecipe = currentRecipe.copy();
            currentRecipe.mFluidInputs = Arrays.stream(currentRecipe.mFluidInputs)
                .filter(fluid -> !fluid.isFluidEqual(ozone)).toArray(FluidStack[]::new);
        }
        return result;
    }

    @Override
    public float calculateBoostedSuccessChance() {
        return MainConfig.grade2WaterPurificationEnabled ? 100.0f : super.calculateBoostedSuccessChance();
    }
}
