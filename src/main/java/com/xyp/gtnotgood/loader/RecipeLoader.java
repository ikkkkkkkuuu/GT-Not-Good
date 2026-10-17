package com.xyp.gtnotgood.loader;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.common.blocks.beekeeping.WorkingApiaryRegistration;
import com.xyp.gtnotgood.common.recipe.gregtech.AssemblerRecipes;
import com.xyp.gtnotgood.common.recipe.gregtech.BenderRecipes;
import com.xyp.gtnotgood.common.recipe.gregtech.CentrifugeRecipes;
import com.xyp.gtnotgood.common.recipe.gregtech.CraftingTableRecipes;
import com.xyp.gtnotgood.common.recipe.gregtech.FuelRodRecipes;
import com.xyp.gtnotgood.common.recipe.gregtech.FurnaceRecipes;
import com.xyp.gtnotgood.common.recipe.gregtech.MixerRecipes;
import com.xyp.gtnotgood.common.recipe.gtnotgood.AdvancedIORecipes;
import com.xyp.gtnotgood.common.recipe.gtnotgood.FuelRefiningRecipes;
import com.xyp.gtnotgood.common.recipe.gtnotgood.OreProcessingRecipes;
import com.xyp.gtnotgood.common.recipe.gtnotgood.PackagedRecipes;
import com.xyp.gtnotgood.common.recipe.gtnotgood.PatternSorterRecipes;
import com.xyp.gtnotgood.common.recipe.gtnotgood.PetrochemicalRecipes;
import com.xyp.gtnotgood.common.recipe.gtnotgood.WoodDistillationRecipes;
import com.xyp.gtnotgood.common.recipe.machine.CrossRecipeWirelessEnergyHatchRecipes;
import com.xyp.gtnotgood.common.recipe.machine.FluxConnectorRecipes;
import com.xyp.gtnotgood.common.recipe.machine.IntegratedChemicalPlantRecipes;
import com.xyp.gtnotgood.common.recipe.machine.IntegratedProductionFactoryRecipes;
import com.xyp.gtnotgood.common.recipe.machine.LargeBeeBreederRecipes;
import com.xyp.gtnotgood.common.recipe.machine.LargeCombProcessorRecipes;
import com.xyp.gtnotgood.common.recipe.machine.LargeCropBreederRecipes;
import com.xyp.gtnotgood.common.recipe.machine.LargeIndustrialApiaryRecipes;
import com.xyp.gtnotgood.common.recipe.machine.LargeIndustrialFarmRecipes;
import com.xyp.gtnotgood.common.recipe.machine.LargeInterfaceRecipes;
import com.xyp.gtnotgood.common.recipe.machine.LargeTransmutationMachineRecipes;
import com.xyp.gtnotgood.common.recipe.machine.LargeVoidMinerRecipes;
import com.xyp.gtnotgood.common.recipe.machine.MEBridgeRecipes;
import com.xyp.gtnotgood.common.recipe.machine.MEContainerRecipes;
import com.xyp.gtnotgood.common.recipe.machine.MEDataAccessRecipes;
import com.xyp.gtnotgood.common.recipe.machine.MaxCapacityMEOutputRecipes;
import com.xyp.gtnotgood.common.recipe.machine.MechanicalUserRecipes;
import com.xyp.gtnotgood.common.recipe.machine.NetworkRecipes;
import com.xyp.gtnotgood.common.recipe.machine.SeedConverterRecipes;
import com.xyp.gtnotgood.common.recipe.machine.SingularityDataHubRecipes;
import com.xyp.gtnotgood.common.recipe.machine.StockIOInterfaceRecipes;
import com.xyp.gtnotgood.common.recipe.machine.SuperCraftingInputRecipes;
import com.xyp.gtnotgood.common.recipe.machine.WebScreenRecipes;
import com.xyp.gtnotgood.common.recipe.machine.WildcardPatternRecipes;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;
import thaumcraft.common.config.ConfigBlocks;

public class RecipeLoader {

    /** Waits for external fluid and dimension-display registrations. */
    public static void loadPostInitRecipes() {
        CentrifugeRecipes.loadRecipes();
        LargeIndustrialFarmRecipes.loadRecipes();
        MixerRecipes.loadRecipes();
        FuelRefiningRecipes.loadRecipes();
        WoodDistillationRecipes.loadRecipes();
        PetrochemicalRecipes.loadRecipes();
    }

    public static void loadRecipes() {
        GameRegistry.addShapelessRecipe(GTNGItemList.UniversalFluidPump.get(1), ItemList.Pump_LV.get(1),
            ItemList.Electric_Pump_LV.get(1));
        if (ModList.Forestry.isModLoaded()) {
            WorkingApiaryRegistration.registerRecipe();
            LargeIndustrialApiaryRecipes.loadRecipes();
        }
        GameRegistry.addShapelessRecipe(GTNGItemList.StructureCompass.get(1), Items.compass, Items.map, Items.brick);
        if (ModList.ThaumicEnergistics.isModLoaded() && ModList.Thaumcraft.isModLoaded()) {
            GameRegistry.addShapelessRecipe(GTNGItemList.EssentiaDisassembler.get(1),
                ItemList.Machine_HV_Extractor.get(1),
                AEApi.instance().definitions().blocks().iface().maybeStack(1).get(),
                new ItemStack(ConfigBlocks.blockStoneDevice, 1, 0));
        }
        LargeTransmutationMachineRecipes.loadRecipes();
        IntegratedChemicalPlantRecipes.loadRecipes();
        AdvancedIORecipes.register();
        StockIOInterfaceRecipes.register();
        LargeInterfaceRecipes.register();
        PackagedRecipes.register();
        MechanicalUserRecipes.loadRecipes();
        WebScreenRecipes.loadRecipes();
        FluxConnectorRecipes.loadRecipes();
        IntegratedProductionFactoryRecipes.loadRecipes();
        AssemblerRecipes.loadRecipes();
        CrossRecipeWirelessEnergyHatchRecipes.loadRecipes();
        BenderRecipes.loadRecipes();
        FurnaceRecipes.loadRecipes();
        FuelRodRecipes.loadRecipes();
        SingularityDataHubRecipes.loadRecipes();
        CraftingTableRecipes.loadRecipes();
        SeedConverterRecipes.loadRecipes();
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
        LargeCombProcessorRecipes.loadRecipes();
        LargeCropBreederRecipes.loadRecipes();
        NetworkRecipes.loadRecipes();
    }
}
