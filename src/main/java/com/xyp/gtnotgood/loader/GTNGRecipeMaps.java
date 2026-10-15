package com.xyp.gtnotgood.loader;

import com.xyp.gtnotgood.client.nei.ChemicalPlantFrontend;
import com.xyp.gtnotgood.client.nei.TransmutationFrontend;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.gui.modularui.GTUITextures;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBuilder;
import gregtech.nei.formatter.HeatingCoilSpecialValueFormatter;

/**
 * Defines custom GregTech recipe maps owned by GT Not Good.
 * <p>
 * Recipe maps are static registries in GregTech. Keep custom maps here so machines, recipe loaders, and NEI display
 * handlers all reference the same object instead of constructing duplicate maps.
 */
public final class GTNGRecipeMaps {

    public static final RecipeMap<RecipeMapBackend> FuelRefiningRecipes = RecipeMapBuilder
        // #tr recipe.gtnotgood.fuel_refining
        // # Integrated Fuel Refining
        // # zh_CN 集成燃料精炼
        .of("recipe.gtnotgood.fuel_refining").maxIO(3, 0, 8, 1).progressBar(GTUITextures.PROGRESSBAR_ARROW_MULTIPLE)
        .progressBarPos(68, 35).logoPos(68, 86).neiRecipeBackgroundSize(170, 110).frontend(ChemicalPlantFrontend::new)
        .neiSpecialInfoFormatter(HeatingCoilSpecialValueFormatter.INSTANCE).neiHandlerInfo(builder -> builder
            .setDisplayStack(GTNGItemList.IntegratedChemicalPlant.get(1)).setHeight(210).setMaxRecipesPerPage(1))
        .build();

    public static final RecipeMap<RecipeMapBackend> WoodDistillationRecipes = RecipeMapBuilder
        // #tr recipe.gtnotgood.wood_distillation
        // # Integrated Wood Distillation
        // # zh_CN 集成木化
        .of("recipe.gtnotgood.wood_distillation").maxIO(1, 1, 1, 16)
        .progressBar(GTUITextures.PROGRESSBAR_ARROW_MULTIPLE).progressBarPos(68, 35).logoPos(68, 86)
        .neiRecipeBackgroundSize(170, 110).frontend(ChemicalPlantFrontend::new).neiHandlerInfo(builder -> builder
            .setDisplayStack(GTNGItemList.IntegratedChemicalPlant.get(1)).setHeight(174).setMaxRecipesPerPage(1))
        .build();

    public static final RecipeMap<RecipeMapBackend> PetrochemicalRecipes = RecipeMapBuilder
        // #tr recipe.gtnotgood.petrochemical
        // # Integrated Petrochemical Processing
        // # zh_CN 集成石化
        .of("recipe.gtnotgood.petrochemical").maxIO(1, 0, 2, 12).progressBar(GTUITextures.PROGRESSBAR_ARROW_MULTIPLE)
        .progressBarPos(68, 35).logoPos(68, 86).neiRecipeBackgroundSize(170, 110).frontend(ChemicalPlantFrontend::new)
        .neiHandlerInfo(builder -> builder.setDisplayStack(GTNGItemList.IntegratedChemicalPlant.get(1)).setHeight(174)
            .setMaxRecipesPerPage(1))
        .build();

    /** Comb conversions imported from completed GregTech recipe registries. */
    public static final RecipeMap<RecipeMapBackend> CombProcessingRecipes = RecipeMapBuilder
        // #tr recipe.gtnotgood.combProcessing
        // # Comb Processing
        // # zh_CN 蜂窝处理
        .of("recipe.gtnotgood.combProcessing").maxIO(9, 9, 0, 3).progressBar(GTUITextures.PROGRESSBAR_ARROW_MULTIPLE)
        .neiHandlerInfo(builder -> builder.setDisplayStack(GTNGItemList.LargeCombProcessor.get(1))).build();

    /** Item and fluid recovery through the standard GT processing pipeline and NEI. */
    public static final RecipeMap<RecipeMapBackend> TransmutationRecipes = RecipeMapBuilder
        // #tr recipe.gtnotgood.transmutation
        // # Transmutation Disassembly
        // # zh_CN 嬗变拆解
        .of("recipe.gtnotgood.transmutation").maxIO(1, 16, 0, 4).progressBar(GTUITextures.PROGRESSBAR_ARROW_MULTIPLE)
        .progressBarPos(56, 35).logoPos(24, 86).neiRecipeBackgroundSize(170, 110).frontend(TransmutationFrontend::new)
        .neiHandlerInfo(builder -> builder.setDisplayStack(GTNGItemList.LargeTransmutationMachine.get(1)).setHeight(174)
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
        .of("recipe.gtnotgood.oreProcessing").maxIO(1, 9, 0, 0).progressBar(GTUITextures.PROGRESSBAR_ARROW_MULTIPLE)
        .neiHandlerInfo(builder -> builder.setDisplayStack(GTNGItemList.LargeOreProcessor.get(1))).build();
}
