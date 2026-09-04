package com.xyp.gtnotgood.common.utils;

import net.minecraft.item.ItemStack;

import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;

/**
 * Helper for converting mined ore drops into processed ore forms.
 */
public final class OreCrushedUtil {

    private OreCrushedUtil() {}

    /**
     * Finds the macerator's primary output for the supplied ore-like stack.
     *
     * @param item stack to query
     * @return copied primary output, or null when no safe conversion exists
     */
    public static ItemStack getCrushedProduct(ItemStack item) {
        if (item == null || item.getItem() == null) return null;
        try {
            GTRecipe recipe = RecipeMaps.maceratorRecipes.findRecipeQuery()
                .caching(true)
                .items(item)
                .find();
            if (recipe != null && recipe.mOutputs.length > 0 && recipe.mOutputs[0] != null) {
                return recipe.mOutputs[0].copy();
            }
        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * Checks whether a stack is already an ore-processing result.
     *
     * @param item stack to inspect
     * @return true for crushed ores, dusts, and gem forms that should not be converted again
     */
    public static boolean isProcessedForm(ItemStack item) {
        if (item == null || item.getItem() == null) return false;
        ItemData data = GTOreDictUnificator.getItemData(item);
        return data != null && data.mPrefix != null && isProcessedForm(data.mPrefix);
    }

    public static boolean isProcessedForm(OrePrefixes prefix) {
        return prefix == OrePrefixes.crushed || prefix == OrePrefixes.crushedCentrifuged
            || prefix == OrePrefixes.crushedPurified
            || prefix == OrePrefixes.dustImpure
            || prefix == OrePrefixes.dustPure
            || prefix == OrePrefixes.dustRefined
            || prefix == OrePrefixes.dust
            || prefix == OrePrefixes.gem
            || prefix == OrePrefixes.gemChipped
            || prefix == OrePrefixes.gemExquisite
            || prefix == OrePrefixes.gemFlawed
            || prefix == OrePrefixes.gemFlawless;
    }
}
