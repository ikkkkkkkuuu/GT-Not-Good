package com.xyp.gtnotgood.common.recipe.machine;

import static gregtech.api.enums.TierEU.RECIPE_LV;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;

/** Registers the ME container at the same assembler tier as this addon's existing ME bridges. */
public final class MEContainerRecipes {

    private MEContainerRecipes() {}

    public static void loadRecipes() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                GTModHandler.getModItem(ModList.AE2.getID(), "tile.BlockInterface", 1),
                new ItemStack(Blocks.chest),
                new ItemStack(Blocks.glass, 4),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.LV, 1))
            .itemOutputs(GTNGItemList.MEContainer.get(1))
            .eut(RECIPE_LV)
            .duration(15 * SECONDS)
            .addTo(RecipeMaps.assemblerRecipes);
    }
}
