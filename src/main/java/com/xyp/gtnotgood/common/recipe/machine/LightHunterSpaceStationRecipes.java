// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.recipe.machine;

import static gregtech.api.util.GTRecipeConstants.AssemblyLine;
import static gregtech.api.util.GTRecipeConstants.RESEARCH_ITEM;
import static gregtech.api.util.GTRecipeConstants.SCANNING;

import java.util.Objects;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.common.blocks.lighthunter.LightHunterCasing;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import goodgenerator.util.ItemRefer;
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
import gtPlusPlus.core.material.MaterialMisc;
import gtPlusPlus.core.material.MaterialsAlloy;
import gtPlusPlus.core.material.MaterialsElements;
import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList;
import tectech.thing.CustomItemList;

public final class LightHunterSpaceStationRecipes {

    private LightHunterSpaceStationRecipes() {}

    public static void loadRecipes() {
        rockBreaking();
        casings();
    }

    public static void loadPostInitRecipes() {
        station();
        satelliteFactories();
        astralArray();
        threadModifierHatch();
    }

    private static void station() {
        GTRecipeBuilder.builder().metadata(RESEARCH_ITEM, ItemList.SpaceElevatorController.get(1))
            .metadata(SCANNING, new Scanning(2400, TierEU.RECIPE_UXV))
            .itemInputs(CustomItemList.Machine_Multi_Research.get(16), ItemList.MiningDroneUHV.get(64),
                ItemList.MiningDroneUEV.get(64), ItemList.MiningDroneUIV.get(64), ItemList.MiningDroneUMV.get(64),
                ItemList.MiningDroneUXV.get(64), ItemList.SpaceElevatorModuleMinerT3.get(64),
                stack(OrePrefixes.circuit, Materials.UXV, 32), ItemList.Energy_Cluster.get(64),
                stack(OrePrefixes.plateDense, Materials.Infinity, 16),
                stack(OrePrefixes.plateDouble, Materials.Tritanium, 16), ItemList.Gravistar.get(16))
            .fluidInputs(fluid(MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(1600000)),
                fluid(Materials.UUMatter.getFluid(1600000)), fluid(Materials.TranscendentMetal.getMolten(1600000)))
            .itemOutputs(GTNGItemList.LightHunterSpaceStation.get(1)).duration(2400).eut(TierEU.RECIPE_UXV)
            .addTo(AssemblyLine);
    }

