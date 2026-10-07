package com.xyp.gtnotgood;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;

import com.xyp.gtnotgood.ae2thing.nei.ButtonConstants;
import com.xyp.gtnotgood.ae2thing.nei.NEI_TH_Config;
import com.xyp.gtnotgood.client.StructureCompassRenderer;
import com.xyp.gtnotgood.client.WindowsTaskbarButton;
import com.xyp.gtnotgood.client.flux.FluxConnectorRenderer;
import com.xyp.gtnotgood.client.gui.LibraryDemoCommand;
import com.xyp.gtnotgood.client.gui.wildcard.WildcardPreviewCommand;
import com.xyp.gtnotgood.client.mebridge.MEWirelessNodeRenderer;
import com.xyp.gtnotgood.client.mestock.StockModelRenderer;
import com.xyp.gtnotgood.client.nei.FactoryRecipeImport;
import com.xyp.gtnotgood.client.network.NetworkBlockRenderer;
import com.xyp.gtnotgood.client.packaged.PackagedCoreRenderer;
import com.xyp.gtnotgood.client.packaged.WirelessConnectorRenderer;
import com.xyp.gtnotgood.client.text.EffectTextRenderer;
import com.xyp.gtnotgood.client.text.TextEffectPreferences;
import com.xyp.gtnotgood.client.text.effect.BuiltinTextEffects;
import com.xyp.gtnotgood.client.text.preview.TextEffectPreviewCommand;
import com.xyp.gtnotgood.client.wireless.WirelessMonitorHud;
import com.xyp.gtnotgood.client.wireless.WirelessMonitorPreferences;
import com.xyp.gtnotgood.common.blocks.flux.BlockFluxConnector;
import com.xyp.gtnotgood.common.blocks.mestock.BlockMERequester;
import com.xyp.gtnotgood.common.blocks.network.BlockNetwork;
import com.xyp.gtnotgood.common.gui.BlockIcons;
import com.xyp.gtnotgood.common.network.WirelessMonitorSnapshot;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.event.SubscribeEventClientUtils;
import com.xyp.gtnotgood.utils.event.ToolBeltClientEvents;
import com.xyp.gtnotgood.utils.keybind.KeyBindManager;
import com.xyp.gtnotgood.utils.text.TextEffectsCompat;

import WayofTime.alchemicalWizardry.ModBlocks;
import appeng.api.AEApi;
import appeng.api.parts.IPartItem;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import ggfab.GGItemList;
import gregtech.api.enums.ItemList;
import thaumcraft.common.config.ConfigBlocks;

/**
 * Client-side proxy for render, GUI, and other client-only registrations.
 * Instantiated by Forge through the class name declared in {@link GTNotGood}'s sided proxy registration.
 */
@SuppressWarnings("unused")
public class ClientProxy extends CommonProxy {

    @Override
    public boolean preferOwnInterfaceNames() {
        return NEI_TH_Config.getConfigValue(ButtonConstants.PREFER_OWN_INTERFACE_NAMES, false);
    }

    @Override
    public void receiveServerConfig(com.xyp.gtnotgood.common.network.ServerConfigMessage message) {
        Minecraft.getMinecraft()
            .func_152344_a(() -> com.xyp.gtnotgood.client.config.ServerSettingsScreen.receive(message));
    }

    @Override
    public void receiveWirelessMonitor(WirelessMonitorSnapshot message) {
        Minecraft mc = Minecraft.getMinecraft();
        NetHandlerPlayClient source = mc.getNetHandler();
        mc.func_152344_a(() -> WirelessMonitorHud.INSTANCE.receive(message, source));
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

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        MinecraftForgeClient
            .registerItemRenderer(GTNGItemList.StructureCompass.getItem(), new StructureCompassRenderer());
        registerAEPartRenderer();
        initializeTextEffects();
        WirelessMonitorPreferences.load();
        registerPackagedCoreRenderers();
        registerPreviewCommands();
        registerBlockRenderers();
        KeyBindManager.registerAllKeyBinds();
        BlockIcons.initialize();
        registerClientEvents();
    }

