package com.xyp.gtnotgood.common.recipe.machine;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;

/**
 * User-requested MV access to adjustable GTNH wireless connectors. MV hulls, circuits and
 * emitter/sensor components distinguish the two directions; both assemble at MV voltage.
 */
public final class FluxConnectorRecipes {

    private FluxConnectorRecipes() {}

    public static void loadRecipes() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                GTNGItemList.FluxPlug.get(1),
                GTNGItemList.MEBridgeReceiver.get(1),
                ItemList.Conveyor_Module_MV.get(1),
                ItemList.Electric_Pump_MV.get(1))
            .circuit(3)
            .itemOutputs(GTNGItemList.FluxLogisticsPlug.get(1))
            .duration(200)
            .eut(TierEU.RECIPE_MV)
            .addTo(RecipeMaps.assemblerRecipes);
        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Hull_MV.get(1),
                ItemList.Emitter_MV.get(1),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.MV, 2),
                new ItemStack(Items.ender_pearl, 2))
            .circuit(1)
            .itemOutputs(GTNGItemList.FluxPlug.get(1))
            .duration(200)
            .eut(TierEU.RECIPE_MV)
            .addTo(RecipeMaps.assemblerRecipes);
        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Hull_MV.get(1),
                ItemList.Sensor_MV.get(1),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.MV, 2),
                new ItemStack(Items.ender_pearl, 2))
            .circuit(2)
            .itemOutputs(GTNGItemList.FluxPoint.get(1))
            .duration(200)
            .eut(TierEU.RECIPE_MV)
            .addTo(RecipeMaps.assemblerRecipes);
    }
}
