// SPDX-License-Identifier: CC-BY-NC-SA-4.0
package com.xyp.gtnotgood.common.recipe.machine;

import static gregtech.api.util.GTRecipeConstants.AssemblyLine;
import static gregtech.api.util.GTRecipeConstants.RESEARCH_ITEM;
import static gregtech.api.util.GTRecipeConstants.SCANNING;

import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import bartworks.common.loaders.ItemRegistry;
import gregtech.api.casing.Casings;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import gregtech.api.util.recipe.Scanning;
import gtPlusPlus.core.material.MaterialsAlloy;

// GTL 1.4.5.3 recipe adaptations: META-INF/field-extruder-port/NOTICE.md.
public final class FieldExtruderRecipes {

    private FieldExtruderRecipes() {}

    public static void loadRecipes() {
        controller();
        hyperMechanicalCasing();
        cleanroomGlass();
    }

    private static void controller() {
        GTRecipeBuilder.builder().metadata(RESEARCH_ITEM, ItemList.IndustrialExtruder.get(1))
            .metadata(SCANNING, new Scanning(1200, TierEU.RECIPE_UEV))
            .itemInputs(ItemList.ExtruderUHV.get(4), ItemList.CompressorUHV.get(4), ItemList.Field_Generator_UEV.get(8),
                ItemList.Emitter_UEV.get(4), new Object[] { OrePrefixes.circuit.get(Materials.UIV), 4 },
                Materials.Neutronium.getNanite(4), ItemList.Casing_Advanced_Iridium.get(64),
                Materials.Neutronium.getPlates(64))
            .fluidInputs(MaterialsAlloy.INDALLOY_140.getFluidStack(2304), Materials.Platinum.getMolten(4608),
                Materials.EpoxidFiberReinforced.getMolten(4608), Materials.SuperconductorUVBase.getMolten(4608))
            .itemOutputs(GTNGItemList.FieldExtruderFactory.get(1)).duration(800).eut(TierEU.RECIPE_UEV)
            .addTo(AssemblyLine);
    }

    private static void hyperMechanicalCasing() {
        GTRecipeBuilder.builder()
            .itemInputs(GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.NaquadahAlloy, 1),
                Materials.Naquadria.getPlates(6), GTUtility.getIntegratedCircuit(6))
            .itemOutputs(GTNGItemList.HyperMechanicalCasing.get(2)).duration(50).eut(16)
            .addTo(RecipeMaps.assemblerRecipes);
    }

    private static void cleanroomGlass() {
        GTRecipeBuilder.builder()
            .itemInputs(new ItemStack(ItemRegistry.bw_realglas, 4, 0),
                Casings.CleanStainlessSteelMachineCasing.toStack(1), Materials.RubberSilicone.getPlates(2),
                GTUtility.getIntegratedCircuit(6))
            .itemOutputs(GTNGItemList.CleanroomGlass.get(4)).duration(100).eut(TierEU.RECIPE_EV)
            .addTo(RecipeMaps.assemblerRecipes);
    }
}
