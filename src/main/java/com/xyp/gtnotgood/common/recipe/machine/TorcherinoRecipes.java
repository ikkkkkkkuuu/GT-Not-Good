package com.xyp.gtnotgood.common.recipe.machine;

import static gregtech.api.enums.TierEU.RECIPE_MV;
import static gregtech.api.enums.TierEU.RECIPE_UEV;
import static gregtech.api.enums.TierEU.RECIPE_UHV;
import static gregtech.api.enums.TierEU.RECIPE_ZPM;
import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.recipe.RecipeMaps.compressorRecipes;
import static gregtech.api.recipe.RecipeMaps.neutroniumCompressorRecipes;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;
import static gregtech.api.util.GTRecipeConstants.COMPRESSION_TIER;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.GTValues;
import gregtech.api.util.GTUtility;

/**
 * Registers crafting and GregTech machine recipes for GT Not Good's Torcherino block family.
 */
public final class TorcherinoRecipes {

    private TorcherinoRecipes() {}

    /**
     * Adds the ordinary Torcherino, compressed Torcherino, and optional wireless Torcherino recipes.
     */
    public static void loadRecipes() {
        addCraftingTableRecipes();
        addGregTechRecipes();
    }

    private static void addCraftingTableRecipes() {
        GameRegistry
            .addShapedRecipe(GTNGItemList.Torcherino.get(1), " C ", "CTC", " C ", 'C', Items.clock, 'T', Blocks.torch);

        GameRegistry.addShapedRecipe(
            GTNGItemList.CompressedTorcherino.get(1),
            "TTT",
            "TTT",
            "TTT",
            'T',
            GTNGItemList.Torcherino.get(1));

        GameRegistry.addShapedRecipe(
            GTNGItemList.DoubleCompressedTorcherino.get(1),
            "TTT",
            "TTT",
            "TTT",
            'T',
            GTNGItemList.CompressedTorcherino.get(1));
    }

    private static void addGregTechRecipes() {
        GTValues.RA.stdBuilder()
            .itemInputs(new ItemStack(Blocks.torch), new ItemStack(Items.clock, 4))
            .circuit(4)
            .itemOutputs(GTNGItemList.Torcherino.get(1))
            .duration(10 * SECONDS)
            .eut(RECIPE_MV)
            .addTo(assemblerRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(GTNGItemList.Torcherino.get(9))
            .itemOutputs(GTNGItemList.CompressedTorcherino.get(1))
            .duration(10 * SECONDS)
            .eut(RECIPE_MV)
            .addTo(neutroniumCompressorRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(GTNGItemList.CompressedTorcherino.get(9))
            .itemOutputs(GTNGItemList.DoubleCompressedTorcherino.get(1))
            .duration(10 * SECONDS)
            .eut(RECIPE_MV)
            .addTo(neutroniumCompressorRecipes);

        if (!Config.enableWirelessTorcherino) return;

        GTValues.RA.stdBuilder()
            .itemInputsUnsafe(GTUtility.copyAmountUnsafe(256, GTNGItemList.Torcherino.get(1)))
            .itemOutputs(GTNGItemList.WirelessTorcherino.get(1))
            .metadata(COMPRESSION_TIER, 1)
            .duration(30 * SECONDS)
            .eut(RECIPE_ZPM)
            .addTo(compressorRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(GTNGItemList.WirelessTorcherino.get(9))
            .itemOutputs(GTNGItemList.CompressedWirelessTorcherino.get(1))
            .metadata(COMPRESSION_TIER, 1)
            .duration(30 * SECONDS)
            .eut(RECIPE_UHV)
            .addTo(compressorRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(GTNGItemList.CompressedWirelessTorcherino.get(9))
            .itemOutputs(GTNGItemList.DoubleCompressedWirelessTorcherino.get(1))
            .metadata(COMPRESSION_TIER, 1)
            .duration(30 * SECONDS)
            .eut(RECIPE_UEV)
            .addTo(compressorRecipes);
    }
}
