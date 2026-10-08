package com.xyp.gtnotgood.ae2thing.nei.recipes;

import com.xyp.gtnotgood.ae2thing.integration.Mods;
import com.xyp.gtnotgood.ae2thing.nei.recipes.extractor.GT5RecipeExtractor;
import com.xyp.gtnotgood.ae2thing.nei.recipes.extractor.ThaumcraftRecipeExtractor;
import com.xyp.gtnotgood.ae2thing.nei.recipes.extractor.VanillaRecipeExtractor;

import gregtech.api.recipe.RecipeCategory;

public class DefaultExtractorLoader implements Runnable {

    @Override
    public void run() {
        FluidRecipe.addRecipeMap("smelting", new VanillaRecipeExtractor(false));
        FluidRecipe.addRecipeMap("brewing", new VanillaRecipeExtractor(false));
        FluidRecipe.addRecipeMap("crafting", new VanillaRecipeExtractor(true));
        FluidRecipe.addRecipeMap("crafting2x2", new VanillaRecipeExtractor(true));
        ThaumcraftRecipeExtractor.register();
        if (Mods.isGt5UnofficialLoaded() || Mods.isLegacyGt5Loaded()) {
            for (RecipeCategory category : RecipeCategory.ALL_RECIPE_CATEGORIES.values()) {
                FluidRecipe.addRecipeMap(category.unlocalizedName,
                    new GT5RecipeExtractor(category.recipeMap.unlocalizedName.equals("gt.recipe.scanner")
                        || category.recipeMap.unlocalizedName.equals("gt.recipe.fakeAssemblylineProcess")));
            }
        }
    }
}