    private void registerAEPartRenderer() {
        for (GTNGItemList part : new GTNGItemList[] { GTNGItemList.AdvancedIOBus, GTNGItemList.StockIOInterfacePart,
            GTNGItemList.LargeInterfacePart, GTNGItemList.ThresholdExportBus, GTNGItemList.ThresholdLevelEmitter,
            GTNGItemList.MERequesterTerminal }) {
            AEApi.instance()
                .partHelper()
                .setItemBusRenderer((IPartItem) part.getItem());
        }
        BlockMERequester.renderId = RenderingRegistry.getNextAvailableRenderId();
        StockModelRenderer stockRenderer = new StockModelRenderer();
        RenderingRegistry.registerBlockHandler(stockRenderer);
        MinecraftForge.EVENT_BUS.register(stockRenderer);
    }

    private void initializeTextEffects() {
        BuiltinTextEffects.register();
        TextEffectPreferences.load();
        if (!TextEffectsCompat.hasUpstreamRenderer()) {
            ((IReloadableResourceManager) Minecraft.getMinecraft()
                .getResourceManager()).registerReloadListener(EffectTextRenderer.INSTANCE);
        }
        ClientCommandHandler.instance.registerCommand(new TextEffectPreviewCommand());
    }

    /** Keeps optional mod access guarded and defers target icon creation until rendering. */
    private void registerPackagedCoreRenderers() {
        if (ModList.Thaumcraft.isModLoaded()) {
            MinecraftForgeClient.registerItemRenderer(
                GTNGItemList.ThaumcraftCrucibleCore.getItem(),
                new PackagedCoreRenderer(() -> new ItemStack(ConfigBlocks.blockMetalDevice, 1, 0)));
            MinecraftForgeClient.registerItemRenderer(
                GTNGItemList.ArcaneWorkbenchCore.getItem(),
                new PackagedCoreRenderer(() -> new ItemStack(ConfigBlocks.blockTable, 1, 15)));
            MinecraftForgeClient.registerItemRenderer(
                GTNGItemList.ThaumcraftInfusionCore.getItem(),
                new PackagedCoreRenderer(() -> new ItemStack(ConfigBlocks.blockStoneDevice, 1, 2)));
        }
        if (ModList.BloodMagic.isModLoaded()) {
            MinecraftForgeClient.registerItemRenderer(
                GTNGItemList.BloodAltarCore.getItem(),
                new PackagedCoreRenderer(() -> new ItemStack(ModBlocks.blockAltar)));
        }
        MinecraftForgeClient.registerItemRenderer(
            GTNGItemList.AssemblyLineCore.getItem(),
            new PackagedCoreRenderer(() -> ItemList.Machine_Multi_Assemblyline.get(1)));
        MinecraftForgeClient.registerItemRenderer(
            GTNGItemList.AdvancedAssemblyLineCore.getItem(),
            new PackagedCoreRenderer(() -> GGItemList.AdvAssLine.get(1)));
    }

    private void registerPreviewCommands() {
        WildcardPreviewCommand.register();
        LibraryDemoCommand.register();
    }

    private void registerBlockRenderers() {
        BlockNetwork.cableRenderId = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(new NetworkBlockRenderer());
        BlockFluxConnector.renderId = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(new FluxConnectorRenderer());
    }

    /** Registers one shared client utility listener on both Forge and FML event buses. */
    private void registerClientEvents() {
        FMLCommonHandler.instance()
            .bus()
            .register(new ToolBeltClientEvents());

        SubscribeEventClientUtils clientUtils = new SubscribeEventClientUtils();
        MinecraftForge.EVENT_BUS.register(clientUtils);
        FMLCommonHandler.instance()
            .bus()
            .register(clientUtils);
        MinecraftForge.EVENT_BUS.register(new MEWirelessNodeRenderer());
        MinecraftForge.EVENT_BUS.register(new WirelessConnectorRenderer());
        MinecraftForge.EVENT_BUS.register(new FactoryRecipeImport());
        MinecraftForge.EVENT_BUS.register(WirelessMonitorHud.INSTANCE);
        FMLCommonHandler.instance()
            .bus()
            .register(WirelessMonitorHud.INSTANCE);
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
        FMLCommonHandler.instance()
            .bus()
            .register(new WindowsTaskbarButton());
        if (ModList.ThaumcraftResearchTweaks.isModLoaded()) {
            NetworkRegistry.INSTANCE.registerGuiHandler(
                ModList.ThaumcraftResearchTweaks.getID(),
                new com.xyp.gtnotgood.client.research.GuiHandler());
        }
    }
}
