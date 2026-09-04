package com.xyp.gtnotgood;

import net.minecraftforge.common.MinecraftForge;

import com.xyp.gtnotgood.client.mebridge.MEWirelessNodeRenderer;
import com.xyp.gtnotgood.client.torcherino.WirelessTorcherinoBeamRenderer;
import com.xyp.gtnotgood.common.gui.BlockIcons;
import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.utils.event.SubscribeEventClientUtils;
import com.xyp.gtnotgood.utils.event.ToolBeltClientEvents;
import com.xyp.gtnotgood.utils.keybind.KeyBindManager;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;

/**
 * Client-side proxy for render, GUI, and other client-only registrations.
 */
public class ClientProxy extends CommonProxy {

    /**
     * Initializes client-only research automation after the shared config has
     * been loaded by {@link CommonProxy}.
     *
     * @param event Forge pre-initialization event
     */
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        com.xyp.gtnotgood.client.research.Config.synchronizeConfiguration();
        FMLCommonHandler.instance()
            .bus()
            .register(new com.xyp.gtnotgood.client.research.ClientResearchTickHandler());
    }

    // Override CommonProxy methods here, if you want a different behaviour on the client (e.g. registering renders).
    // Don't forget to call the super methods as well.

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        KeyBindManager.registerAllKeyBinds();
        // Force load BlockIcons to register textures
        BlockIcons.values();
        FMLCommonHandler.instance()
            .bus()
            .register(new ToolBeltClientEvents());

        SubscribeEventClientUtils clientUtils = new SubscribeEventClientUtils();
        MinecraftForge.EVENT_BUS.register(clientUtils);
        FMLCommonHandler.instance()
            .bus()
            .register(clientUtils);
        MinecraftForge.EVENT_BUS.register(new MEWirelessNodeRenderer());
        if (Config.enableWirelessTorcherino) {
            MinecraftForge.EVENT_BUS.register(new WirelessTorcherinoBeamRenderer());
        }
    }

    /**
     * Registers the optional ThaumcraftResearchTweaks GUI bridge once all mods
     * have completed loading.
     *
     * @param event Forge load-complete event
     */
    @Override
    public void complete(FMLLoadCompleteEvent event) {
        super.complete(event);
        if (Loader.isModLoaded("ThaumcraftResearchTweaks")) {
            NetworkRegistry.INSTANCE
                .registerGuiHandler("ThaumcraftResearchTweaks", new com.xyp.gtnotgood.client.research.GuiHandler());
        }
    }
}
