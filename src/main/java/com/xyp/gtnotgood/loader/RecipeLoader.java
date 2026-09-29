package com.xyp.gtnotgood.loader;

import com.xyp.gtnotgood.common.advancedio.AdvancedIORecipes;
import com.xyp.gtnotgood.common.beekeeping.WorkingApiaryRegistration;
import com.xyp.gtnotgood.common.packaged.PackagedRecipes;
import com.xyp.gtnotgood.common.patternsorter.PatternSorterRecipes;
import com.xyp.gtnotgood.common.recipe.gregtech.BenderRecipes;
import com.xyp.gtnotgood.common.recipe.gregtech.CraftingTableRecipes;
import com.xyp.gtnotgood.common.recipe.gregtech.FuelRodRecipes;
import com.xyp.gtnotgood.common.recipe.gregtech.FurnaceRecipes;
import com.xyp.gtnotgood.common.recipe.gtnotgood.OreProcessingRecipes;
import com.xyp.gtnotgood.common.recipe.machine.FluxConnectorRecipes;
import com.xyp.gtnotgood.common.recipe.machine.IntegratedProductionFactoryRecipes;
import com.xyp.gtnotgood.common.recipe.machine.LargeBeeBreederRecipes;
import com.xyp.gtnotgood.common.recipe.machine.LargeCropBreederRecipes;
import com.xyp.gtnotgood.common.recipe.machine.LargeTransmutationMachineRecipes;
import com.xyp.gtnotgood.common.recipe.machine.LargeVoidMinerRecipes;
import com.xyp.gtnotgood.common.recipe.machine.MEBridgeRecipes;
import com.xyp.gtnotgood.common.recipe.machine.MEContainerRecipes;
import com.xyp.gtnotgood.common.recipe.machine.MEDataAccessRecipes;
import com.xyp.gtnotgood.common.recipe.machine.MaxCapacityMEOutputRecipes;
import com.xyp.gtnotgood.common.recipe.machine.MechanicalUserRecipes;
import com.xyp.gtnotgood.common.recipe.machine.NetworkRecipes;
import com.xyp.gtnotgood.common.recipe.machine.SingularityDataHubRecipes;
import com.xyp.gtnotgood.common.recipe.machine.SuperCraftingInputRecipes;
import com.xyp.gtnotgood.common.recipe.machine.WildcardPatternRecipes;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;

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
        GameRegistry.addShapelessRecipe(
            GTNGItemList.UniversalFluidPump.get(1),
            ItemList.Pump_HV.get(1),
            ItemList.Electric_Pump_HV.get(1));
        if (ModList.Forestry.isModLoaded()) {
            WorkingApiaryRegistration.registerRecipe();
        }
        cpw.mods.fml.common.registry.GameRegistry.addShapelessRecipe(
            GTNGItemList.StructureCompass.get(1),
            net.minecraft.init.Items.compass,
            net.minecraft.init.Items.map,
            net.minecraft.init.Items.brick);
        if (ModList.ThaumicEnergistics.isModLoaded() && ModList.Thaumcraft.isModLoaded()) {
            GameRegistry.addShapelessRecipe(
                GTNGItemList.EssentiaDisassembler.get(1),
                ItemList.Machine_HV_Extractor.get(1),
                appeng.api.AEApi.instance()
                    .definitions()
                    .blocks()
                    .iface()
                    .maybeStack(1)
                    .get(),
                new net.minecraft.item.ItemStack(thaumcraft.common.config.ConfigBlocks.blockStoneDevice, 1, 0));
        }
        LargeTransmutationMachineRecipes.loadRecipes();
        AdvancedIORecipes.register();
        PackagedRecipes.register();
        MechanicalUserRecipes.loadRecipes();
        FluxConnectorRecipes.loadRecipes();
        IntegratedProductionFactoryRecipes.loadRecipes();
        BenderRecipes.loadRecipes();
        FurnaceRecipes.loadRecipes();
        FuelRodRecipes.loadRecipes();
        SingularityDataHubRecipes.loadRecipes();
        CraftingTableRecipes.loadRecipes();
        OreProcessingRecipes.loadOreProcessingRecipes();
        MEBridgeRecipes.loadRecipes();
        MEContainerRecipes.loadRecipes();
        MEDataAccessRecipes.loadRecipes();
        MaxCapacityMEOutputRecipes.loadRecipes();
        WildcardPatternRecipes.loadRecipes();
        PatternSorterRecipes.register();
        SuperCraftingInputRecipes.loadRecipes();
        LargeVoidMinerRecipes.loadRecipes();
        LargeBeeBreederRecipes.loadRecipes();
        com.xyp.gtnotgood.common.recipe.machine.LargeCombProcessorRecipes.loadRecipes();
        LargeCropBreederRecipes.loadRecipes();
        NetworkRecipes.loadRecipes();
    }
}
