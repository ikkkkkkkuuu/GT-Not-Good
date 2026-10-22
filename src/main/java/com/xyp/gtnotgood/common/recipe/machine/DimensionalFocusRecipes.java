// SPDX-License-Identifier: CC-BY-NC-SA-4.0
package com.xyp.gtnotgood.common.recipe.machine;

import static gregtech.api.util.GTRecipeConstants.AssemblyLine;
import static gregtech.api.util.GTRecipeConstants.RESEARCH_ITEM;
import static gregtech.api.util.GTRecipeConstants.SCANNING;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.oredict.OreDictionary;

import com.xyp.gtnotgood.loader.GTNGRecipeMaps;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.casing.Casings;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.objects.OreDictItemStack;
import gregtech.api.objects.SubstituteFluidStack;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.RecipeMetadataKey;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipe.GTRecipe_WithAlt;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import gregtech.api.util.recipe.Scanning;
import gtPlusPlus.core.material.MaterialsAlloy;
import tectech.thing.CustomItemList;

public final class DimensionalFocusRecipes {

    private static boolean imported;
    private static int importedRecipes;

    private DimensionalFocusRecipes() {}

    public static void loadRecipes() {
        controller();
        dimensionInjectionCasing();
        dimensionallyTranscendentCasing();
        molecularCoil();
        improvedSuperconductorCoil();
        laserSafeCasing();
        fusionGlass();
        photoresists();
    }

    public static void importNativeRecipes() {
        if (imported) return;
        imported = true;
        List<GTRecipe> recipes = new ArrayList<>();
        for (GTRecipe source : new ArrayList<>(RecipeMaps.laserEngraverRecipes.getAllRecipes())) {
            GTRecipe recipe = convert(source);
            if (recipe == null) continue;
            GTNGRecipeMaps.DimensionalFocusRecipes.getBackend().compileRecipe(recipe);
            recipes.add(recipe);
        }
        importedRecipes = recipes.size();
        DimensionalFocusResearch.registerRecipes(recipes);
    }

    public static int getImportedRecipeCount() {
        return importedRecipes;
    }

    public static GTRecipe convert(GTRecipe source) {
        int eut = DimensionalFocusRecipeRules.energy(source.mEUt);
        if (
            eut == 0 || source.mDuration <= 0
                || !source.mEnabled
                || source.mFakeRecipe
                || source.mFluidOutputs.length != 0
                || source.mOutputs.length == 0
                || source.mOutputs[0] == null
        ) return null;
        GTRecipeBuilder builder = GTRecipeBuilder.builder().itemInputs(itemInputs(source))
            .itemOutputs(copyItems(source.mOutputs)).fluidInputs(fluidInputs(source))
            .duration(DimensionalFocusRecipeRules.duration(source.mDuration)).eut(eut).special(source.mSpecialItems)
            .specialValue(source.mSpecialValue);
        if (source.mInputChances != null) builder.inputChances(source.mInputChances.clone());
        if (source.mOutputChances != null) builder.outputChances(source.mOutputChances.clone());
        if (source.mFluidInputChances != null) {
            int[] chances = Arrays.copyOf(source.mFluidInputChances, source.mFluidInputs.length + 1);
            chances[chances.length - 1] = 10000;
            builder.fluidInputChances(chances);
        }
        if (source.mHidden) builder.hidden();
        if (!source.mCanBeBuffered) builder.noBuffer();
        if (source.mNeedsEmptyOutput) builder.needsEmptyOutput();
        if (source.isNBTSensitive) builder.nbtSensitive();
        for (Map.Entry<RecipeMetadataKey<?>, Object> entry : source.getMetadataStorage().getEntries())
            copyMetadata(builder, entry.getKey(), entry.getValue());
        long computation = DimensionalFocusRecipeRules.computation(source.mEUt);
        builder.metadata(DimensionalFocusResearch.ORIGINAL_EUT, source.mEUt)
            .metadata(DimensionalFocusResearch.COMPUTATION, computation);
        if (
            computation > 0
        ) builder.metadata(DimensionalFocusResearch.RESEARCH, DimensionalFocusResearch.fingerprint(source.mOutputs[0]));
        GTRecipe result = builder.buildWithAlt().orElse(null);
        if (result != null) {
            List<String> description = new ArrayList<>();
            if (source.getNeiDesc() != null) description.addAll(Arrays.asList(source.getNeiDesc()));
            if (computation > 0) {
                // #tr gtng.dimensional_focus.recipe.computation
                // # Computation: %s CWU/t per parallel
                // # zh_CN 每并行需要 %s CWU/t 算力
                description.add(
                    StatCollector.translateToLocalFormatted("gtng.dimensional_focus.recipe.computation", computation));
                // #tr gtng.dimensional_focus.recipe.research
                // # Research: scan one output item with a Data Stick
                // # zh_CN 研究：扫描一件产物，将结果写入数据存储介质
                description.add(StatCollector.translateToLocal("gtng.dimensional_focus.recipe.research"));
                // #tr gtng.dimensional_focus.recipe.scanning
                // # Scan: 120 s at %s EU/t
                // # zh_CN 扫描耗时：120秒，耗电：%s EU/t
                description.add(
                    StatCollector.translateToLocalFormatted("gtng.dimensional_focus.recipe.scanning", source.mEUt));
            }
            if (!description.isEmpty()) result.setNeiDesc(description.toArray(new String[0]));
        }
        return result;
    }