    private static void satelliteFactories() {
        GTRecipeBuilder.builder().metadata(RESEARCH_ITEM, GregtechItemList.Industrial_Mixer.get(1))
            .metadata(SCANNING, new Scanning(1200, TierEU.RECIPE_UXV))
            .itemInputs(GregtechItemList.Industrial_PlatePress.get(16), GregtechItemList.Industrial_WireFactory.get(16),
                ItemList.Machine_Multi_IndustrialElectromagneticSeparator.get(16),
                GregtechItemList.Industrial_Extruder.get(16), GregtechItemList.Industrial_Mixer.get(4),
                stack(OrePrefixes.circuit, Materials.UXV, 32), ItemList.Energy_Cluster.get(1),
                ItemList.Emitter_UXV.get(16), stack(OrePrefixes.plateDouble, Materials.CosmicNeutronium, 16),
                required(MaterialsAlloy.BLACK_TITANIUM.getPlate(32)),
                stack(OrePrefixes.plateDouble, Materials.Infinity, 16), required(MaterialsAlloy.CINOBITE.getPlate(32)))
            .fluidInputs(fluid(MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(16000)),
                fluid(Materials.UUMatter.getFluid(16000)), fluid(Materials.Cerium.getMolten(16000)))
            .itemOutputs(GTNGItemList.NexusSatelliteFactoryMkI.get(1)).duration(1200).eut(TierEU.RECIPE_UXV)
            .addTo(AssemblyLine);

        GTRecipeBuilder.builder().metadata(RESEARCH_ITEM, GregtechItemList.Industrial_Centrifuge.get(1))
            .metadata(SCANNING, new Scanning(1200, TierEU.RECIPE_UXV))
            .itemInputs(ItemList.ChemicalBathUMV.get(64), GregtechItemList.Industrial_Sifter.get(64),
                GregtechItemList.Industrial_Centrifuge.get(64),
                ItemList.Machine_Multi_IndustrialElectromagneticSeparator.get(16),
                GregtechItemList.Controller_Vacuum_Furnace.get(16), GregtechItemList.Industrial_Electrolyzer.get(16),
                stack(OrePrefixes.circuit, Materials.UXV, 32), ItemList.Energy_Cluster.get(1),
                stack(OrePrefixes.plateDouble, Materials.Enderium, 16),
                stack(OrePrefixes.plateDouble, Materials.Osmiridium, 16),
                stack(OrePrefixes.plateDouble, Materials.Trinium, 16),
                stack(OrePrefixes.plateDouble, Materials.Quantium, 16))
            .fluidInputs(fluid(MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(16000)),
                fluid(Materials.UUMatter.getFluid(16000)), fluid(Materials.Neutronium.getMolten(16000)))
            .itemOutputs(GTNGItemList.NexusSatelliteFactoryMkII.get(1)).duration(1200).eut(TierEU.RECIPE_UXV)
            .addTo(AssemblyLine);

        GTRecipeBuilder.builder().metadata(RESEARCH_ITEM, ItemList.SpaceElevatorModulePumpT3.get(1))
            .metadata(SCANNING, new Scanning(1200, TierEU.RECIPE_UXV))
            .itemInputs(ItemList.AutoclaveUMV.get(16), ItemList.Machine_Multi_IndustrialExtractor.get(16),
                GregtechItemList.Machine_Adv_DistillationTower.get(16), ItemList.FluidHeaterUMV.get(16),
                ItemList.Machine_Multi_IndustrialBrewery.get(64), ItemList.Distillation_Tower.get(64),
                stack(OrePrefixes.circuit, Materials.UXV, 32), ItemList.Energy_Cluster.get(1),
                required(MaterialsElements.STANDALONE.CELESTIAL_TUNGSTEN.getPlate(32)),
                required(MaterialsElements.STANDALONE.ASTRAL_TITANIUM.getPlate(32)),
                required(MaterialsElements.getInstance().RHENIUM.getPlate(32)),
                stack(OrePrefixes.plate, Materials.Neutronium, 32))
            .fluidInputs(fluid(MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(16000)),
                fluid(Materials.UUMatter.getFluid(16000)),
                fluid(MaterialsElements.STANDALONE.HYPOGEN.getFluidStack(16000)))
            .itemOutputs(GTNGItemList.NexusSatelliteFactoryMkIII.get(1)).duration(1200).eut(TierEU.RECIPE_UXV)
            .addTo(AssemblyLine);

        GTRecipeBuilder.builder().metadata(RESEARCH_ITEM, ItemRefer.Precise_Assembler.get(1))
            .metadata(SCANNING, new Scanning(1200, TierEU.RECIPE_UXV))
            .itemInputs(ItemList.Machine_Multi_Canner.get(16),
                ItemList.Machine_Multi_IndustrialElectromagneticSeparator.get(16),
                ItemList.AssemblingMachineUMV.get(16), ItemRefer.Precise_Assembler.get(64),
                GregtechItemList.Industrial_Arc_Furnace.get(64), stack(OrePrefixes.circuit, Materials.UXV, 32),
                ItemList.Energy_Cluster.get(1), ItemList.Field_Generator_UXV.get(16),
                required(MaterialsAlloy.HASTELLOY_C276.getPlate(32)),
                stack(OrePrefixes.plateDouble, Materials.DraconiumAwakened, 16),
                required(MaterialsAlloy.HASTELLOY_X.getPlate(32)),
                stack(OrePrefixes.plateDouble, Materials.Infinity, 16))
            .fluidInputs(fluid(MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(16000)),
                fluid(Materials.UUMatter.getFluid(16000)), fluid(Materials.TranscendentMetal.getMolten(16000)))
            .itemOutputs(GTNGItemList.NexusSatelliteFactoryMkIV.get(1)).duration(1200).eut(TierEU.RECIPE_UXV)
            .addTo(AssemblyLine);
    }

