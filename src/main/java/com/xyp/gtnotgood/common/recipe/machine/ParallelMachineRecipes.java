package com.xyp.gtnotgood.common.recipe.machine;

import com.xyp.gtnotgood.loader.ParallelMachineLoader;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMaps;

public final class ParallelMachineRecipes {

    private ParallelMachineRecipes() {}

    public static void loadRecipes() {
        GTValues.RA.stdBuilder()
            .itemInputs(ItemList.Machine_LV_E_Furnace.get(4), ItemList.Casing_SolidSteel.get(4),
                new Object[] { OrePrefixes.circuit.get(Materials.LV), 4 }, Materials.Steel.getPlates(8))
            .itemOutputs(GTNGItemList.LargeFurnace.get(1)).duration(400).eut(30).addTo(RecipeMaps.assemblerRecipes);
        Materials[] circuits = { Materials.IV, Materials.LuV, Materials.ZPM, Materials.UV, Materials.UHV, Materials.UEV,
            Materials.UIV, Materials.UMV, Materials.UXV, Materials.MAX };
        for (int i = 0; i < circuits.length; i++) {
            int tier = i + 5;
            GTValues.RA.stdBuilder()
                .itemInputs(ItemList.MACHINE_HULLS[tier].get(1), ItemList.valueOf("Sensor_" + GTValues.VN[tier]).get(2),
                    new Object[] { OrePrefixes.circuit.get(circuits[i]), 4 })
                .itemOutputs(ParallelMachineLoader.HATCHES[i].get(1)).duration(200).eut(GTValues.VP[tier])
                .addTo(RecipeMaps.assemblerRecipes);
        }
    }
}
