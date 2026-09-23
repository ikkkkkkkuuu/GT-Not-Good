package com.xyp.gtnotgood.common.recipe.gregtech;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.recipe.RecipeMaps;

/**
 * Manufacturing and spent-rod recovery for the configurable example fuel rod.
 * Recovered native depleted uranium rods retain GregTech's normal downstream processing route.
 */
public final class FuelRodRecipes {

    private FuelRodRecipes() {}

    /** Registers one-to-one assembly and deterministic centrifuge recovery recipes. */
    public static void loadRecipes() {
        GTValues.RA.stdBuilder()
            .itemInputs(ItemList.RodUranium4.get(1), Materials.Iron.getDust(4))
            .itemOutputs(GTNGItemList.IronFuelRod.get(1))
            .duration(20 * 10)
            .eut(1920)
            .addTo(RecipeMaps.assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(GTNGItemList.DepletedIronFuelRod.get(1))
            .itemOutputs(ItemList.DepletedRodUranium4.get(1), Materials.Iron.getDust(4))
            .duration(20 * 10)
            .eut(1920)
            .addTo(RecipeMaps.centrifugeRecipes);
    }
}
