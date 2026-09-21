package com.xyp.gtnotgood;

import net.minecraftforge.common.MinecraftForge;

import com.xyp.gtnotgood.client.mebridge.MEWirelessNodeRenderer;
import com.xyp.gtnotgood.client.network.NetworkBlockRenderer;
import com.xyp.gtnotgood.common.gui.BlockIcons;
import com.xyp.gtnotgood.common.network.BlockNetwork;
import com.xyp.gtnotgood.utils.event.SubscribeEventClientUtils;
import com.xyp.gtnotgood.utils.event.ToolBeltClientEvents;
import com.xyp.gtnotgood.utils.keybind.KeyBindManager;

import cpw.mods.fml.client.registry.RenderingRegistry;
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

    @Override
    public void receiveServerConfig(com.xyp.gtnotgood.common.packet.ServerConfigMessage message) {
        net.minecraft.client.Minecraft.getMinecraft()
            .func_152344_a(() -> com.xyp.gtnotgood.client.config.ServerSettingsScreen.receive(message));
    }

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
        com.xyp.gtnotgood.client.text.effect.BuiltinTextEffects.register();
        ((net.minecraft.client.resources.IReloadableResourceManager) net.minecraft.client.Minecraft.getMinecraft()
            .getResourceManager()).registerReloadListener(com.xyp.gtnotgood.client.text.EffectTextRenderer.INSTANCE);
        net.minecraftforge.client.ClientCommandHandler.instance
            .registerCommand(new com.xyp.gtnotgood.client.text.preview.TextEffectPreviewCommand());
        if (com.xyp.gtnotgood.utils.enums.ModList.Thaumcraft.isModLoaded()) {
            net.minecraftforge.client.MinecraftForgeClient.registerItemRenderer(
                com.xyp.gtnotgood.utils.enums.GTNGItemList.ThaumcraftInfusionCore.getItem(),
                new com.xyp.gtnotgood.client.packaged.PackagedCoreRenderer(
                    () -> new net.minecraft.item.ItemStack(
                        thaumcraft.common.config.ConfigBlocks.blockStoneDevice,
                        1,
                        2)));
        }
        net.minecraftforge.client.MinecraftForgeClient.registerItemRenderer(
            com.xyp.gtnotgood.utils.enums.GTNGItemList.AssemblyLineCore.getItem(),
            new com.xyp.gtnotgood.client.packaged.PackagedCoreRenderer(
                () -> gregtech.api.enums.ItemList.Machine_Multi_Assemblyline.get(1)));
        net.minecraftforge.client.MinecraftForgeClient.registerItemRenderer(
            com.xyp.gtnotgood.utils.enums.GTNGItemList.AdvancedAssemblyLineCore.getItem(),
            new com.xyp.gtnotgood.client.packaged.PackagedCoreRenderer(() -> ggfab.GGItemList.AdvAssLine.get(1)));
        com.xyp.gtnotgood.client.gui.wildcard.WildcardPreviewCommand.register();
        com.xyp.gtnotgood.client.gui.LibraryDemoCommand.register();
        BlockNetwork.cableRenderId = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(new NetworkBlockRenderer());
        com.xyp.gtnotgood.common.flux.BlockFluxConnector.renderId = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(new com.xyp.gtnotgood.client.flux.FluxConnectorRenderer());
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
        MinecraftForge.EVENT_BUS.register(new com.xyp.gtnotgood.client.packaged.WirelessConnectorRenderer());
        MinecraftForge.EVENT_BUS.register(new com.xyp.gtnotgood.client.nei.FactoryRecipeImport());
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
        if (com.xyp.gtnotgood.config.Config.useEdgeWindowIcon) {
            FMLCommonHandler.instance()
                .bus()
                .register(new com.xyp.gtnotgood.client.EdgeWindowIcon());
        }
        if (Loader.isModLoaded("ThaumcraftResearchTweaks")) {
            NetworkRegistry.INSTANCE
                .registerGuiHandler("ThaumcraftResearchTweaks", new com.xyp.gtnotgood.client.research.GuiHandler());
        }
    }
}
