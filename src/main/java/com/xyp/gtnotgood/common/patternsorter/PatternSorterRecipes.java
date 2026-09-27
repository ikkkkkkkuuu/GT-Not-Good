package com.xyp.gtnotgood.common.patternsorter;

import net.minecraft.init.Items;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import appeng.api.AEApi;
import cpw.mods.fml.common.registry.GameRegistry;

/** Early AE utility recipe; requires a blank pattern and never consumes an encoded recipe. */
public final class PatternSorterRecipes {

    private PatternSorterRecipes() {}

    public static void register() {
        net.minecraft.item.ItemStack pattern = AEApi.instance()
            .definitions()
            .materials()
            .blankPattern()
            .maybeStack(1)
            .orNull();
        if (pattern != null)
            GameRegistry.addShapelessRecipe(GTNGItemList.PatternSorter.get(1), pattern, Items.compass, Items.paper);
    }
}
