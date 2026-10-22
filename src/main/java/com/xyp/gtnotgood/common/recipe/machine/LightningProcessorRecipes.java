// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.common.recipe.machine;

import com.xyp.gtnotgood.loader.GTNGRecipeMaps;

import gregtech.api.enums.Materials;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

public final class LightningProcessorRecipes {

    private LightningProcessorRecipes() {}

    public static void loadRecipes() {
        GTRecipeBuilder.builder()
            .itemInputs(Materials.Beryllium.getDust(1), Materials.Potassium.getDust(4),
                GTUtility.getIntegratedCircuit(1))
            .fluidInputs(Materials.Nitrogen.getGas(5000)).itemOutputs(Materials.EnderPearl.getDust(10)).duration(400)
            .eut(TierEU.RECIPE_LV).addTo(GTNGRecipeMaps.LightningProcessorRecipes);
    }
}
