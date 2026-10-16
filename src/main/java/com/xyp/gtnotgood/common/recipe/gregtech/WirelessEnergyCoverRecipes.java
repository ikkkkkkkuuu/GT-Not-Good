// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT Not Leisure; copyright ABKQPO and contributors.
package com.xyp.gtnotgood.common.recipe.gregtech;

import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.utils.enums.ModList;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.objects.SubstituteFluidStack;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;

public final class WirelessEnergyCoverRecipes {

    private WirelessEnergyCoverRecipes() {}

    public static void loadRecipes() {
        if (ModList.GTNotLeisure.isModLoaded()) return;

        Materials[] circuits = { Materials.LV, Materials.MV, Materials.HV, Materials.EV, Materials.IV, Materials.LuV,
            Materials.ZPM, Materials.UV, Materials.UHV, Materials.UEV, Materials.UIV, Materials.UMV, Materials.UXV,
            Materials.MAX };
        Materials[] cables = { Materials.Tin, Materials.Copper, Materials.Gold, Materials.Aluminium, Materials.Tungsten,
            Materials.VanadiumGallium, Materials.Naquadah, Materials.NaquadahAlloy, Materials.Bedrockium,
            Materials.Draconium, Materials.NetherStar, Materials.Quantium, Materials.BlackPlutonium,
            Materials.DraconiumAwakened };
        Materials[] plates = { Materials.Steel, Materials.Aluminium, Materials.StainlessSteel, Materials.Titanium,
            Materials.TungstenSteel, Materials.Iridium, Materials.NaquadahAlloy, Materials.Osmium, Materials.Neutronium,
            Materials.Bedrockium, Materials.BlackPlutonium, Materials.SpaceTime, Materials.MagMatter,
            Materials.MHDCSM };
        ItemStack[] coils = { ItemList.LV_Coil.get(1), ItemList.MV_Coil.get(2), ItemList.HV_Coil.get(3),
            ItemList.EV_Coil.get(3), ItemList.IV_Coil.get(4), ItemList.LuV_Coil.get(8), ItemList.ZPM_Coil.get(8),
            ItemList.UV_Coil.get(8), ItemList.UHV_Coil.get(8), ItemList.UHV_Coil.get(8), ItemList.UHV_Coil.get(12),
            ItemList.UHV_Coil.get(12), ItemList.UHV_Coil.get(16), ItemList.UHV_Coil.get(32) };
        ItemStack[] chips = { GTOreDictUnificator.get(OrePrefixes.spring, Materials.Tin, 16),
            ItemList.Circuit_Chip_ULPIC.get(2), ItemList.Circuit_Chip_LPIC.get(2), ItemList.Circuit_Chip_PIC.get(3),
            ItemList.Circuit_Chip_HPIC.get(3), ItemList.Circuit_Chip_UHPIC.get(4), ItemList.Circuit_Chip_NPIC.get(4),
            ItemList.Circuit_Chip_PPIC.get(6), ItemList.Circuit_Chip_QPIC.get(6), ItemList.Circuit_Chip_QPIC.get(8),
            ItemList.Circuit_Chip_QPIC.get(8), ItemList.Circuit_Chip_QPIC.get(12), ItemList.Circuit_Chip_QPIC.get(16),
            ItemList.Circuit_Chip_QPIC.get(32) };

        for (int tier = VoltageIndex.LV; tier <= VoltageIndex.MAX; tier++) {
            int index = tier - VoltageIndex.LV;
            OrePrefixes cable = tier >= VoltageIndex.UMV ? OrePrefixes.wireGt01 : OrePrefixes.cableGt01;
            GTValues.RA.stdBuilder()
                .itemInputs(ItemList.valueOf("Sensor_" + GTValues.VN[tier]).get(1),
                    GTOreDictUnificator.get(OrePrefixes.plate, Materials.EnderPearl, 12),
                    GTOreDictUnificator.get(OrePrefixes.circuit, circuits[index], 4), coils[index], chips[index],
                    GTOreDictUnificator.get(cable, cables[index], 8),
                    GTOreDictUnificator.get(cable, Materials.RedAlloy, 32),
                    GTOreDictUnificator.get(OrePrefixes.plate, plates[index], 12))
                .fluidInputs(SubstituteFluidStack.soldering(144))
                .itemOutputs(ItemList.WIRELESS_ENERGY_COVERS[index].get(1)).duration(200).eut(GTValues.VP[tier])
                .addTo(RecipeMaps.assemblerRecipes);
        }
    }
}
