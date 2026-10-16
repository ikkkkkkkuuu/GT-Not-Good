package com.xyp.gtnotgood.common.recipe.machine;

import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;

public class SingularityDataHubRecipes {

    public static void loadRecipes() {
        // 保险库
        GTRecipeBuilder.builder()
            .itemInputs(GTOreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 4),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Aluminium, 4),
                new Object[] { OrePrefixes.circuit.get(Materials.LV), 4 })
            .circuit(24).itemOutputs(GTNGItemList.SingularityDataHub.get(1)).duration(5 * SECONDS).eut(32)
            .addTo(RecipeMaps.assemblerRecipes);
        // 保险库数据中心
        GTRecipeBuilder.builder()
            .itemInputs(GTOreDictUnificator.get(OrePrefixes.plate, Materials.Iron, 4),
                GTOreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 4),
                new Object[] { OrePrefixes.circuit.get(Materials.LV), 4 })
            .circuit(24).itemOutputs(GTNGItemList.VaultPortHatch.get(1)).duration(5 * SECONDS).eut(32)
            .addTo(RecipeMaps.assemblerRecipes);

        GTRecipeBuilder.builder()
            .itemInputs(GTOreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 6),
                GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Steel, 1),
                GTOreDictUnificator.get(OrePrefixes.foil, Materials.Copper, 4))
            .circuit(24).fluidInputs(Materials.Tin.getMolten(144)).itemOutputs(GTNGItemList.EddingtonWallCasing.get(2))
            .duration(10 * SECONDS).eut(TierEU.RECIPE_MV).addTo(RecipeMaps.assemblerRecipes);
    }

}
