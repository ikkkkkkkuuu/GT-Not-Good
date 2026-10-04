package com.xyp.gtnotgood.common.recipe.machine;

import static gregtech.api.enums.TierEU.RECIPE_UXV;
import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;

public final class CrossRecipeWirelessEnergyHatchRecipes {

    private CrossRecipeWirelessEnergyHatchRecipes() {}

    public static void loadRecipes() {
        GTRecipeBuilder.builder()
            .itemInputs(
                ItemList.HATCHES_ENERGY[VoltageIndex.UXV].get(1),
                ItemList.WIRELESS_ENERGY_COVERS[VoltageIndex.UXV - VoltageIndex.LV].get(4),
                new Object[] { OrePrefixes.circuit.get(Materials.UXV), 4 },
                ItemList.Emitter_UXV.get(2),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Neutronium, 8))
            .circuit(24)
            .fluidInputs(Materials.SolderingAlloy.getMolten(2880))
            .itemOutputs(GTNGItemList.CrossRecipeWirelessEnergyHatch.get(1))
            .duration(100 * SECONDS)
            .eut(RECIPE_UXV)
            .addTo(assemblerRecipes);
    }
}