    private static void astralArray() {
        GTRecipeBuilder.builder().metadata(RESEARCH_ITEM, GTNGItemList.HeartOfTheUniverse.get(1))
            .metadata(SCANNING, new Scanning(12000, TierEU.RECIPE_MAX))
            .itemInputs(CustomItemList.Machine_Multi_EyeOfHarmony.get(64),
                CustomItemList.Machine_Multi_EyeOfHarmony.get(64), GTNGItemList.AnnihilationConstrainer.get(64),
                GTNGItemList.AnnihilationConstrainer.get(64), Casings.InfiniteSpacetimeEnergyBoundaryCasing.toStack(64),
                Casings.InfiniteSpacetimeEnergyBoundaryCasing.toStack(64),
                Casings.InfiniteSpacetimeEnergyBoundaryCasing.toStack(10),
                Casings.GallifreyanSpacetimeCompressionFieldGenerator.toStack(64),
                Casings.GallifreyanSpacetimeCompressionFieldGenerator.toStack(64),
                Casings.GallifreyanSpacetimeCompressionFieldGenerator.toStack(40),
                GTNGItemList.DimensionalStabilityCasing.get(48), GTNGItemList.DimensionConnectionCasing.get(64),
                GTNGItemList.CreateCasing.get(64), Casings.SpaceTimeBendingCore.toStack(32),
                ItemRefer.AntimatterAnnihilationMatrix.get(16))
            .fluidInputs(fluid(Materials.SpaceTime.getMolten(2097152)), fluid(Materials.Eternity.getMolten(1048576)),
                fluid(Materials.ExcitedDTSC.getFluid(524288)))
            .itemOutputs(GTNGItemList.AstralArray.get(1)).duration(12000).eut(TierEU.RECIPE_MAX).addTo(AssemblyLine);
    }

    private static void rockBreaking() {
        rockRecipe(1, new ItemStack(Blocks.cobblestone), 16);
        rockRecipe(2, new ItemStack(Blocks.stone), 16);
        GTRecipeBuilder.builder().itemInputs(new ItemStack(Items.redstone)).circuit(3)
            .fluidInputs(new FluidStack(FluidRegistry.WATER, 1000), new FluidStack(FluidRegistry.LAVA, 1000))
            .itemOutputs(new ItemStack(Blocks.obsidian)).duration(128).eut(TierEU.RECIPE_LV)
            .addTo(GTNGRecipeMaps.LightHunterRockBreakingRecipes);
    }

    private static void threadModifierHatch() {
        GTRecipeBuilder.builder().metadata(RESEARCH_ITEM, CustomItemList.Machine_Multi_Computer.get(1))
            .metadata(SCANNING, new Scanning(12000, TierEU.RECIPE_MAX))
            .itemInputsUnsafe(amount(4096, ItemRefer.HiC_T5.get(1)), CustomItemList.Machine_Multi_Computer.get(4),
                amount(4096, ItemList.Field_Generator_UXV.get(1)),
                amount(6144, stack(OrePrefixes.frameGt, Materials.Eternity, 1)),
                amount(8192, required(Materials.Neutronium.getNanite(1))),
                amount(8192, required(Materials.Eternity.getNanite(1))),
                amount(8192, required(Materials.BlackDwarfMatter.getNanite(1))),
                amount(8192, required(Materials.WhiteDwarfMatter.getNanite(1))),
                amount(4096, GTNGItemList.RheniumReinforcedEnergyGlass.get(1)),
                amount(131072, stack(OrePrefixes.circuit, Materials.UXV, 1)))
            .fluidInputs(fluid(Materials.RadoxPolymer.getMolten(9203125)), fluid(Materials.SpaceTime.getMolten(720000)),
                fluid(MaterialsElements.STANDALONE.HYPOGEN.getFluidStack(1440000)),
                fluid(Materials.CosmicNeutronium.getMolten(294912000)))
            .itemOutputs(GTNGItemList.SatelliteThreadModifierHatch.get(1)).duration(12000).eut(TierEU.RECIPE_MAX)
            .addTo(AssemblyLine);
    }

