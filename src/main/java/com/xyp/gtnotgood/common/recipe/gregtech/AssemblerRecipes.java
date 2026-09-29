package com.xyp.gtnotgood.common.recipe.gregtech;

import com.xyp.gtnotgood.utils.enums.ModsItemlist;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;

public class AssemblerRecipes {

    public static void loadRecipes() {
        // 生碳纤维
        GTRecipeBuilder.builder()
            .itemInputs(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Carbon, 4))
            .fluidInputs(Materials.Polyethylene.getMolten(36))
            .itemOutputs(ModsItemlist.IC2ItemPartCarbonFibre.get(1))
            .duration(1)
            .eut(30)
            .addTo(RecipeMaps.assemblerRecipes);
    }
}
