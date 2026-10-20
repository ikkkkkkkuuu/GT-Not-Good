package com.xyp.gtnotgood.common.recipe.gregtech;

import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipeBuilder;

public final class FormingPressRecipes {

    private FormingPressRecipes() {}

    public static void loadRecipes() {
        GTRecipeBuilder.builder().itemInputs(Materials.Iron.getDust(4), ItemList.Shape_Mold_Rod_Long.get(0))
            .itemOutputs(GTNGItemList.IronElectrode.get(1)).duration(10 * SECONDS).eut(TierEU.RECIPE_LV)
            .addTo(RecipeMaps.formingPressRecipes);
    }
}
