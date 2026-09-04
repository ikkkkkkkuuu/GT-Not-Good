package com.xyp.gtnotgood.common.recipe.machine;

import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;

/**
 * Registers the assembler recipes for the super ME pattern input hatch family.
 */
public final class SuperCraftingInputRecipes {

    private SuperCraftingInputRecipes() {}

    /**
     * Adds the three LV-tier assembler recipes copied from GT-Not-Cool.
     */
    public static void loadRecipes() {
        GTRecipeBuilder.builder()
            .itemInputs(
                ItemList.Hatch_Input_Bus_LV.get(1L),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.LV, 2),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 2))
            .circuit(22)
            .itemOutputs(GTNGItemList.SuperMTEHatchCraftingInputBusME.get(1))
            .duration(5 * SECONDS)
            .eut(32)
            .addTo(assemblerRecipes);

        GTRecipeBuilder.builder()
            .itemInputs(
                ItemList.Hatch_Input_Bus_LV.get(1L),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.LV, 2),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 2))
            .circuit(21)
            .itemOutputs(GTNGItemList.SuperMTEHatchCraftingInputME.get(1))
            .duration(5 * SECONDS)
            .eut(32)
            .addTo(assemblerRecipes);

        GTRecipeBuilder.builder()
            .itemInputs(
                ItemList.Hatch_Input_Bus_LV.get(1L),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.LV, 2),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 2))
            .circuit(23)
            .itemOutputs(GTNGItemList.SuperMTEHatchCraftingInputSlave.get(1))
            .duration(5 * SECONDS)
            .eut(32)
            .addTo(assemblerRecipes);
    }
}
