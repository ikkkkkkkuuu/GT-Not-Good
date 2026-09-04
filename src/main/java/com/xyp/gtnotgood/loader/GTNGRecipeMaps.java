package com.xyp.gtnotgood.loader;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import gregtech.api.gui.modularui.GTUITextures;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.recipe.RecipeCategory;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBuilder;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.maps.AssemblyLineFrontend;
import gregtech.api.util.GTRecipe;

/**
 * Defines custom GregTech recipe maps owned by GT Not Good.
 * <p>
 * Recipe maps are static registries in GregTech. Keep custom maps here so machines, recipe loaders, and NEI display
 * handlers all reference the same object instead of constructing duplicate maps.
 */
public final class GTNGRecipeMaps {

    private static boolean assemblyFactoryRecipesPopulated;

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

    /** Independent Assembly Line recipe pool used by the Assembly Factory's second GUI mode. */
    public static final RecipeMap<RecipeMapBackend> AssemblyFactoryAssemblyLineRecipes = RecipeMapBuilder
        // #tr recipe.gtnotgood.assemblyFactoryAssemblyLine
        // # Assembly Factory - Assembly Line
        // # zh_CN 装配线加工
        .of("recipe.gtnotgood.assemblyFactoryAssemblyLine")
        .maxIO(16, 1, 4, 0)
        .minInputs(1, 0)
        .useSpecialSlot()
        .slotOverlays((index, isFluid, isOutput, isSpecial) -> isSpecial ? GTUITextures.OVERLAY_SLOT_DATA_ORB : null)
        .slotOverlaysMUI2(
            (index, isFluid, isOutput, isSpecial) -> isSpecial ? GTGuiTextures.OVERLAY_SLOT_DATA_ORB : null)
        .neiTransferRect(88, 8, 18, 72)
        .neiTransferRect(124, 8, 18, 72)
        .neiTransferRect(142, 26, 18, 18)
        .frontend(AssemblyLineFrontend::new)
        .neiHandlerInfo(
            builder -> builder.setHeight(110)
                .setDisplayStack(GTNGItemList.AssemblyFactory.get(1)))
        .build();

    /**
     * Copies the completed visual Assembly Line registry into the factory's executable private recipe map.
     */
    public static synchronized void populateAssemblyFactoryAssemblyLineRecipes() {
        if (assemblyFactoryRecipesPopulated) return;
        RecipeCategory category = AssemblyFactoryAssemblyLineRecipes.getDefaultRecipeCategory();
        for (GTRecipe recipe : RecipeMaps.assemblylineVisualRecipes.getAllRecipes()) {
            GTRecipe copy = recipe.copy();
            copy.setRecipeCategory(category);
            AssemblyFactoryAssemblyLineRecipes.addRecipe(copy, false, false, false);
        }
        assemblyFactoryRecipesPopulated = true;
    }

}
