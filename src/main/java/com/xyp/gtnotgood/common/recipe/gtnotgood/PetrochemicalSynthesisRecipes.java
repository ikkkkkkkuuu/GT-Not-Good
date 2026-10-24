// SPDX-License-Identifier: MIT
// Copyright (c) 2026 QionSi
// QionSi recipe port; see META-INF/production-simplification-port/NOTICE.md.
package com.xyp.gtnotgood.common.recipe.gtnotgood;

import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.loader.GTNGRecipeMaps;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.objects.SubstituteFluidStack;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

public final class PetrochemicalSynthesisRecipes {

    private static final int OIL_AMOUNT = 10000;
    private static final SubstituteFluidStack ANY_OIL = new SubstituteFluidStack(Materials.Oil.getFluid(OIL_AMOUNT),
        Materials.OilLight.getFluid(OIL_AMOUNT), Materials.OilHeavy.getFluid(OIL_AMOUNT),
        Materials.OilExtraHeavy.getFluid(OIL_AMOUNT));

    private PetrochemicalSynthesisRecipes() {}

    public static void loadRecipes() {
        GTRecipeBuilder.builder().circuit(1)
            .fluidInputs(ANY_OIL, Materials.Oxygen.getGas(10000), Materials.Steam.getGas(12000))
            .fluidOutputs(Materials.Polyethylene.getMolten(1656))
            .itemOutputs(dust(Materials.Sulfur, 1), dust(Materials.Carbon, 1)).outputChances(10000, 3333)
            .duration(30 * SECONDS).eut(TierEU.RECIPE_LV).addTo(GTNGRecipeMaps.PetrochemicalSynthesisRecipes);

        GTRecipeBuilder.builder().circuit(2)
            .fluidInputs(ANY_OIL, Materials.Chlorine.getGas(1104), Materials.Oxygen.getGas(10000),
                Materials.Steam.getGas(12000))
            .fluidOutputs(Materials.PolyvinylChloride.getMolten(1656))
            .itemOutputs(dust(Materials.Sulfur, 1), dust(Materials.Carbon, 1)).outputChances(10000, 3333)
            .duration(30 * SECONDS).eut(TierEU.RECIPE_MV).addTo(GTNGRecipeMaps.PetrochemicalSynthesisRecipes);

        GTRecipeBuilder.builder().circuit(3)
            .fluidInputs(ANY_OIL, Materials.Hydrogen.getGas(2208), Materials.Fluorine.getGas(2208),
                Materials.Oxygen.getGas(10000), Materials.Steam.getGas(12000))
            .fluidOutputs(Materials.Polytetrafluoroethylene.getMolten(828))
            .itemOutputs(dust(Materials.Sulfur, 1), dust(Materials.Carbon, 1)).outputChances(10000, 3333)
            .duration(30 * SECONDS).eut(TierEU.RECIPE_MV).addTo(GTNGRecipeMaps.PetrochemicalSynthesisRecipes);

        GTRecipeBuilder.builder().circuit(4)
            .fluidInputs(ANY_OIL, Materials.Hydrogen.getGas(552), Materials.Oxygen.getGas(10000),
                Materials.Steam.getGas(12000))
            .fluidOutputs(Materials.Styrene.getFluid(1656)).itemOutputs(dust(Materials.Carbon, 1))
            .duration(30 * SECONDS).eut(TierEU.RECIPE_MV).addTo(GTNGRecipeMaps.PetrochemicalSynthesisRecipes);

        GTRecipeBuilder.builder().circuit(5)
            .fluidInputs(ANY_OIL, Materials.Steam.getGas(12000), Materials.Oxygen.getGas(4080),
                Materials.Nitrogen.getGas(960))
            .fluidOutputs(Materials.NitroFuel.getFluid(12000), Materials.Hydrogen.getGas(5520),
                Materials.Water.getFluid(2040), Materials.Helium.getGas(1440), Materials.Methane.getGas(10080),
                Materials.Ethane.getGas(144), Materials.Propene.getGas(144))
            .itemOutputs(dust(Materials.Sulfur, 1), dust(Materials.Carbon, 12)).duration(16 * SECONDS)
            .eut(TierEU.RECIPE_HV).addTo(GTNGRecipeMaps.PetrochemicalSynthesisRecipes);

        GTRecipeBuilder.builder().circuit(6).fluidInputs(ANY_OIL, Materials.Steam.getGas(12000))
            .fluidOutputs(Materials.Diesel.getFluid(12000), Materials.Methane.getGas(10080),
                Materials.Propene.getGas(144), Materials.Helium.getGas(1000))
            .itemOutputs(dust(Materials.Sulfur, 2), dust(Materials.Carbon, 3)).duration(36 * SECONDS)
            .eut(TierEU.RECIPE_MV).addTo(GTNGRecipeMaps.PetrochemicalSynthesisRecipes);

        GTRecipeBuilder.builder().circuit(7)
            .fluidInputs(ANY_OIL, Materials.Steam.getGas(10870), Materials.Oxygen.getGas(2990),
                Materials.Nitrogen.getGas(4180))
            .fluidOutputs(Materials.GasolinePremium.getFluid(11130), Materials.Hydrogen.getGas(11620),
                Materials.NaphthenicAcid.getFluid(250))
            .itemOutputs(dust(Materials.Carbon, 80), dust(Materials.Sulfur, 3)).outputChances(8800, 8000)
            .duration(30 * SECONDS).eut(6144).addTo(GTNGRecipeMaps.PetrochemicalSynthesisRecipes);

        GTRecipeBuilder.builder().circuit(8).itemInputs(dust(Materials.Carbon, 9), dust(Materials.Sulfur, 1))
            .fluidInputs(Materials.Hydrogen.getGas(26000))
            .fluidOutputs(Materials.StyreneButadieneRubber.getMolten(9000)).duration(60 * SECONDS).eut(TierEU.RECIPE_MV)
            .addTo(GTNGRecipeMaps.PetrochemicalSynthesisRecipes);

        GTRecipeBuilder.builder().circuit(9)
            .itemInputs(dust(Materials.Carbon, 2), dust(Materials.Silicon, 1), dust(Materials.Sulfur, 1))
            .fluidInputs(Materials.Hydrogen.getGas(24000), Materials.Oxygen.getGas(10000))
            .fluidOutputs(Materials.RubberSilicone.getMolten(9000)).duration(16 * SECONDS).eut(TierEU.RECIPE_HV)
            .addTo(GTNGRecipeMaps.PetrochemicalSynthesisRecipes);

        GTRecipeBuilder.builder().circuit(10)
            .itemInputs(dust(Materials.Carbon, 6), dust(Materials.Sodium, 6), dust(Materials.Sulfur, 4))
            .fluidInputs(Materials.Hydrogen.getGas(4000), Materials.Oxygen.getGas(8000),
                Materials.Chlorine.getGas(2000))
            .fluidOutputs(Materials.PolyphenyleneSulfide.getMolten(1000)).duration(12 * SECONDS).eut(1960)
            .addTo(GTNGRecipeMaps.PetrochemicalSynthesisRecipes);

        GTRecipeBuilder.builder().circuit(11)
            .itemInputs(dust(Materials.Carbon, 25), dust(Materials.Sodium, 2), dust(Materials.Sulfur, 1))
            .fluidInputs(Materials.Hydrogen.getGas(30000), Materials.Oxygen.getGas(31000),
                Materials.Chlorine.getGas(32000))
            .fluidOutputs(Materials.Epoxid.getMolten(9000)).duration(16 * SECONDS).eut(TierEU.RECIPE_HV)
            .addTo(GTNGRecipeMaps.PetrochemicalSynthesisRecipes);

        GTRecipeBuilder.builder().circuit(12).itemInputs(dust(Materials.Carbon, 20))
            .fluidInputs(Materials.Hydrogen.getGas(12000), Materials.Oxygen.getGas(14000),
                Materials.Nitrogen.getGas(4000), Materials.Chlorine.getGas(4000))
            .fluidOutputs(Materials.Polybenzimidazole.getMolten(1000)).duration(16 * SECONDS).eut(10240)
            .addTo(GTNGRecipeMaps.PetrochemicalSynthesisRecipes);
    }

    private static ItemStack dust(Materials material, int amount) {
        return GTUtility.copyAmountUnsafe(amount, GTOreDictUnificator.get(OrePrefixes.dust, material, 1));
    }
}
