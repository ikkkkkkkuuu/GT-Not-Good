package com.xyp.gtnotgood.common.recipe.machine;

import static gregtech.api.enums.TierEU.RECIPE_LV;
import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;

/** LV assembler progression for the three network blocks, with distinct selector circuits and no crafting overrides. */
public final class NetworkRecipes {

    private NetworkRecipes() {}

    /** Registers recipes with normal GT collision checks; connectors and controllers require this mod's own pipes. */
    public static void loadRecipes() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                GTOreDictUnificator.get(OrePrefixes.pipeSmall, Materials.Tin, 2),
                GTOreDictUnificator.get(OrePrefixes.cableGt01, Materials.Copper, 2),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Rubber, 2),
                new ItemStack(Items.redstone))
            .circuit(21)
            .itemOutputs(GTNGItemList.NetworkPipe.get(8))
            .duration(5 * SECONDS)
            .eut(RECIPE_LV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                GTNGItemList.NetworkPipe.get(2),
                ItemList.Conveyor_Module_LV.get(1),
                ItemList.Electric_Pump_LV.get(1),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 2))
            .circuit(22)
            .itemOutputs(GTNGItemList.NetworkConnector.get(2))
            .duration(5 * SECONDS)
            .eut(RECIPE_LV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Hull_LV.get(1),
                GTNGItemList.NetworkConnector.get(2),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.LV, 2),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 2),
                new ItemStack(Items.redstone, 2))
            .circuit(23)
            .itemOutputs(GTNGItemList.NetworkController.get(1))
            .duration(10 * SECONDS)
            .eut(RECIPE_LV)
            .addTo(assemblerRecipes);
    }
}