    private static <T> void copyMetadata(GTRecipeBuilder builder, RecipeMetadataKey<T> key, Object value) {
        builder.metadata(key, key.cast(value));
    }

    private static Object[] itemInputs(GTRecipe source) {
        Object[] inputs = new Object[source.mInputs.length];
        for (int index = 0; index < inputs.length; index++) {
            ItemStack stack = source.mInputs[index];
            if (source instanceof GTRecipe_WithAlt alternatives) {
                if (
                    alternatives.mOreDictIds != null && index < alternatives.mOreDictIds.length
                        && alternatives.mOreDictIds[index] >= 0
                ) {
                    inputs[index] = new OreDictItemStack(OreDictionary.getOreName(alternatives.mOreDictIds[index]),
                        stack.stackSize);
                    continue;
                }
                if (
                    alternatives.mOreDictAlt != null && index < alternatives.mOreDictAlt.length
                        && alternatives.mOreDictAlt[index] != null
                        && alternatives.mOreDictAlt[index].length > 0
                ) {
                    inputs[index] = copyItems(alternatives.mOreDictAlt[index]);
                    continue;
                }
            }
            inputs[index] = stack.copy();
        }
        return inputs;
    }

    private static Object[] fluidInputs(GTRecipe source) {
        Object[] inputs = new Object[source.mFluidInputs.length + 1];
        for (int index = 0; index < source.mFluidInputs.length; index++) {
            if (
                source.mAltFluidInputs != null && index < source.mAltFluidInputs.length
                    && source.mAltFluidInputs[index] != null
                    && source.mAltFluidInputs[index].length > 0
            ) {
                inputs[index] = new SubstituteFluidStack(source.mAltFluidInputs[index]);
            } else {
                inputs[index] = source.mFluidInputs[index].copy();
            }
        }
        int amount = DimensionalFocusRecipeRules.photoresistAmount(source.mEUt);
        inputs[inputs.length - 1] = DimensionalFocusRecipeRules.usesEuv(source.mEUt)
            ? DimensionalFocusFluids.euvPhotoresist(amount)
            : DimensionalFocusFluids.photoresist(amount);
        return inputs;
    }

    private static ItemStack[] copyItems(ItemStack[] stacks) {
        ItemStack[] result = new ItemStack[stacks.length];
        for (int index = 0; index < stacks.length; index++)
            result[index] = stacks[index] == null ? null : stacks[index].copy();
        return result;
    }

    private static void controller() {
        GTRecipeBuilder.builder().metadata(RESEARCH_ITEM, ItemList.Machine_Multi_IndustrialLaserEngraver.get(1))
            .metadata(SCANNING, new Scanning(2400, TierEU.RECIPE_UEV))
            .itemInputs(ItemList.PrecisionLaserEngraverUHV.get(8), Materials.Neutronium.getNanite(16),
                CustomItemList.LASERpipe.get(64), ItemList.Field_Generator_UEV.get(8), ItemList.Emitter_UEV.get(4),
                new Object[] { OrePrefixes.circuit.get(Materials.UIV), 16 },
                GTOreDictUnificator.get(OrePrefixes.stick, Materials.Neutronium, 16),
                GTOreDictUnificator.get(OrePrefixes.plateDouble, Materials.Duranium, 32))
            .fluidInputs(MaterialsAlloy.INDALLOY_140.getFluidStack(28800), Materials.Polybenzimidazole.getMolten(28800),
                MaterialsAlloy.PIKYONIUM.getFluidStack(4608), MaterialsAlloy.ABYSSAL.getFluidStack(4608))
            .itemOutputs(GTNGItemList.DimensionalFocusEngravingArray.get(1)).duration(1600).eut(TierEU.RECIPE_UEV)
            .addTo(AssemblyLine);
    }

