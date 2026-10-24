package com.xyp.gtnotgood.loader;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.common.blocks.NativeStructureCasings;
import com.xyp.gtnotgood.common.blocks.beekeeping.WorkingApiaryRegistration;
import com.xyp.gtnotgood.common.recipe.gregtech.AssemblerRecipes;
import com.xyp.gtnotgood.common.recipe.gregtech.BenderRecipes;
import com.xyp.gtnotgood.common.recipe.gregtech.CentrifugeRecipes;
import com.xyp.gtnotgood.common.recipe.gregtech.CraftingTableRecipes;
import com.xyp.gtnotgood.common.recipe.gregtech.FormingPressRecipes;
import com.xyp.gtnotgood.common.recipe.gregtech.FuelRodRecipes;
import com.xyp.gtnotgood.common.recipe.gregtech.FurnaceRecipes;
import com.xyp.gtnotgood.common.recipe.gregtech.LargeChemicalReactorRecipes;
import com.xyp.gtnotgood.common.recipe.gregtech.MixerRecipes;
import com.xyp.gtnotgood.common.recipe.gtnotgood.AdvancedIORecipes;
import com.xyp.gtnotgood.common.recipe.gtnotgood.FuelRefiningRecipes;
import com.xyp.gtnotgood.common.recipe.gtnotgood.MultiblockTerminalRecipes;
import com.xyp.gtnotgood.common.recipe.gtnotgood.OreProcessingRecipes;
import com.xyp.gtnotgood.common.recipe.gtnotgood.PackagedRecipes;
import com.xyp.gtnotgood.common.recipe.gtnotgood.PatternSorterRecipes;
import com.xyp.gtnotgood.common.recipe.gtnotgood.PetrochemicalRecipes;
import com.xyp.gtnotgood.common.recipe.gtnotgood.PetrochemicalSynthesisRecipes;
import com.xyp.gtnotgood.common.recipe.gtnotgood.RareEarthProcessingRecipes;
import com.xyp.gtnotgood.common.recipe.gtnotgood.WoodDistillationRecipes;
import com.xyp.gtnotgood.common.recipe.machine.AssemblyPlantCasingRecipes;
import com.xyp.gtnotgood.common.recipe.machine.BasicIndustrialApiaryRecipes;
import com.xyp.gtnotgood.common.recipe.machine.CrossRecipeWirelessEnergyHatchRecipes;
import com.xyp.gtnotgood.common.recipe.machine.DimensionalFocusRecipes;
import com.xyp.gtnotgood.common.recipe.machine.EndCrystalResonatorRecipes;
import com.xyp.gtnotgood.common.recipe.machine.FieldExtruderRecipes;
import com.xyp.gtnotgood.common.recipe.machine.FluxConnectorRecipes;
import com.xyp.gtnotgood.common.recipe.machine.FragmentWorldCollectorRecipes;
import com.xyp.gtnotgood.common.recipe.machine.HeartOfTheUniverseRecipes;
import com.xyp.gtnotgood.common.recipe.machine.IntegratedChemicalPlantRecipes;
import com.xyp.gtnotgood.common.recipe.machine.IntegratedProductionFactoryRecipes;
import com.xyp.gtnotgood.common.recipe.machine.KerrNewmanCasingRecipes;
import com.xyp.gtnotgood.common.recipe.machine.KerrNewmanHomogenizerRecipes;
import com.xyp.gtnotgood.common.recipe.machine.LargeBeeBreederRecipes;
import com.xyp.gtnotgood.common.recipe.machine.LargeCombProcessorRecipes;
import com.xyp.gtnotgood.common.recipe.machine.LargeCropBreederRecipes;
import com.xyp.gtnotgood.common.recipe.machine.LargeIndustrialApiaryRecipes;
import com.xyp.gtnotgood.common.recipe.machine.LargeIndustrialFarmRecipes;
import com.xyp.gtnotgood.common.recipe.machine.LargeInterfaceRecipes;
import com.xyp.gtnotgood.common.recipe.machine.LargeTransmutationMachineRecipes;
import com.xyp.gtnotgood.common.recipe.machine.LargeVoidDrillerRecipes;
import com.xyp.gtnotgood.common.recipe.machine.LargeVoidMinerRecipes;
import com.xyp.gtnotgood.common.recipe.machine.LightHunterSpaceStationRecipes;
import com.xyp.gtnotgood.common.recipe.machine.LightningProcessorRecipes;
import com.xyp.gtnotgood.common.recipe.machine.MEBridgeRecipes;
import com.xyp.gtnotgood.common.recipe.machine.MEContainerRecipes;
import com.xyp.gtnotgood.common.recipe.machine.MEDataAccessRecipes;
import com.xyp.gtnotgood.common.recipe.machine.MageAssemblerRecipes;
import com.xyp.gtnotgood.common.recipe.machine.MaxCapacityMEOutputRecipes;
import com.xyp.gtnotgood.common.recipe.machine.MechanicalUserRecipes;
import com.xyp.gtnotgood.common.recipe.machine.ModularHatchRecipes;
import com.xyp.gtnotgood.common.recipe.machine.NativeCasingRecipes;
import com.xyp.gtnotgood.common.recipe.machine.NetworkRecipes;
import com.xyp.gtnotgood.common.recipe.machine.NyarlathotepsTentacleRecipes;
import com.xyp.gtnotgood.common.recipe.machine.ParallelMachineRecipes;
import com.xyp.gtnotgood.common.recipe.machine.SeedConverterRecipes;
import com.xyp.gtnotgood.common.recipe.machine.SingularityDataHubRecipes;
import com.xyp.gtnotgood.common.recipe.machine.StockIOInterfaceRecipes;
import com.xyp.gtnotgood.common.recipe.machine.SuperCraftingInputRecipes;
import com.xyp.gtnotgood.common.recipe.machine.SuperParticleColliderRecipes;
import com.xyp.gtnotgood.common.recipe.machine.SuperconductingElectromagneticRecipes;
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
        HeartOfTheUniverseRecipes.loadPostInitRecipes();
        LightHunterSpaceStationRecipes.loadPostInitRecipes();
        SuperParticleColliderRecipes.loadPostInitRecipes();
        MageAssemblerRecipes.loadPostInitRecipes();
        NyarlathotepsTentacleRecipes.loadPostInitRecipes();
        SuperconductingElectromagneticRecipes.loadPostInitRecipes();
        KerrNewmanHomogenizerRecipes.loadPostInitRecipes();
        KerrNewmanCasingRecipes.loadPostInitRecipes();
        ModularHatchRecipes.loadRecipes();
        CentrifugeRecipes.loadRecipes();
        LargeChemicalReactorRecipes.loadRecipes();
        LargeIndustrialFarmRecipes.loadRecipes();
        MixerRecipes.loadRecipes();
        FuelRefiningRecipes.loadRecipes();
        WoodDistillationRecipes.loadRecipes();
        PetrochemicalRecipes.loadRecipes();
        PetrochemicalSynthesisRecipes.loadRecipes();
        RareEarthProcessingRecipes.loadRecipes();
    }

    public static void loadRecipes() {
        NativeStructureCasings.bind();
        NativeCasingRecipes.loadRecipes();
        EndCrystalResonatorRecipes.loadRecipes();
        HeartOfTheUniverseRecipes.loadRecipes();
        LightHunterSpaceStationRecipes.loadRecipes();
        GameRegistry.addShapelessRecipe(GTNGItemList.UniversalFluidPump.get(1), ItemList.Pump_LV.get(1),
            ItemList.Electric_Pump_LV.get(1));
        if (ModList.Forestry.isModLoaded()) {
            WorkingApiaryRegistration.registerRecipe();
            BasicIndustrialApiaryRecipes.loadRecipes();
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
        FieldExtruderRecipes.loadRecipes();
        DimensionalFocusRecipes.loadRecipes();
        SuperParticleColliderRecipes.loadRecipes();
        MageAssemblerRecipes.loadRecipes();
        AssemblyPlantCasingRecipes.loadRecipes();
        LightningProcessorRecipes.loadRecipes();
        SuperconductingElectromagneticRecipes.loadRecipes();
        AssemblerRecipes.loadRecipes();
        CrossRecipeWirelessEnergyHatchRecipes.loadRecipes();
        BenderRecipes.loadRecipes();
        FormingPressRecipes.loadRecipes();
        FurnaceRecipes.loadRecipes();
        FuelRodRecipes.loadRecipes();
        SingularityDataHubRecipes.loadRecipes();
        CraftingTableRecipes.loadRecipes();
        SeedConverterRecipes.loadRecipes();
        ParallelMachineRecipes.loadRecipes();
        FragmentWorldCollectorRecipes.loadMachineRecipe();
        OreProcessingRecipes.loadOreProcessingRecipes();
        MEBridgeRecipes.loadRecipes();
        MEContainerRecipes.loadRecipes();
        MEDataAccessRecipes.loadRecipes();
        MaxCapacityMEOutputRecipes.loadRecipes();
        WildcardPatternRecipes.loadRecipes();
        PatternSorterRecipes.register();
        MultiblockTerminalRecipes.register();
        SuperCraftingInputRecipes.loadRecipes();
        LargeVoidMinerRecipes.loadRecipes();
        LargeVoidDrillerRecipes.loadRecipes();
        LargeBeeBreederRecipes.loadRecipes();
        LargeCombProcessorRecipes.loadRecipes();
        LargeCropBreederRecipes.loadRecipes();
        NetworkRecipes.loadRecipes();
    }
}
