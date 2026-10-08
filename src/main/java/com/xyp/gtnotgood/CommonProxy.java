package com.xyp.gtnotgood;

import net.minecraftforge.common.MinecraftForge;

import com.cleanroommc.modularui.factory.GuiManager;
import com.gtnewhorizon.gtnhlib.chat.ChatComponentCustomRegistry;
import com.rtsbuilding.rtsbuilding.RtsbuildingMod;
import com.xyp.gtnotgood.ae2thing.AE2Thing;
import com.xyp.gtnotgood.commandtree.CommandTreeBootstrap;
import com.xyp.gtnotgood.common.blocks.largeinterface.LargeInterfaceGuiFactory;
import com.xyp.gtnotgood.common.blocks.mebridge.MEBridgeEventHandler;
import com.xyp.gtnotgood.common.blocks.mebridge.MEWirelessLinkEventHandler;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.LargeVoidMinerConfigGuiFactory;
import com.xyp.gtnotgood.common.items.compass.StructureSearch;
import com.xyp.gtnotgood.common.items.toolbelt.common.BeltEvents;
import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtnotgood.common.machines.hatch.me.ChatComponentInterfaceNameSuffix;
import com.xyp.gtnotgood.common.machines.hatch.me.CircuitMEPatternBuffer;
import com.xyp.gtnotgood.common.machines.multiblock.AssemblerMatrix;
import com.xyp.gtnotgood.common.network.NetworkHandler;
import com.xyp.gtnotgood.common.network.ServerConfigMessage;
import com.xyp.gtnotgood.common.network.WirelessMonitorSnapshot;
import com.xyp.gtnotgood.common.parts.advancedio.AdvancedIOGuiFactory;
import com.xyp.gtnotgood.common.parts.largeinterface.PartLargeInterface;
import com.xyp.gtnotgood.common.parts.mestock.StockRegistration;
import com.xyp.gtnotgood.common.parts.stockio.StockIOGuiFactory;
import com.xyp.gtnotgood.common.recipe.gtnotgood.CombProcessingRecipes;
import com.xyp.gtnotgood.common.recipe.gtnotgood.OreProcessingRecipes;
import com.xyp.gtnotgood.common.recipe.gtnotgood.TransmutationRecipes;
import com.xyp.gtnotgood.common.recipe.machine.EasyWirelessRecipes;
import com.xyp.gtnotgood.common.recipe.machine.LargeInterfaceRecipes;
import com.xyp.gtnotgood.common.wireless.monitor.WirelessMonitorService;
import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.config.MainConfig;
import com.xyp.gtnotgood.config.ServerConfigService;
import com.xyp.gtnotgood.loader.BlockLoader;
import com.xyp.gtnotgood.loader.ItemsLoader;
import com.xyp.gtnotgood.loader.MachineLoader;
import com.xyp.gtnotgood.loader.RecipeLoader;
import com.xyp.gtnotgood.loader.WirelessLaserLoader;

import appeng.api.AEApi;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;

/**
 * Common-side lifecycle proxy for configuration, machine registration, and recipes.
 */
public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        ChatComponentCustomRegistry.register(ChatComponentInterfaceNameSuffix::new);
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());
        MainConfig.ensureLoaded();
        if (Config.enableRTSBuilding) RtsbuildingMod.INSTANCE.preInit(event);

        GTNotGood.channel = NetworkRegistry.INSTANCE.newSimpleChannel(GTNotGood.MODID);
        NetworkHandler.registerAllMessage();
        FMLCommonHandler.instance().bus().register(new WirelessMonitorService());
        CommandTreeBootstrap.preInit();
        FMLCommonHandler.instance().bus().register(new ServerConfigService());

        ItemsLoader.registry();
        MinecraftForge.EVENT_BUS.register(StructureSearch.INSTANCE);
        FMLCommonHandler.instance().bus().register(StructureSearch.INSTANCE);
        BlockLoader.registry();
        StockRegistration.preInit();
        MachineLoader.registry();
        AE2Thing.preInit(event, GTNotGood.instance);

        BeltEvents beltEvents = new BeltEvents();
        MinecraftForge.EVENT_BUS.register(beltEvents);
        FMLCommonHandler.instance().bus().register(beltEvents);

        MinecraftForge.EVENT_BUS.register(new MEBridgeEventHandler());
        MEWirelessLinkEventHandler wirelessLinkHandler = new MEWirelessLinkEventHandler();
        MinecraftForge.EVENT_BUS.register(wirelessLinkHandler);
        FMLCommonHandler.instance().bus().register(wirelessLinkHandler);

        GTNotGood.LOG.info(Config.greeting);
        GTNotGood.LOG.info("Loaded " + GTNotGood.NAME + " at version " + Tags.VERSION);
    }

    public void init(FMLInitializationEvent event) {
        if (RtsbuildingMod.INSTANCE.isInitialized()) RtsbuildingMod.INSTANCE.init(event);
        GuiManager.registerFactory(AdvancedIOGuiFactory.INSTANCE);
        GuiManager.registerFactory(StockIOGuiFactory.INSTANCE);
        GuiManager.registerFactory(LargeInterfaceGuiFactory.INSTANCE);
        GuiManager.registerFactory(LargeVoidMinerConfigGuiFactory.INSTANCE);
        RecipeLoader.loadRecipes();
        StockRegistration.init();
        AE2Thing.init(event);
        CommandTreeBootstrap.init();
    }

    public void postInit(FMLPostInitializationEvent event) {
        AEApi.instance().registries().interfaceTerminal().register(PartLargeInterface.class);
        RecipeLoader.loadPostInitRecipes();
        WirelessLaserLoader.bindNativeHatches();
        EasyWirelessRecipes.loadRecipes();
        AEApi.instance().registries().interfaceTerminal().register(SuperMTEHatchCraftingInputME.class);
        AEApi.instance().registries().interfaceTerminal().register(CircuitMEPatternBuffer.class);
        AEApi.instance().registries().interfaceTerminal().register(AssemblerMatrix.class);
        AE2Thing.postInit(event);
    }

    public void complete(FMLLoadCompleteEvent event) {
        LargeInterfaceRecipes.registerUpgrades();
        AE2Thing.onLoadComplete(event);
        OreProcessingRecipes.loadExternalOreRecipes();
        CombProcessingRecipes.loadRecipes();
        TransmutationRecipes.load();
    }

    public void serverStarting(FMLServerStartingEvent event) {}

    /** Receives a server settings reply on the client proxy; dedicated servers have no settings screen. */
    public void receiveServerConfig(ServerConfigMessage message) {}

    /** Forwards a balance snapshot only on the client; dedicated servers have no HUD renderer. */
    public void receiveWirelessMonitor(WirelessMonitorSnapshot message) {}

    /** Server-side names preserve the standard suffix without loading NEI client configuration. */
    public boolean preferOwnInterfaceNames() {
        return false;
    }

}
