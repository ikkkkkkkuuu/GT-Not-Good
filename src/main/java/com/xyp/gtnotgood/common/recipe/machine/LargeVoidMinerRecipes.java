package com.xyp.gtnotgood.common.recipe.machine;

import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;

/**
 * Registers the assembler recipe for the electric Large Void Miner.
 */
public final class LargeVoidMinerRecipes {

    private LargeVoidMinerRecipes() {}

    /**
     * Adds a steel-tier assembler recipe that upgrades a basic miner into the large void miner controller.
     */
    public static void loadRecipes() {
        GTRecipeBuilder.builder()
            .itemInputs(
                ItemList.Machine_LV_Miner.get(1L),
                GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Steel, 4),
                GTOreDictUnificator.get(OrePrefixes.plateTriple, Materials.Steel, 8),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.LV, 4),
                ItemList.Electric_Motor_LV.get(4L),
                ItemList.Electric_Piston_LV.get(4L))
            .fluidInputs(Materials.SolderingAlloy.getMolten(576))
            .itemOutputs(GTNGItemList.LargeVoidMiner.get(1))
            .duration(30 * SECONDS)
            .eut(TierEU.RECIPE_LV)
            .addTo(assemblerRecipes);
    }
}
