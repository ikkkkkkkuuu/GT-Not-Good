package com.xyp.gtnotgood.common.recipe.machine;

import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import com.gtnewhorizon.cropsnh.api.CropsNHItemList;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

/**
 * Registers the assembler recipe for the electric large crop breeder.
 */
public final class LargeCropBreederRecipes {

    private LargeCropBreederRecipes() {}

    /**
     * Adds an MV assembler recipe centered on CropsNH crop sticks and standard GregTech electric machine parts.
     */
    public static void loadRecipes() {
        GTRecipeBuilder.builder()
            .itemInputs(
                ItemList.Hull_MV.get(1L),
                CropsNHItemList.cropSticks.get(16),
                GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Steel, 4),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 8),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.MV, 4),
                ItemList.Electric_Motor_MV.get(2L),
                ItemList.Electric_Piston_MV.get(2L),
                GTUtility.getIntegratedCircuit(23))
            .fluidInputs(Materials.SolderingAlloy.getMolten(576))
            .itemOutputs(GTNGItemList.LargeCropBreeder.get(1))
            .duration(30 * SECONDS)
            .eut(TierEU.RECIPE_MV)
            .addTo(assemblerRecipes);
    }
}
