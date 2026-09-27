package com.xyp.gtnotgood.common.recipe.machine;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

/** Registers the LV controller recipe using a native assembler instead of the source-only steam module. */
public final class LargeCombProcessorRecipes {

    private LargeCombProcessorRecipes() {}

    /** Retains the upstream circuits, bronze plates, configuration, duration and EU/t. */
    public static void loadRecipes() {
        GTRecipeBuilder.builder()
            .itemInputs(
                ItemList.Machine_LV_Assembler.get(1),
                new Object[] { OrePrefixes.circuit.get(Materials.LV), 8 },
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Bronze, 2),
                GTUtility.getIntegratedCircuit(24))
            .itemOutputs(GTNGItemList.LargeCombProcessor.get(1))
            .duration(100)
            .eut(32)
            .addTo(RecipeMaps.assemblerRecipes);
    }
}
