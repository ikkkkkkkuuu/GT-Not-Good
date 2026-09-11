package com.xyp.gtnotgood.common.recipe.machine;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;

/** Registers the EV-stage assembler recipe for the integrated production controller. */
public final class IntegratedProductionFactoryRecipes {

    private IntegratedProductionFactoryRecipes() {}

    /** Uses standard EV components and ore-dictionary circuits, without late-game materials. */
    public static void loadRecipes() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Hull_EV.get(2),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.EV, 4),
                ItemList.Robot_Arm_EV.get(2),
                ItemList.Conveyor_Module_EV.get(4),
                Materials.StainlessSteel.getPlates(16),
                GTUtility.getIntegratedCircuit(24))
            .fluidInputs(Materials.SolderingAlloy.getMolten(1152))
            .itemOutputs(GTNGItemList.IntegratedProductionFactory.get(1))
            .eut(TierEU.RECIPE_EV)
            .duration(20 * 120)
            .addTo(RecipeMaps.assemblerRecipes);
    }
}
