package com.xyp.gtnotgood.common.recipe.machine;

import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.loader.ItemsLoader;

import appeng.api.AEApi;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Registers the crafting-table recipe for the AE2 wildcard pattern item.
 */
public final class WildcardPatternRecipes {

    private WildcardPatternRecipes() {}

    /**
     * Adds the shapeless upgrade from an AE2 blank pattern to the wildcard pattern.
     */
    public static void loadRecipes() {
        GameRegistry.addRecipe(
            new ItemStack(ItemsLoader.wildcardPattern),
            "B",
            'B',
            AEApi.instance()
                .definitions()
                .materials()
                .blankPattern()
                .maybeStack(1)
                .orNull());
    }
}
