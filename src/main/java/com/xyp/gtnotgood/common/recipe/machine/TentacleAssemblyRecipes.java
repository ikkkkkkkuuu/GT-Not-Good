package com.xyp.gtnotgood.common.recipe.machine;

import java.util.ArrayList;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.loader.GTNGRecipeMaps;
import com.xyp.gtnotgood.utils.enums.ModsItemlist;

import bartworks.API.recipe.BartWorksRecipeMaps;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Mods;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipe.GTRecipe_WithAlt;
import gregtech.api.util.GTRecipe.RecipeAssemblyLine;
import gregtech.api.util.GTUtility;

public final class TentacleAssemblyRecipes {

    private static boolean imported;

    private TentacleAssemblyRecipes() {}

    public static void scheduleImport() {
        if (GregTechAPI.sGTCompleteLoad == null) importNativeRecipes();
        else GregTechAPI.sGTCompleteLoad.add(TentacleAssemblyRecipes::importNativeRecipes);
    }

    public static void importNativeRecipes() {
        if (imported) return;
        for (RecipeAssemblyLine source : new ArrayList<>(RecipeAssemblyLine.sAssemblylineRecipes)) {
            GTRecipe recipe = convert(source);
            if (recipe == null) continue;
            GTNGRecipeMaps.TentacleAssemblyRecipes.getBackend().compileRecipe(recipe);
        }
        for (GTRecipe source : BartWorksRecipeMaps.circuitAssemblyLineRecipes.getAllRecipes()) {
            GTRecipe recipe = source.copy();
            recipe.mSpecialItems = null;
            recipe.setRecipeCategory(GTNGRecipeMaps.TentacleCircuitAssemblyRecipes.getDefaultRecipeCategory());
            GTNGRecipeMaps.TentacleCircuitAssemblyRecipes.getBackend().compileRecipe(recipe);
            // The native pool has already applied the configured recipe speed.
            recipe.mDuration = source.mDuration;
        }
        imported = true;
    }

    public static GTRecipe convert(RecipeAssemblyLine source) {
        if (source.mEUt <= 0 || source.mDuration <= 0 || source.mOutput == null || source.mInputs.length == 0)
            return null;
        ItemStack[] items = copyItems(source.mInputs);
        // Expanding the Shuttle's six ore-variant groups creates billions of lookup paths.
        boolean fixedInputs = Mods.GalacticraftAmunRa.isModLoaded()
            && GTUtility.areStacksEqual(source.mOutput, ModsItemlist.ShuttleSchematic.get(1), true);
        ItemStack[][] alternatives = new ItemStack[items.length][];
        for (int i = 0; i < alternatives.length; i++) {
            if (
                !fixedInputs && source.mOreDictAlt != null
                    && i < source.mOreDictAlt.length
                    && source.mOreDictAlt[i] != null
            ) alternatives[i] = copyItems(source.mOreDictAlt[i]);
        }
        FluidStack[] fluids = new FluidStack[source.mFluidInputs.length];
        for (int i = 0; i < fluids.length; i++)
            if (source.mFluidInputs[i] != null) fluids[i] = source.mFluidInputs[i].copy();
        return new GTRecipe_WithAlt(false, items, new ItemStack[] { source.mOutput.copy() }, null, null, null, null,
            null, fluids, new FluidStack[0], source.mDuration, source.mEUt, 0, alternatives, null);
    }

    private static ItemStack[] copyItems(ItemStack[] source) {
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < copy.length; i++) if (source[i] != null) copy[i] = source[i].copy();
        return copy;
    }
}
