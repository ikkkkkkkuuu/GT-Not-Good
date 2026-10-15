package com.xyp.gtnotgood.common.recipe;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.cropsnh.api.ICropCard;
import com.gtnewhorizon.cropsnh.farming.registries.CropRegistry;

import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBackendPropertiesBuilder;
import gregtech.api.util.GTRecipe;

public final class SeedConversionBackend extends RecipeMapBackend {

    public SeedConversionBackend(RecipeMapBackendPropertiesBuilder propertiesBuilder) {
        super(propertiesBuilder);
    }

    @Override
    public boolean containsInput(ItemStack item) {
        return item != null && item.getItem() != null
            && CropRegistry.instance.fromAlternateSeed(item) != null
            && super.containsInput(item);
    }

    @Override
    protected boolean filterFindRecipe(GTRecipe recipe, ItemStack[] items, FluidStack[] fluids, ItemStack specialSlot,
        boolean dontCheckStackSizes) {
        if (recipe.mOutputs.length == 0) return false;
        ICropCard outputCrop = CropRegistry.instance.get(recipe.mOutputs[0], false);
        if (outputCrop == null) return false;
        // Ore dictionary equivalence must not change CropsNH's item and metadata mapping.
        for (ItemStack item : items) {
            if (item != null && item.getItem() != null && CropRegistry.instance.fromAlternateSeed(item) == outputCrop)
                return super.filterFindRecipe(recipe, items, fluids, specialSlot, dontCheckStackSizes);
        }
        return false;
    }
}
