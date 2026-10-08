package com.xyp.gtnotgood.common.recipe.gregtech;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModsItemlist;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

public class AssemblerRecipes {

    public static void loadRecipes() {
        GTRecipeBuilder.builder()
            .itemInputs(GTOreDictUnificator.get(OrePrefixes.plate, Materials.Copper, 4),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 4),
                new Object[] { OrePrefixes.circuit.get(Materials.LV), 4 }, GTUtility.getIntegratedCircuit(24))
            .itemOutputs(GTNGItemList.LargeOreProcessor.get(1)).duration(200).eut(32)
            .addTo(RecipeMaps.assemblerRecipes);

        // 生碳纤维
        GTRecipeBuilder.builder().itemInputs(GTOreDictUnificator.get(OrePrefixes.dust, Materials.Carbon, 4))
            .fluidInputs(Materials.Polyethylene.getMolten(36)).itemOutputs(ModsItemlist.RawCarbonFibre.get(1))
            .duration(1).eut(30).addTo(RecipeMaps.assemblerRecipes);
    }
}
