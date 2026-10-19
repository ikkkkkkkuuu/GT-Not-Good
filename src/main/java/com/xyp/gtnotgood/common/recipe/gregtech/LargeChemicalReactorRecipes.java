package com.xyp.gtnotgood.common.recipe.gregtech;

import net.minecraftforge.fluids.FluidStack;

import gregtech.api.enums.Materials;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipeBuilder;
import gtPlusPlus.core.fluids.GTPPFluids;

public final class LargeChemicalReactorRecipes {

    private LargeChemicalReactorRecipes() {}

    public static void loadRecipes() {
        GTRecipeBuilder.builder().itemInputs(Materials.Platinum.getDust(0)).circuit(24)
            .fluidInputs(Materials.Hydrogen.getGas(2000), Materials.Oxygen.getGas(2000))
            .fluidOutputs(new FluidStack(GTPPFluids.HydrogenPeroxide, 1000)).duration(20 * GTRecipeBuilder.SECONDS)
            .eut(TierEU.RECIPE_EV).addTo(RecipeMaps.multiblockChemicalReactorRecipes);
    }
}
