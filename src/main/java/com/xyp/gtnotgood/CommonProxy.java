package com.xyp.gtnotgood;

import net.minecraftforge.common.MinecraftForge;

import com.cleanroommc.modularui.factory.GuiManager;
import com.xyp.gtnotgood.ae2thing.AE2Thing;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.LargeVoidMinerConfigGuiFactory;
import com.xyp.gtnotgood.common.items.toolbelt.common.BeltEvents;
import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtnotgood.common.mebridge.MEBridgeEventHandler;
import com.xyp.gtnotgood.common.mebridge.MEWirelessLinkEventHandler;
import com.xyp.gtnotgood.common.packet.NetWorkHandler;
import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.config.MainConfig;
import com.xyp.gtnotgood.loader.BlockLoader;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;
import com.xyp.gtnotgood.loader.ItemsLoader;
import com.xyp.gtnotgood.loader.MachineLoader;
import com.xyp.gtnotgood.loader.RecipeLoader;

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

    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());
        MainConfig.ensureLoaded();

        GTNotGood.channel = NetworkRegistry.INSTANCE.newSimpleChannel(GTNotGood.MODID);
        NetWorkHandler.registerAllMessage();
        FMLCommonHandler.instance()
            .bus()
            .register(new com.xyp.gtnotgood.config.ServerConfigService());

        ItemsLoader.registry();
        BlockLoader.registry();
        MachineLoader.registry();
        AE2Thing.preInit(event, GTNotGood.instance);

        BeltEvents beltEvents = new BeltEvents();
        MinecraftForge.EVENT_BUS.register(beltEvents);
        FMLCommonHandler.instance()
            .bus()
            .register(beltEvents);

        MinecraftForge.EVENT_BUS.register(new MEBridgeEventHandler());
        MEWirelessLinkEventHandler wirelessLinkHandler = new MEWirelessLinkEventHandler();
        MinecraftForge.EVENT_BUS.register(wirelessLinkHandler);
        FMLCommonHandler.instance()
            .bus()
            .register(wirelessLinkHandler);

        GTNotGood.LOG.info(Config.greeting);
        GTNotGood.LOG.info("Loaded " + GTNotGood.NAME + " at version " + Tags.VERSION);
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {
        GuiManager.registerFactory(LargeVoidMinerConfigGuiFactory.INSTANCE);
        RecipeLoader.loadRecipes();
        AE2Thing.init(event);
    }

    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {
        AEApi.instance()
            .registries()
            .interfaceTerminal()
            .register(SuperMTEHatchCraftingInputME.class);
        AE2Thing.postInit(event);
        GTNGRecipeMaps.populateAssemblyFactoryAssemblyLineRecipes();
    }

    public void complete(FMLLoadCompleteEvent event) {
        AE2Thing.onLoadComplete(event);
    }

    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {}

    /** Receives a server settings reply on the client proxy; dedicated servers have no settings screen. */
    public void receiveServerConfig(com.xyp.gtnotgood.common.packet.ServerConfigMessage message) {}
}
