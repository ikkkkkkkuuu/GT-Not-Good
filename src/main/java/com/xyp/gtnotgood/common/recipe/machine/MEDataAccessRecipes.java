package com.xyp.gtnotgood.common.recipe.machine;

import static gregtech.api.enums.TierEU.RECIPE_IV;
import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModsItemlist;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTRecipeBuilder;

public final class MEDataAccessRecipes {

    private MEDataAccessRecipes() {}

    public static void loadRecipes() {
        GTRecipeBuilder.builder()
            .itemInputs(ItemList.Hatch_DataAccess_EV.get(1), ModsItemlist.MEInterface.get(1), ItemList.Hull_IV.get(1),
                new Object[] { OrePrefixes.circuit.get(Materials.IV), 2 }, ItemList.Sensor_IV.get(1),
                ItemList.Emitter_IV.get(1))
            .fluidInputs(Materials.SolderingAlloy.getMolten(576)).itemOutputs(GTNGItemList.MEDataAccessHatch.get(1))
            .duration(20 * SECONDS).eut(RECIPE_IV).addTo(assemblerRecipes);
    }
}
