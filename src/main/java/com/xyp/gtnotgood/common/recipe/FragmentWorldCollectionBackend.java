package com.xyp.gtnotgood.common.recipe;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBackendPropertiesBuilder;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;

public final class FragmentWorldCollectionBackend extends RecipeMapBackend {

    public FragmentWorldCollectionBackend(RecipeMapBackendPropertiesBuilder properties) {
        super(properties);
    }

    @Override
    protected boolean filterFindRecipe(GTRecipe recipe, ItemStack[] items, FluidStack[] fluids, ItemStack specialSlot,
        boolean dontCheckStackSizes) {
        if (!super.filterFindRecipe(recipe, items, fluids, specialSlot, dontCheckStackSizes)) return false;
        // A drill must select fluid collection even when an ore recipe uses the same circuit.
        for (ItemStack supplied : items) {
            if (supplied == null) continue;
            boolean matched = false;
            for (ItemStack required : recipe.mInputs) {
                if (GTUtility.areStacksEqual(supplied, required)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) return false;
        }
        return true;
    }
}
