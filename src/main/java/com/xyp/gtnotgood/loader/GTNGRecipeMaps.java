package com.xyp.gtnotgood.loader;

import com.xyp.gtnotgood.client.nei.ChemicalPlantFrontend;
import com.xyp.gtnotgood.client.nei.HeartGenerationFrontend;
import com.xyp.gtnotgood.client.nei.TransmutationFrontend;
import com.xyp.gtnotgood.common.recipe.SeedConversionBackend;
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

    public static final RecipeMap<RecipeMapBackend> LightHunterRockBreakingRecipes = RecipeMapBuilder
        // #tr recipe.gtnotgood.light_hunter_rock_breaking
        // # Satellite Rock Breaking
        // # zh_CN 卫星岩石粉碎
        .of("recipe.gtnotgood.light_hunter_rock_breaking").maxIO(2, 1, 2, 0).progressBar(GTUITextures.PROGRESSBAR_ARROW)
        .neiHandlerInfo(builder -> builder.setDisplayStack(GTNGItemList.NexusSatelliteFactoryMkII.get(1))).build();

    public static final RecipeMap<RecipeMapBackend> HeartGenerationRecipes = RecipeMapBuilder
        // #tr recipe.gtnotgood.heart_generation
        // # Genesis Generation
        // # zh_CN 创世发电
        .of("recipe.gtnotgood.heart_generation").maxIO(2, 1, 0, 0).progressBar(GTUITextures.PROGRESSBAR_ARROW)
        .frontend(HeartGenerationFrontend::new).neiHandlerInfo(builder -> builder
            .setDisplayStack(GTNGItemList.HeartOfTheUniverse.get(1)).setHeight(202).setMaxRecipesPerPage(1))
        .build();

    public static final RecipeMap<RecipeMapBackend> LightningProcessorRecipes = RecipeMapBuilder
        // #tr recipe.gtnotgood.lightning_processor
        // # Lightning Processing
        // # zh_CN 闪电处理
        .of("recipe.gtnotgood.lightning_processor").maxIO(6, 1, 6, 1)
        .progressBar(GTUITextures.PROGRESSBAR_ARROW_MULTIPLE)
        .neiHandlerInfo(builder -> builder.setDisplayStack(GTNGItemList.SuperconductingElectromagneticFactory.get(1)))
        .build();

    public static final RecipeMap<RecipeMapBackend> DimensionalFocusRecipes = RecipeMapBuilder
        // #tr recipe.gtnotgood.dimensional_focus
        // # Dimensional Focus Engraving
        // # zh_CN 维度聚焦激光蚀刻
        .of("recipe.gtnotgood.dimensional_focus").maxIO(4, 4, 3, 0).progressBar(GTUITextures.PROGRESSBAR_ARROW_MULTIPLE)
        .neiHandlerInfo(builder -> builder.setDisplayStack(GTNGItemList.DimensionalFocusEngravingArray.get(1))).build();

    public static final RecipeMap<RecipeMapBackend> SuperParticleColliderRecipes = RecipeMapBuilder
        // #tr recipe.gtnotgood.particle_collider
        // # Particle Collision
        // # zh_CN 粒子对撞
        .of("recipe.gtnotgood.particle_collider").maxIO(0, 0, 2, 1).progressBar(GTUITextures.PROGRESSBAR_ARROW_MULTIPLE)
        .neiHandlerInfo(builder -> builder.setDisplayStack(GTNGItemList.SuperParticleCollider.get(1))).build();

    public static final RecipeMap<SeedConversionBackend> SeedConversionRecipes = RecipeMapBuilder
        // #tr recipe.gtnotgood.seedConversion
        // # Seed Conversion
        // # zh_CN 种子转化
        .of("recipe.gtnotgood.seedConversion", SeedConversionBackend::new).maxIO(1, 1, 0, 0)
        .progressBar(GTUITextures.PROGRESSBAR_ARROW)
        .neiHandlerInfo(builder -> builder.setDisplayStack(GTNGItemList.SeedConverter.get(1))).build();

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
