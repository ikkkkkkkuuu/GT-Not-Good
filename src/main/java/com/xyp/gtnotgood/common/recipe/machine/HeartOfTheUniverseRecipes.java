// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.recipe.machine;

import static gregtech.api.util.GTRecipeConstants.AssemblyLine;
import static gregtech.api.util.GTRecipeConstants.RESEARCH_ITEM;
import static gregtech.api.util.GTRecipeConstants.SCANNING;

import java.util.Objects;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.common.blocks.heart.HeartCasing;
import com.xyp.gtnotgood.common.machines.processing.HeartGenerationRules;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import goodgenerator.api.recipe.GoodGeneratorRecipeMaps;
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
import gtPlusPlus.core.material.MaterialsElements;

public final class HeartOfTheUniverseRecipes {

    private HeartOfTheUniverseRecipes() {}

    public static void loadRecipes() {
        generationRecipe(GTNGItemList.StrangeAnnihilationFuelRod.get(1), GTNGItemList.BlackHoleSeed.get(1),
            GTNGItemList.AnnihilationConstrainer.get(64)).addTo(GTNGRecipeMaps.HeartGenerationRecipes);
        fuelRod();
        casings();
    }

    static GTRecipeBuilder generationRecipe(ItemStack fuelRod, ItemStack seed, ItemStack constrainers) {
        return GTRecipeBuilder.builder().itemInputs(fuelRod, seed).itemOutputs(constrainers).outputChances(6000)
            .duration(HeartGenerationRules.BASE_DURATION).eut(0);
    }

    private static void fuelRod() {
        GTRecipeBuilder.builder()
            .itemInputs(required(Materials.Antimatter.getCells(64)), required(Materials.Antimatter.getCells(64)),
                required(Materials.Antimatter.getCells(64)), Casings.SpaceTimeContinuumRipper.toStack(64))
            .fluidInputs(fluid(Materials.CosmicNeutronium.getMolten(1440)), fluid(Materials.MHDCSM.getMolten(1600)),
                fluid(Materials.SpaceTime.getMolten(5760)), fluid(Materials.MoltenProtoHalkoniteBase.getFluid(1440)))
            .itemOutputs(GTNGItemList.StrangeAnnihilationFuelRod.get(1)).duration(12000).eut(TierEU.RECIPE_MAX)
            .specialValue(4).addTo(GoodGeneratorRecipeMaps.preciseAssemblerRecipes);
    }

    public static void loadPostInitRecipes() {
        blackHoleSeed();
        controller();
    }

    private static void blackHoleSeed() {
        GTRecipeBuilder.builder().metadata(RESEARCH_ITEM, ItemRefer.Compact_Fusion_MK5.get(1))
            .metadata(SCANNING, new Scanning(11520, TierEU.RECIPE_UXV))
            .itemInputs(ItemList.Emitter_UXV.get(16), ItemRefer.Compact_Fusion_MK5.get(16),
                ItemRefer.Compact_Fusion_MK5.get(16), ItemList.Emitter_UXV.get(16),
                stack(OrePrefixes.plateDouble, Materials.ProtoHalkonite, 8),
                Casings.SpaceTimeContinuumRipper.toStack(64), Casings.SpaceTimeContinuumRipper.toStack(64),
                stack(OrePrefixes.plateDouble, Materials.ProtoHalkonite, 8),
                stack(OrePrefixes.plateDouble, Materials.ProtoHalkonite, 8),
                Casings.SpaceTimeContinuumRipper.toStack(64), Casings.SpaceTimeContinuumRipper.toStack(64),
                stack(OrePrefixes.plateDouble, Materials.ProtoHalkonite, 8), ItemList.Emitter_UXV.get(16),
                ItemRefer.Compact_Fusion_MK5.get(16), ItemRefer.Compact_Fusion_MK5.get(16),
                ItemList.Emitter_UXV.get(16))
            .fluidInputs(fluid(Materials.Eternity.getMolten(92160)), fluid(Materials.WhiteDwarfMatter.getMolten(92160)),
                fluid(Materials.Tartarite.getMolten(92160)), fluid(Materials.BlackDwarfMatter.getMolten(92160)))
            .itemOutputs(GTNGItemList.BlackHoleSeed.get(1)).duration(11520).eut(TierEU.RECIPE_MAX).addTo(AssemblyLine);
    }

