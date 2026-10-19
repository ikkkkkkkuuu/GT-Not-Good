package com.xyp.gtnotgood.common.recipe.gregtech;

import net.minecraft.item.ItemStack;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.SubTag;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

public final class ExtruderRecipes {

    private ExtruderRecipes() {}

    public static void loadRecipes() {
        for (Materials material : Materials.values()) {
            if (
                !material.mUnifiable || material.mMaterialInto != material
                    || !material.contains(SubTag.CRYSTAL)
                    || material.contains(SubTag.NO_RECIPES)
                    || material.contains(SubTag.NO_ORE_PROCESSING)
                    || material == Materials.Salt
                    || material == Materials.RockSalt
                    || material == Materials.Spodumene
                    || material == Materials.NetherQuartz
            ) continue;

            ItemStack gem = GTOreDictUnificator.get(OrePrefixes.gem, material, 1);
            ItemStack rod = GTOreDictUnificator.get(OrePrefixes.stick, material, 1);
            ItemStack dust = GTOreDictUnificator.get(OrePrefixes.dustSmall, material, 2);
            if (gem == null || rod == null || dust == null) continue;
            if (
                !RecipeMaps.latheRecipes.findRecipeQuery().items(gem).findAll()
                    .anyMatch(recipe -> isStandardGemLatheRecipe(recipe, gem, rod, dust))
            ) continue;
            if (
                RecipeMaps.extruderRecipes.findRecipeQuery()
                    .items(GTUtility.copyAmount(Integer.MAX_VALUE, gem), ItemList.Shape_Extruder_Rod.get(1)).find()
                    != null
            ) continue;

            int voltageMultiplier = material.mBlastFurnaceTemp >= 2800 ? 60 : 15;
            if (material.contains(SubTag.NO_SMASHING)) voltageMultiplier /= 4;
            GTRecipeBuilder.builder().itemInputs(gem, ItemList.Shape_Extruder_Rod.get(0))
                .itemOutputs(GTUtility.copyAmount(2, rod)).duration(Math.max(material.getMass() * 2, 1))
                .eut(GTUtility.calculateRecipeEU(material, 6 * voltageMultiplier)).addTo(RecipeMaps.extruderRecipes);
        }
    }

    /** Only supplement the native one-unit route; special gem ratios keep their existing recipes. */
    private static boolean isStandardGemLatheRecipe(GTRecipe recipe, ItemStack gem, ItemStack rod, ItemStack dust) {
        if (
            recipe.mInputs.length != 1 || recipe.mOutputs.length != 2
                || recipe.mFluidInputs.length != 0
                || recipe.mFluidOutputs.length != 0
                || !sameAmount(recipe.mInputs[0], gem)
                || recipe.getOutputChance(0) != 10000
                || recipe.getOutputChance(1) != 10000
        ) return false;
        return (sameAmount(recipe.mOutputs[0], rod) && sameAmount(recipe.mOutputs[1], dust))
            || (sameAmount(recipe.mOutputs[1], rod) && sameAmount(recipe.mOutputs[0], dust));
    }

    private static boolean sameAmount(ItemStack actual, ItemStack expected) {
        return actual != null && actual.stackSize == expected.stackSize && GTUtility.areStacksEqual(actual, expected);
    }
}
