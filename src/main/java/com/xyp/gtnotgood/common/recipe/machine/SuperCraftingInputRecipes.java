package com.xyp.gtnotgood.common.recipe.machine;

import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;

/**
 * Registers assembly and size-conversion recipes for the super ME pattern input hatch family.
 */
public final class SuperCraftingInputRecipes {

    private SuperCraftingInputRecipes() {}

    /**
     * Adds LV-tier assembly recipes and one-to-one shapeless hatch size conversions.
     */
    public static void loadRecipes() {
        GTRecipeBuilder.builder()
            .itemInputs(GTNGItemList.SuperMTEHatchCraftingInputME.get(1),
                new Object[] { OrePrefixes.circuit.get(Materials.LV), 2 },
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 2))
            .circuit(23).itemOutputs(GTNGItemList.CircuitMEPatternBuffer.get(1)).duration(5 * SECONDS).eut(32)
            .addTo(assemblerRecipes);
        GTRecipeBuilder.builder()
            .itemInputs(ItemList.Hatch_Input_ME_Advanced.get(1L),
                new Object[] { OrePrefixes.circuit.get(Materials.LV), 2 },
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 2))
            .itemOutputs(GTNGItemList.SuperAdvancedMEInputHatch.get(1)).duration(5 * SECONDS).eut(32)
            .addTo(assemblerRecipes);
        GTRecipeBuilder.builder()
            .itemInputs(ItemList.Hatch_Input_Bus_ME_Advanced.get(1L),
                new Object[] { OrePrefixes.circuit.get(Materials.LV), 2 },
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 2))
            .itemOutputs(GTNGItemList.SuperAdvancedMEInputBus.get(1)).duration(5 * SECONDS).eut(32)
            .addTo(assemblerRecipes);
        GTRecipeBuilder.builder()
            .itemInputs(ItemList.Hatch_Input_Bus_LV.get(1L), new Object[] { OrePrefixes.circuit.get(Materials.LV), 2 },
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 2))
            .circuit(22).itemOutputs(GTNGItemList.SuperMTEHatchCraftingInputBusME.get(1)).duration(5 * SECONDS).eut(32)
            .addTo(assemblerRecipes);

        GTRecipeBuilder.builder()
            .itemInputs(ItemList.Hatch_Input_Bus_LV.get(1L), new Object[] { OrePrefixes.circuit.get(Materials.LV), 2 },
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 2))
            .circuit(21).itemOutputs(GTNGItemList.SuperMTEHatchCraftingInputME.get(1)).duration(5 * SECONDS).eut(32)
            .addTo(assemblerRecipes);

        GTRecipeBuilder.builder()
            .itemInputs(ItemList.Hatch_Input_Bus_LV.get(1L), new Object[] { OrePrefixes.circuit.get(Materials.LV), 2 },
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 2))
            .circuit(23).itemOutputs(GTNGItemList.SuperMTEHatchCraftingInputSlave.get(1)).duration(5 * SECONDS).eut(32)
            .addTo(assemblerRecipes);

        GTRecipeBuilder.builder()
            .itemInputs(ItemList.Hatch_Input_Bus_LV.get(1L), new Object[] { OrePrefixes.circuit.get(Materials.LV), 2 },
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 2))
            .circuit(24).itemOutputs(GTNGItemList.CompactSuperMTEHatchCraftingInputME.get(1)).duration(5 * SECONDS)
            .eut(32).addTo(assemblerRecipes);

        GameRegistry.addShapelessRecipe(GTNGItemList.CompactSuperMTEHatchCraftingInputME.get(1),
            GTNGItemList.SuperMTEHatchCraftingInputME.get(1));
        GameRegistry.addShapelessRecipe(GTNGItemList.SuperMTEHatchCraftingInputME.get(1),
            GTNGItemList.CompactSuperMTEHatchCraftingInputME.get(1));
    }
}
