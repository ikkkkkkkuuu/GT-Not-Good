package com.xyp.gtnotgood.common.recipe.machine;

import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModsItemlist;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;

public final class BasicIndustrialApiaryRecipes {

    private BasicIndustrialApiaryRecipes() {}

    public static void loadRecipes() {
        GTRecipeBuilder.builder()
            .itemInputs(ItemList.Hull_LV.get(1), ModsItemlist.Apiary.get(1),
                new Object[] { OrePrefixes.circuit.get(Materials.LV), 2 }, ItemList.Electric_Motor_LV.get(2),
                ItemList.Electric_Piston_LV.get(1), GTOreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 4))
            .fluidInputs(Materials.SolderingAlloy.getMolten(144)).itemOutputs(GTNGItemList.BasicIndustrialApiary.get(1))
            .duration(20 * SECONDS).eut(TierEU.RECIPE_LV).addTo(assemblerRecipes);
    }
}
