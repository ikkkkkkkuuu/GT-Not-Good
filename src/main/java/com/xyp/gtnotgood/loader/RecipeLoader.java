package com.xyp.gtnotgood.loader;

import com.xyp.gtnotgood.common.recipe.gregtech.BenderRecipes;
import com.xyp.gtnotgood.common.recipe.gregtech.CraftingTableRecipes;
import com.xyp.gtnotgood.common.recipe.gregtech.FurnaceRecipes;
import com.xyp.gtnotgood.common.recipe.gtnotgood.OreProcessingRecipes;
import com.xyp.gtnotgood.common.recipe.machine.AssemblyFactoryRecipes;
import com.xyp.gtnotgood.common.recipe.machine.DimensionallyTranscendentPlasmaFusionComputerRecipes;
import com.xyp.gtnotgood.common.recipe.machine.LargeBeeBreederRecipes;
import com.xyp.gtnotgood.common.recipe.machine.LargeCropBreederRecipes;
import com.xyp.gtnotgood.common.recipe.machine.LargeVoidMinerRecipes;
import com.xyp.gtnotgood.common.recipe.machine.MEBridgeRecipes;
import com.xyp.gtnotgood.common.recipe.machine.NetworkRecipes;
import com.xyp.gtnotgood.common.recipe.machine.SingularityDataHubRecipes;
import com.xyp.gtnotgood.common.recipe.machine.SuperCraftingInputRecipes;
import com.xyp.gtnotgood.common.recipe.machine.TorcherinoRecipes;
import com.xyp.gtnotgood.common.recipe.machine.WildcardPatternRecipes;

/**
 * Dispatches recipe registration for this mod during Forge initialization.
 * <p>
 * Keep recipe family loaders behind this class so proxy lifecycle code only needs one recipe entry point, matching the
 * loader layout used by GT-Not-Cool.
 */
public class RecipeLoader {

    /**
     * Registers all recipes owned by GT Not Good.
     * <p>
     * This currently delegates to the Large Ore Processor recipe generator. Future recipe families should be added here
     * as additional one-line loader calls.
     */
    public static void loadRecipes() {
        com.xyp.gtnotgood.common.recipe.machine.IntegratedProductionFactoryRecipes.loadRecipes();
        BenderRecipes.loadRecipes();
        FurnaceRecipes.loadRecipes();
        SingularityDataHubRecipes.loadRecipes();
        CraftingTableRecipes.loadRecipes();
        DimensionallyTranscendentPlasmaFusionComputerRecipes.loadRecipes();
        AssemblyFactoryRecipes.loadRecipes();
        OreProcessingRecipes.loadOreProcessingRecipes();
        MEBridgeRecipes.loadRecipes();
        WildcardPatternRecipes.loadRecipes();
        TorcherinoRecipes.loadRecipes();
        SuperCraftingInputRecipes.loadRecipes();
        LargeVoidMinerRecipes.loadRecipes();
        LargeBeeBreederRecipes.loadRecipes();
        LargeCropBreederRecipes.loadRecipes();
        NetworkRecipes.loadRecipes();
    }
}
