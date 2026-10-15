package com.xyp.gtnotgood.common.recipe.machine;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

public final class IntegratedChemicalPlantRecipes {

    private IntegratedChemicalPlantRecipes() {}

    public static void loadRecipes() {
        GTRecipeBuilder.builder()
            .itemInputs(ItemList.Hull_EV.get(1), ItemList.Machine_EV_ChemicalReactor.get(1),
                ItemList.Machine_EV_Distillery.get(1), ItemList.Electric_Pump_EV.get(4),
                Materials.StainlessSteel.getPlates(16), new Object[] { OrePrefixes.circuit.get(Materials.EV), 4 },
                GTUtility.getIntegratedCircuit(22))
            .fluidInputs(Materials.SolderingAlloy.getMolten(1152))
            .itemOutputs(GTNGItemList.IntegratedChemicalPlant.get(1)).duration(1200).eut(TierEU.RECIPE_EV)
            .addTo(RecipeMaps.assemblerRecipes);
    }
}
