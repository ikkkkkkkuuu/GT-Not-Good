package com.xyp.gtnotgood.common.recipe.gregtech;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipeBuilder;

public final class CentrifugeRecipes {

    private CentrifugeRecipes() {}

    public static void loadRecipes() {
        GTRecipeBuilder.builder().itemInputs(Materials.Borax.getDust(23), ItemList.Cell_Empty.get(10))
            .itemOutputs(Materials.Sodium.getDust(2), Materials.Boron.getDust(4), Materials.Water.getCells(10))
            .fluidOutputs(Materials.Oxygen.getGas(7000)).duration(230).eut(TierEU.RECIPE_LV)
            .addTo(RecipeMaps.centrifugeRecipes);
    }
}
