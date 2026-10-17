package com.xyp.gtnotgood.common.recipe.machine;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTModHandler;

public final class WebScreenRecipes {

    private WebScreenRecipes() {}

    public static void loadRecipes() {
        GTModHandler.addCraftingRecipe(GTNGItemList.WebScreen.get(1),
            new Object[] { "PLP", "CGC", "PBP", 'P', OrePrefixes.plate.get(Materials.Steel), 'L',
                new ItemStack(Items.glowstone_dust), 'C', OrePrefixes.cableGt01.get(Materials.Copper), 'G',
                new ItemStack(Blocks.glass_pane), 'B', OrePrefixes.circuit.get(Materials.LV) });
    }
}
