package com.xyp.gtnotgood.common.recipe.gregtech;

import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

public class FurnaceRecipes {

    public static void loadRecipes() {
        GTModHandler.addSmeltingRecipe(new ItemStack(Blocks.sand), new ItemStack(Blocks.glass));

        // 铁锭烧锻铁锭 (Iron → WroughtIron)
        GTModHandler.addSmeltingRecipe(
            GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Iron, 1L),
            GTOreDictUnificator.get(OrePrefixes.ingot, Materials.WroughtIron, 1L));

        // 铜锭烧退火铜锭 (Copper → AnnealedCopper)
        GTModHandler.addSmeltingRecipe(
            GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Copper, 1L),
            GTOreDictUnificator.get(OrePrefixes.ingot, Materials.AnnealedCopper, 1L));

    }
}
