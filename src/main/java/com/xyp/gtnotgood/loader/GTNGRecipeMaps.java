package com.xyp.gtnotgood.loader;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.gui.modularui.GTUITextures;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBuilder;

/**
 * Defines custom GregTech recipe maps owned by GT Not Good.
 * <p>
 * Recipe maps are static registries in GregTech. Keep custom maps here so machines, recipe loaders, and NEI display
 * handlers all reference the same object instead of constructing duplicate maps.
 */
public final class GTNGRecipeMaps {

    /** Item and fluid recovery through the standard GT processing pipeline and NEI. */
    public static final RecipeMap<RecipeMapBackend> TransmutationRecipes = RecipeMapBuilder
        // #tr recipe.gtnotgood.transmutation
        // # Transmutation Disassembly
        // # zh_CN 嬗变拆解
        .of("recipe.gtnotgood.transmutation")
        .maxIO(1, 16, 0, 4)
        .progressBar(GTUITextures.PROGRESSBAR_ARROW_MULTIPLE)
        .progressBarPos(56, 35)
        .logoPos(24, 86)
        .neiRecipeBackgroundSize(170, 110)
        .frontend(com.xyp.gtnotgood.client.nei.TransmutationFrontend::new)
        .neiHandlerInfo(
            builder -> builder.setDisplayStack(GTNGItemList.LargeTransmutationMachine.get(1))
                .setHeight(174)
                .setMaxRecipesPerPage(1))
        .build();

    /**
     * Recipe map consumed by the Large Ore Processor.
     * <p>
     * The NEI handler display stack points at {@link GTNGItemList#LargeOreProcessor}; this makes the custom recipe page
     * show the controller item instead of a generic or missing icon once machine registration has assigned the stack.
     */
    public static final RecipeMap<RecipeMapBackend> OreProcessingRecipes = RecipeMapBuilder
        // #tr recipe.gtnotgood.oreProcessing
        // # Ore Processing
        // # zh_CN 矿石处理
        .of("recipe.gtnotgood.oreProcessing")
        .maxIO(1, 9, 0, 0)
        .progressBar(GTUITextures.PROGRESSBAR_ARROW_MULTIPLE)
        .neiHandlerInfo(builder -> builder.setDisplayStack(GTNGItemList.LargeOreProcessor.get(1)))
        .build();
}
