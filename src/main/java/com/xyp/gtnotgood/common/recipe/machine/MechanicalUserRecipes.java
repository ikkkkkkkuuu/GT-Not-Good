package com.xyp.gtnotgood.common.recipe.machine;

import static gregtech.api.enums.TierEU.RECIPE_LV;
import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;

/** LV assembler recipes for the powerless user and its dedicated speed upgrades. */
public final class MechanicalUserRecipes {

    private MechanicalUserRecipes() {}

    public static void loadRecipes() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Hull_LV.get(1),
                ItemList.Robot_Arm_LV.get(1),
                ItemList.Conveyor_Module_LV.get(1),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.LV, 2),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 2))
            .circuit(1)
            .itemOutputs(GTNGItemList.MechanicalUser.get(1))
            .duration(10 * SECONDS)
            .eut(RECIPE_LV)
            .addTo(assemblerRecipes);
        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Electric_Motor_LV.get(1),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.LV, 1),
                GTOreDictUnificator.get(OrePrefixes.wireGt01, Materials.Copper, 2))
            .circuit(2)
            .itemOutputs(GTNGItemList.MechanicalUserSpeedUpgrade.get(1))
            .duration(5 * SECONDS)
            .eut(RECIPE_LV)
            .addTo(assemblerRecipes);
    }
}
