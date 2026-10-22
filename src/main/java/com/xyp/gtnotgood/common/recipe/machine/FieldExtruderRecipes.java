// SPDX-License-Identifier: CC-BY-NC-SA-4.0
package com.xyp.gtnotgood.common.recipe.machine;

import static gregtech.api.util.GTRecipeConstants.AssemblyLine;
import static gregtech.api.util.GTRecipeConstants.RESEARCH_ITEM;
import static gregtech.api.util.GTRecipeConstants.SCANNING;

import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import bartworks.common.loaders.ItemRegistry;
import bartworks.system.material.WerkstoffLoader;
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
        iridiumCasing();
        hyperMechanicalCasing();
        molecularCasing();
        hollowCasing();
        containmentFieldGenerator();
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

    private static void iridiumCasing() {
        GTRecipeBuilder.builder().metadata(RESEARCH_ITEM, Casings.StainlessSteelTurbineCasing.toStack(1))
            .metadata(SCANNING, new Scanning(1200, TierEU.RECIPE_LuV))
            .itemInputs(GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Iridium, 2),
                Casings.TitaniumTurbineCasing.toStack(1), Casings.StainlessSteelTurbineCasing.toStack(1),
                GTOreDictUnificator.get(OrePrefixes.foil, Materials.Osmiridium, 4),
                GTOreDictUnificator.get(OrePrefixes.foil, Materials.Iridium, 4),
                MaterialsAlloy.TANTALLOY_61.getPlate(1),
                GTOreDictUnificator.get(OrePrefixes.plateDouble, Materials.Iridium, 1),
                GTOreDictUnificator.get(OrePrefixes.plateDouble, Materials.Osmiridium, 1))
            .fluidInputs(Materials.SolderingAlloy.getMolten(1440), Materials.Iridium.getMolten(576))
            .itemOutputs(GTNGItemList.IridiumCasing.get(2)).duration(200).eut(TierEU.RECIPE_LuV).addTo(AssemblyLine);
    }

    private static void hyperMechanicalCasing() {
        GTRecipeBuilder.builder()
            .itemInputs(GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.NaquadahAlloy, 1),
                Materials.Naquadria.getPlates(6), GTUtility.getIntegratedCircuit(6))
            .itemOutputs(GTNGItemList.HyperMechanicalCasing.get(2)).duration(50).eut(16)
            .addTo(RecipeMaps.assemblerRecipes);
    }

    private static void molecularCasing() {
        // Consolidate the original alloy foils and noble-metal plates for the native six-slot assembler.
        GTRecipeBuilder.builder()
            .itemInputs(Casings.HighPowerCasing.toStack(1),
                GTOreDictUnificator.get(OrePrefixes.plateDouble, Materials.BatteryAlloy, 4), ItemList.Emitter_IV.get(1),
                GTOreDictUnificator.get(OrePrefixes.ring, Materials.Neutronium, 24),
                WerkstoffLoader.Ruridit.get(OrePrefixes.foil, 48), WerkstoffLoader.Rhodium.get(OrePrefixes.plate, 14))
            .fluidInputs(Materials.NiobiumNitride.getMolten(864)).itemOutputs(GTNGItemList.MolecularCasing.get(1))
            .duration(400).eut(TierEU.RECIPE_UV).requiresCleanRoom().addTo(RecipeMaps.assemblerRecipes);
    }

    private static void hollowCasing() {
        GTRecipeBuilder.builder().metadata(RESEARCH_ITEM, GTNGItemList.MolecularCasing.get(1))
            .metadata(SCANNING, new Scanning(1200, TierEU.RECIPE_UV))
            .itemInputs(GTNGItemList.MolecularCasing.get(1),
                GTOreDictUnificator.get(OrePrefixes.screw, Materials.Magnalium, 16), Materials.Europium.getPlates(4),
                GTOreDictUnificator.get(OrePrefixes.stick, Materials.NaquadahAlloy, 4),
                GTOreDictUnificator.get(OrePrefixes.plateDouble, Materials.Uranium, 2),
                GTOreDictUnificator.get(OrePrefixes.screw, Materials.Molybdenum, 16),
                GTOreDictUnificator.get(OrePrefixes.plateDouble, Materials.Duranium, 4))
            .fluidInputs(Materials.StainlessSteel.getMolten(1296), Materials.IndiumGalliumPhosphide.getMolten(1296),
                Materials.WroughtIron.getMolten(1296), Materials.SuperconductorUVBase.getMolten(1296))
            .itemOutputs(GTNGItemList.HollowCasing.get(2)).duration(200).eut(TierEU.RECIPE_UV).addTo(AssemblyLine);
    }

    private static void containmentFieldGenerator() {
        GTRecipeBuilder.builder().metadata(RESEARCH_ITEM, Casings.ForceFieldGlass.toStack(1))
            .metadata(SCANNING, new Scanning(1200, TierEU.RECIPE_UV))
            .itemInputs(GTNGItemList.HollowCasing.get(1), ItemList.Field_Generator_LuV.get(4),
                GTOreDictUnificator.get(OrePrefixes.wireGt04, Materials.SuperconductorUVBase, 6),
                new Object[] { OrePrefixes.circuit.get(Materials.UV), 4 }, ItemList.WetTransformer_UV_ZPM.get(1),
                GTOreDictUnificator.get(OrePrefixes.plateDouble, Materials.VanadiumGallium, 6),
                GTOreDictUnificator.get(OrePrefixes.plateDouble, Materials.SuperconductorUVBase, 4))
            .fluidInputs(Materials.Lanthanum.getMolten(2304), Materials.CobaltBrass.getMolten(5760),
                Materials.BatteryAlloy.getMolten(5760), Materials.Molybdenum.getMolten(1296))
            .itemOutputs(GTNGItemList.ContainmentFieldGenerator.get(1)).duration(500).eut(TierEU.RECIPE_UV)
            .addTo(AssemblyLine);
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