    private static void controller() {
        GTRecipeBuilder.builder().metadata(RESEARCH_ITEM, ItemRefer.AntimatterGenerator.get(1))
            .metadata(SCANNING, new Scanning(2304000, TierEU.RECIPE_MAX))
            .itemInputsUnsafe(amount(128, ItemRefer.AntimatterGenerator.get(1)),
                amount(256, ItemList.DysonSwarmController.get(1)), amount(256, ItemRefer.Compact_Fusion_MK5.get(1)),
                amount(128, ItemRefer.AntimatterGenerator.get(1)), amount(1024, ItemList.BatteryHull_UEV_Full.get(1)),
                amount(1024, ItemList.BatteryHull_UIV_Full.get(1)), amount(1024, ItemList.BatteryHull_UMV_Full.get(1)),
                amount(1024, ItemList.BatteryHull_UxV_Full.get(1)), GTNGItemList.BlackHoleSeed.get(64),
                GTNGItemList.BlackHoleSeed.get(64), GTNGItemList.BlackHoleSeed.get(64),
                ItemList.Transdimensional_Alignment_Matrix.get(16), ItemList.Circuit_TranscendentMainframe.get(16),
                ItemList.BatteryHull_UxV_Full.get(16))
            .fluidInputs(fluid(Materials.Eternity.getMolten(331776000)),
                fluid(MaterialsElements.STANDALONE.HYPOGEN.getFluidStack(754974720)),
                fluid(Materials.TranscendentMetal.getMolten(460800000)), fluid(Materials.UUMatter.getFluid(16384000)))
            .itemOutputs(GTNGItemList.HeartOfTheUniverse.get(1)).duration(2304000).eut(TierEU.RECIPE_MAX)
            .addTo(AssemblyLine);
    }

    private static void casings() {
        appearance(Casings.UltimateContainmentFieldGenerator.toStack(1), HeartCasing.GravitonFieldConstraintCasing);
        appearance(ItemRefer.AntimatterAnnihilationMatrix.get(1), HeartCasing.AnnihilateCore);
        appearance(Casings.CosmicFabricShieldingCore.toStack(1), HeartCasing.DimensionalStabilityCasing);
        appearance(Casings.SpacetimeAlteringCasing.toStack(1), HeartCasing.TemporalAnchorFieldCasing);
        appearance(Casings.DimensionalBridgeGenerator.toStack(1), HeartCasing.DimensionConnectionCasing);
        appearance(Casings.InfiniteSpacetimeEnergyBoundaryCasing.toStack(1), HeartCasing.CreateCasing);
        appearance(ItemList.DysonSwarmDeploymentUnitMagnet.get(1), HeartCasing.DysonDeploymentMagnet);
        GTRecipeBuilder.builder()
            .itemInputs(GTNGItemList.FusionGlass.get(4), required(MaterialsElements.getInstance().RHENIUM.getPlate(16)),
                stack(OrePrefixes.foil, Materials.TranscendentMetal, 16))
            .fluidInputs(fluid(Materials.SpaceTime.getMolten(576)))
            .itemOutputs(HeartCasing.RheniumReinforcedEnergyGlass.getStack(4)).duration(1200).eut(TierEU.RECIPE_UHV)
            .addTo(RecipeMaps.assemblerRecipes);
    }

    private static void appearance(ItemStack nativeCasing, HeartCasing casing) {
        GTRecipeBuilder.builder().itemInputs(required(nativeCasing), GTUtility.getIntegratedCircuit(24))
            .itemOutputs(casing.getStack(1)).duration(50).eut(16).addTo(RecipeMaps.assemblerRecipes);
        GTRecipeBuilder.builder().itemInputs(casing.getStack(1), GTUtility.getIntegratedCircuit(25))
            .itemOutputs(nativeCasing.copy()).duration(50).eut(16).addTo(RecipeMaps.assemblerRecipes);
    }

    private static ItemStack stack(OrePrefixes prefix, Materials material, int amount) {
        return required(GTOreDictUnificator.get(prefix, material, amount));
    }

    private static ItemStack required(ItemStack stack) {
        return Objects.requireNonNull(stack, "Missing native Heart of the Universe ingredient");
    }

    private static ItemStack amount(int amount, ItemStack stack) {
        return GTUtility.copyAmountUnsafe(amount, required(stack));
    }

    private static FluidStack fluid(FluidStack stack) {
        return Objects.requireNonNull(stack, "Missing native Heart of the Universe fluid");
    }
}
