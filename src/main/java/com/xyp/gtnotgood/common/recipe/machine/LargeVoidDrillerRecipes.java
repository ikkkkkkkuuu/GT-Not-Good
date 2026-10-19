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

public final class LargeVoidDrillerRecipes {

    private LargeVoidDrillerRecipes() {}

    public static void loadRecipes() {
        GTRecipeBuilder.builder()
            .itemInputs(ItemList.Pump_LV.get(1), GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Steel, 4),
                GTOreDictUnificator.get(OrePrefixes.plateTriple, Materials.Steel, 8),
                new Object[] { OrePrefixes.circuit.get(Materials.LV), 4 }, ItemList.Electric_Motor_LV.get(4),
                ItemList.Electric_Pump_LV.get(4))
            .fluidInputs(Materials.SolderingAlloy.getMolten(576)).itemOutputs(GTNGItemList.LargeVoidDriller.get(1))
            .duration(30 * SECONDS).eut(TierEU.RECIPE_LV).addTo(assemblerRecipes);
    }
}