    private static void rockRecipe(int circuit, ItemStack output, int duration) {
        GTRecipeBuilder.builder().circuit(circuit)
            .fluidInputs(new FluidStack(FluidRegistry.WATER, 1000), new FluidStack(FluidRegistry.LAVA, 1000))
            .itemOutputs(output).duration(duration).eut(TierEU.RECIPE_LV)
            .addTo(GTNGRecipeMaps.LightHunterRockBreakingRecipes);
    }

    private static void casings() {
        appearance(GTNGItemList.FusionCasingMkIV.get(1), LightHunterCasing.FusionCasingMkIV);
        appearance(Casings.UHVMachineCasing.toStack(1), LightHunterCasing.UhvMachineCasing);
        appearance(ItemList.Hull_UIV.get(1), LightHunterCasing.UivMachineCasing);
        appearance(ItemList.Hull_UXV.get(1), LightHunterCasing.UxvMachineCasing);
        appearance(Casings.ComputerHeatVent.toStack(1), LightHunterCasing.ComputerHeatVent);
        appearance(Casings.HighPowerCasing.toStack(1), LightHunterCasing.HighPowerCasing);
        appearance(Casings.AdvancedComputerCasing.toStack(1), LightHunterCasing.AdvancedComputerCasing);
        appearance(Casings.SingularityReinforcedStellarShieldingCasing.toStack(1),
            LightHunterCasing.UltimateStellarContainmentCasing);
        appearance(ItemList.Energy_Cluster.get(1), LightHunterCasing.UhvUltimateBattery);
        appearance(Casings.ForceFieldGlass.toStack(1), LightHunterCasing.ForceFieldGlass);
        appearance(GTNGItemList.DimensionalBridgeCasing.get(1), LightHunterCasing.DimensionalBridgeCasing);

        GTRecipeBuilder.builder()
            .itemInputs(Casings.AdvancedComputerCasing.toStack(4), ItemList.Circuit_CosmicComputer.get(16),
                ItemList.Field_Generator_UXV.get(4))
            .fluidInputs(fluid(Materials.TranscendentMetal.getMolten(576)))
            .itemOutputs(LightHunterCasing.SuperComputationComponent.getStack(4)).duration(1200).eut(TierEU.RECIPE_UXV)
            .addTo(RecipeMaps.assemblerRecipes);
        GTRecipeBuilder.builder()
            .itemInputs(Casings.DimensionalBridgeGenerator.toStack(1), stack(OrePrefixes.plate, Materials.Infinity, 4),
                stack(OrePrefixes.circuit, Materials.UXV, 1))
            .fluidInputs(fluid(Materials.SpaceTime.getMolten(144)))
            .itemOutputs(LightHunterCasing.ModuleConnector.getStack(1)).duration(1200).eut(TierEU.RECIPE_UXV)
            .addTo(RecipeMaps.assemblerRecipes);
        GTRecipeBuilder.builder()
            .itemInputs(stack(OrePrefixes.frameGt, Materials.Tritanium, 4),
                stack(OrePrefixes.plateDouble, Materials.DraconiumAwakened, 8),
                stack(OrePrefixes.foil, Materials.Infinity, 16))
            .fluidInputs(fluid(Materials.Tritanium.getMolten(576)))
            .itemOutputs(LightHunterCasing.DragonStrengthTritaniumCasing.getStack(4)).duration(1200)
            .eut(TierEU.RECIPE_UXV).addTo(RecipeMaps.assemblerRecipes);
        GTRecipeBuilder.builder()
            .itemInputs(stack(OrePrefixes.frameGt, Materials.Neutronium, 4),
                stack(OrePrefixes.plateDouble, Materials.CosmicNeutronium, 8),
                stack(OrePrefixes.foil, Materials.SpaceTime, 16))
            .fluidInputs(fluid(Materials.Eternity.getMolten(576))).itemOutputs(LightHunterCasing.EchoCasing.getStack(4))
            .duration(1200).eut(TierEU.RECIPE_UXV).addTo(RecipeMaps.assemblerRecipes);
        GTRecipeBuilder.builder()
            .itemInputs(ItemList.Transdimensional_Alignment_Matrix.get(1), ItemList.Field_Generator_UXV.get(4),
                stack(OrePrefixes.frameGt, Materials.Infinity, 1))
            .fluidInputs(fluid(Materials.SpaceTime.getMolten(144))).itemOutputs(LightHunterCasing.HyperCore.getStack(1))
            .duration(1200).eut(TierEU.RECIPE_UXV).addTo(RecipeMaps.assemblerRecipes);
        GTRecipeBuilder.builder()
            .itemInputs(stack(OrePrefixes.frameGt, Materials.Neutronium, 1),
                stack(OrePrefixes.pipeMedium, Materials.Neutronium, 4),
                stack(OrePrefixes.foil, Materials.CosmicNeutronium, 8))
            .fluidInputs(fluid(Materials.Neutronium.getMolten(144)))
            .itemOutputs(LightHunterCasing.NeutroniumPipeCasing.getStack(4)).duration(1200).eut(TierEU.RECIPE_UHV)
            .addTo(RecipeMaps.assemblerRecipes);
        GTRecipeBuilder.builder()
            .itemInputs(Casings.FusionMachineCasingMKIV.toStack(4),
                stack(OrePrefixes.plate, Materials.CosmicNeutronium, 8),
                stack(OrePrefixes.foil, Materials.Infinity, 16))
            .fluidInputs(fluid(Materials.TranscendentMetal.getMolten(576)))
            .itemOutputs(LightHunterCasing.FusionCasingMkV.getStack(4)).duration(1200).eut(TierEU.RECIPE_UXV)
            .addTo(RecipeMaps.assemblerRecipes);
        GTRecipeBuilder.builder()
            .itemInputs(ItemList.Hull_UXV.get(1), stack(OrePrefixes.plate, Materials.ProtoHalkonite, 8))
            .fluidInputs(fluid(Materials.RubberSilicone.getMolten(144)))
            .itemOutputs(LightHunterCasing.UxvHermeticCasing.getStack(1)).duration(1200).eut(TierEU.RECIPE_UXV)
            .addTo(RecipeMaps.assemblerRecipes);
        GTRecipeBuilder.builder()
            .itemInputs(Casings.ContainmentFieldGenerator.toStack(4), stack(OrePrefixes.foil, Materials.SpaceTime, 8),
                stack(OrePrefixes.plate, Materials.Infinity, 4))
            .fluidInputs(fluid(Materials.MHDCSM.getMolten(144)))
            .itemOutputs(LightHunterCasing.RestraintDevice.getStack(4)).duration(1200).eut(TierEU.RECIPE_UXV)
            .addTo(RecipeMaps.assemblerRecipes);
    }

    private static void appearance(ItemStack source, LightHunterCasing target) {
        GTRecipeBuilder.builder().itemInputs(required(source)).circuit(24).itemOutputs(target.getStack(1)).duration(50)
            .eut(16).addTo(RecipeMaps.assemblerRecipes);
        GTRecipeBuilder.builder().itemInputs(target.getStack(1)).circuit(25).itemOutputs(source.copy()).duration(50)
            .eut(16).addTo(RecipeMaps.assemblerRecipes);
    }

    private static ItemStack stack(OrePrefixes prefix, Materials material, int amount) {
        return required(GTOreDictUnificator.get(prefix, material, amount));
    }

    private static ItemStack required(ItemStack stack) {
        return Objects.requireNonNull(stack, "Missing native Light Hunter ingredient");
    }

    private static ItemStack amount(int amount, ItemStack stack) {
        return GTUtility.copyAmountUnsafe(amount, required(stack));
    }

    private static FluidStack fluid(FluidStack stack) {
        return Objects.requireNonNull(stack, "Missing native Light Hunter fluid");
    }
}
