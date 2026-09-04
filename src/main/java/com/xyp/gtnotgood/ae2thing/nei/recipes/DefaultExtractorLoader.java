package com.xyp.gtnotgood.ae2thing.nei.recipes;

import com.xyp.gtnotgood.ae2thing.integration.Mods;
import com.xyp.gtnotgood.ae2thing.nei.recipes.extractor.GT5RecipeExtractor;
import com.xyp.gtnotgood.ae2thing.nei.recipes.extractor.VanillaRecipeExtractor;

import gregtech.api.recipe.RecipeMap;

public class DefaultExtractorLoader implements Runnable {

    @Override
    public void run() {
        FluidRecipe.addRecipeMap("smelting", new VanillaRecipeExtractor(false));
        FluidRecipe.addRecipeMap("brewing", new VanillaRecipeExtractor(false));
        FluidRecipe.addRecipeMap("crafting", new VanillaRecipeExtractor(true));
        FluidRecipe.addRecipeMap("crafting2x2", new VanillaRecipeExtractor(true));
        if (Mods.isGt5UnofficialLoaded() || Mods.isLegacyGt5Loaded()) {
            for (RecipeMap<?> recipeMap : RecipeMap.ALL_RECIPE_MAPS.values()) {
                FluidRecipe.addRecipeMap(
                    recipeMap.unlocalizedName,
                    new GT5RecipeExtractor(
                        recipeMap.unlocalizedName.equals("gt.recipe.scanner")
                            || recipeMap.unlocalizedName.equals("gt.recipe.fakeAssemblylineProcess")));
            }
        }
    }
}