    private static void dimensionInjectionCasing() {
        GTRecipeBuilder.builder().metadata(RESEARCH_ITEM, Casings.FusionMachineCasingMKIII.toStack(1))
            .metadata(SCANNING, new Scanning(2400, TierEU.RECIPE_UEV))
            .itemInputs(Materials.Osmiridium.getPlates(6), Materials.Lead.getPlates(4),
                GTOreDictUnificator.get(OrePrefixes.bolt, Materials.Adamantium, 12),
                new Object[] { OrePrefixes.circuit.get(Materials.IV), 2 },
                GTOreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorUVBase, 2),
                ItemList.Super_Tank_EV.get(1), ItemList.Super_Chest_EV.get(1))
            .fluidInputs(Materials.Duranium.getMolten(200), Materials.VibrantAlloy.getMolten(288),
                Materials.AstralSilver.getMolten(288), Materials.NaquadahEnriched.getMolten(288))
            .itemOutputs(GTNGItemList.DimensionInjectionCasing.get(2)).duration(400).eut(TierEU.RECIPE_UEV)
            .addTo(AssemblyLine);
    }

    private static void dimensionallyTranscendentCasing() {
        GTRecipeBuilder.builder()
            .itemInputs(GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Mithril, 1),
                GTNGItemList.DimensionInjectionCasing.get(2), GTNGItemList.MolecularCasing.get(2),
                ItemList.Quantum_Chest_IV.get(1), ItemList.Quantum_Tank_IV.get(1),
                GTOreDictUnificator.get(OrePrefixes.bolt, Materials.Adamantium, 12),
                GTOreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorUVBase, 1),
                Materials.Palladium.getPlates(6), Materials.Osmiridium.getPlates(6))
            .fluidInputs(Materials.Duranium.getMolten(200))
            .itemOutputs(GTNGItemList.DimensionallyTranscendentCasing.get(2)).duration(200).eut(TierEU.RECIPE_UIV)
            .addTo(RecipeMaps.assemblerRecipes);
    }

    private static void molecularCoil() {
        GTRecipeBuilder.builder().metadata(RESEARCH_ITEM, Casings.NaquadahCoilBlock.toStack(1))
            .metadata(SCANNING, new Scanning(2400, TierEU.RECIPE_UV))
            .itemInputs(GTNGItemList.HollowCasing.get(1), Casings.NaquadahCoilBlock.toStack(2),
                Casings.FusionCoilBlock.toStack(2),
                GTOreDictUnificator.get(OrePrefixes.wireFine, Materials.Europium, 64),
                GTOreDictUnificator.get(OrePrefixes.foil, Materials.SuperconductorUVBase, 32))
            .fluidInputs(Materials.BorosilicateGlass.getMolten(2304), Materials.RubberSilicone.getMolten(5760),
                Materials.Platinum.getMolten(1296), MaterialsAlloy.STELLITE.getFluidStack(1296))
            .itemOutputs(GTNGItemList.MolecularCoil.get(2)).duration(400).eut(TierEU.RECIPE_UV).addTo(AssemblyLine);
    }

    private static void improvedSuperconductorCoil() {
        GTRecipeBuilder.builder()
            .itemInputs(Casings.LuVSolenoidSuperconductorCoil.toStack(1), Materials.NiobiumTitanium.getPlates(2),
                Materials.BorosilicateGlass.getDust(8), new Object[] { OrePrefixes.circuit.get(Materials.LuV), 1 })
            .fluidInputs(Materials.Europium.getMolten(144)).itemOutputs(GTNGItemList.ImprovedSuperconductorCoil.get(1))
            .duration(200).eut(TierEU.RECIPE_LuV).requiresCleanRoom().addTo(RecipeMaps.assemblerRecipes);
    }

    private static void laserSafeCasing() {
        GTRecipeBuilder.builder()
            .itemInputs(Casings.HighPowerCasing.toStack(1), Materials.TungstenCarbide.getPlates(6),
                ItemList.Emitter_IV.get(1), GTUtility.getIntegratedCircuit(6))
            .fluidInputs(Materials.Osmiridium.getMolten(288)).itemOutputs(GTNGItemList.LaserSafeEngravingCasing.get(2))
            .duration(200).eut(TierEU.RECIPE_IV).addTo(RecipeMaps.assemblerRecipes);
    }

    private static void fusionGlass() {
        GTRecipeBuilder.builder()
            .itemInputs(Casings.ForceFieldGlass.toStack(4),
                GTOreDictUnificator.get(OrePrefixes.frameGt, Materials.Neutronium, 2), ItemList.Emitter_UV.get(1),
                GTUtility.getIntegratedCircuit(6))
            .fluidInputs(Materials.Polybenzimidazole.getMolten(576)).itemOutputs(GTNGItemList.FusionGlass.get(4))
            .duration(200).eut(TierEU.RECIPE_UV).requiresCleanRoom().addTo(RecipeMaps.assemblerRecipes);
    }

    private static void photoresists() {
        GTRecipeBuilder.builder().itemInputs(Materials.Carbon.getDust(1), GTUtility.getIntegratedCircuit(4))
            .fluidInputs(Materials.Phenol.getFluid(1000), Materials.Acetone.getFluid(1000))
            .fluidOutputs(DimensionalFocusFluids.photoresist(2000)).duration(400).eut(TierEU.RECIPE_EV)
            .addTo(RecipeMaps.multiblockChemicalReactorRecipes);
        GTRecipeBuilder.builder().itemInputs(Materials.Osmium.getDust(1), GTUtility.getIntegratedCircuit(5))
            .fluidInputs(DimensionalFocusFluids.photoresist(1000), Materials.HydrochloricAcid.getFluid(1000))
            .fluidOutputs(DimensionalFocusFluids.euvPhotoresist(1000)).duration(600).eut(TierEU.RECIPE_LuV)
            .addTo(RecipeMaps.multiblockChemicalReactorRecipes);
    }
}
