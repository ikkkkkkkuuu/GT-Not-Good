package com.xyp.gtnotgood.common.recipe.machine;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

/** EV controller recipe using existing GTNH parts. */
public final class LargeTransmutationMachineRecipes {

    private LargeTransmutationMachineRecipes() {}

    public static void loadRecipes() {
        GTRecipeBuilder.builder()
            .itemInputs(
                ItemList.Hull_EV.get(1),
                ItemList.Robot_Arm_EV.get(2),
                ItemList.Electric_Piston_EV.get(4),
                ItemList.Sensor_EV.get(2),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Titanium, 8),
                new Object[] { OrePrefixes.circuit.get(Materials.EV), 4 },
                GTUtility.getIntegratedCircuit(21))
            .fluidInputs(Materials.SolderingAlloy.getMolten(576))
            .itemOutputs(GTNGItemList.LargeTransmutationMachine.get(1))
            .duration(600)
            .eut(TierEU.RECIPE_EV)
            .addTo(RecipeMaps.assemblerRecipes);
    }
}
