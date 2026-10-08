package com.xyp.gtnotgood.common.recipe.machine;

import static gregtech.api.enums.TierEU.RECIPE_LV;
import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;

/** Registers assembler upgrades from ordinary ME outputs to their maximum-capacity variants. */
public final class MaxCapacityMEOutputRecipes {

    private MaxCapacityMEOutputRecipes() {}

    /** Adds upgrades that retain the ordinary ME output bus and hatch as crafting prerequisites. */
    public static void loadRecipes() {
        GTRecipeBuilder.builder()
            .itemInputs(ItemList.Hatch_Output_Bus_ME.get(1L), new Object[] { OrePrefixes.circuit.get(Materials.LV), 2 },
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 2))
            .itemOutputs(GTNGItemList.MaxCapacityMEOutputBus.get(1)).duration(5 * SECONDS).eut(RECIPE_LV)
            .addTo(assemblerRecipes);

        GTRecipeBuilder.builder()
            .itemInputs(ItemList.Hatch_Output_ME.get(1L), new Object[] { OrePrefixes.circuit.get(Materials.LV), 2 },
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 2))
            .itemOutputs(GTNGItemList.MaxCapacityMEOutputHatch.get(1)).duration(5 * SECONDS).eut(RECIPE_LV)
            .addTo(assemblerRecipes);
    }
}
