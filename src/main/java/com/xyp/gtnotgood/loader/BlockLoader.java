package com.xyp.gtnotgood.loader;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.blocks.beekeeping.WorkingApiaryRegistration;
import com.xyp.gtnotgood.common.blocks.flux.BlockFluxConnector;
import com.xyp.gtnotgood.common.blocks.flux.BlockFluxLogistics;
import com.xyp.gtnotgood.common.blocks.flux.FluxChunkLoading;
import com.xyp.gtnotgood.common.blocks.flux.FluxTransferScheduler;
import com.xyp.gtnotgood.common.blocks.flux.ItemBlockFluxConnector;
import com.xyp.gtnotgood.common.blocks.flux.TileFluxLogistics;
import com.xyp.gtnotgood.common.blocks.flux.TileFluxPlug;
import com.xyp.gtnotgood.common.blocks.flux.TileFluxPoint;
import com.xyp.gtnotgood.common.blocks.largeinterface.BlockLargeInterface;
import com.xyp.gtnotgood.common.blocks.largeinterface.TileLargeInterface;
import com.xyp.gtnotgood.common.blocks.mebridge.BlockMEBridgeReceiver;
import com.xyp.gtnotgood.common.blocks.mebridge.BlockMEBridgeSender;
import com.xyp.gtnotgood.common.blocks.mebridge.ItemBlockMEBridge;
import com.xyp.gtnotgood.common.blocks.mebridge.TileMEBridgeReceiver;
import com.xyp.gtnotgood.common.blocks.mebridge.TileMEBridgeSender;
import com.xyp.gtnotgood.common.blocks.mechanicaluser.BlockMechanicalUser;
import com.xyp.gtnotgood.common.blocks.mechanicaluser.TileMechanicalUser;
import com.xyp.gtnotgood.common.blocks.mecontainer.BlockMEContainer;
import com.xyp.gtnotgood.common.blocks.mecontainer.TileMEContainer;
import com.xyp.gtnotgood.common.blocks.network.BlockNetwork;
import com.xyp.gtnotgood.common.blocks.network.NetworkTopology;
import com.xyp.gtnotgood.common.blocks.network.TileNetworkController;
import com.xyp.gtnotgood.common.blocks.network.TileNetworkNode;
import com.xyp.gtnotgood.common.blocks.packaged.BlockPackagedProvider;
import com.xyp.gtnotgood.common.blocks.packaged.PackagedServerActions;
import com.xyp.gtnotgood.common.blocks.packaged.TilePackagedProvider;
import com.xyp.gtnotgood.common.blocks.stockio.BlockStockIOInterface;
import com.xyp.gtnotgood.common.blocks.stockio.TileStockIOInterface;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Registers ordinary Forge blocks and tile entities owned by GT Not Good.
 */
public final class BlockLoader {

    // #tr tile.flux_logistics_plug.name
    // # Flux Logistics Plug
    // # zh_CN 通量物流插头
    public static final BlockFluxLogistics fluxLogistics = new BlockFluxLogistics();

    public static final BlockMechanicalUser mechanicalUser = new BlockMechanicalUser();

    // #tr tile.flux_plug.name
    // # Flux Plug
    // # zh_CN 通量插头
    public static final BlockFluxConnector fluxPlug = new BlockFluxConnector(true, "flux_plug");
    // #tr tile.flux_point.name
    // # Flux Point
    // # zh_CN 通量点
    public static final BlockFluxConnector fluxPoint = new BlockFluxConnector(false, "flux_point");

    // #tr tile.network_controller.name
    // # Programmable Network Controller
    // # zh_CN 可编程网络控制器
    public static final BlockNetwork networkController = new BlockNetwork(
        BlockNetwork.CONTROLLER,
        "network_controller");
    // #tr tile.network_pipe.name
    // # Network Cable
    // # zh_CN 网络管道
    public static final BlockNetwork networkPipe = new BlockNetwork(BlockNetwork.PIPE, "network_pipe");
    // #tr tile.network_connector.name
    // # Network Connector
    // # zh_CN 网络连接器
    public static final BlockNetwork networkConnector = new BlockNetwork(BlockNetwork.CONNECTOR, "network_connector");

    public static final BlockMEBridgeSender blockMEBridgeSender = new BlockMEBridgeSender();
    public static final BlockMEContainer meContainer = new BlockMEContainer();
    public static final BlockStockIOInterface stockIOInterface = new BlockStockIOInterface();
    public static final BlockLargeInterface largeInterface = new BlockLargeInterface();
    public static final BlockMEBridgeReceiver blockMEBridgeReceiver = new BlockMEBridgeReceiver();

    private BlockLoader() {}

    /**
     * Registers all non-GregTech blocks and stores their item stacks for recipe and creative-tab use.
     */
    public static void registry() {
        if (ModList.Forestry.isModLoaded()) {
            WorkingApiaryRegistration.register();
        }
        registerPackagedProvider();
        registerMechanicalUser();
        registerFluxBlocks();
        registerMEContainer();
        registerStockIOInterface();
        largeInterface.register();
        GTNGItemList.LargeInterface.set(new ItemStack(largeInterface));
        AEApi.instance()
            .registries()
            .interfaceTerminal()
            .register(TileLargeInterface.class);
        registerNetworkBlocks();
        registerMEBridgeBlocks();
    }

    private static void registerPackagedProvider() {
        BlockPackagedProvider packagedProvider = new BlockPackagedProvider();
        GameRegistry.registerBlock(packagedProvider, "wireless_packaged_pattern_provider");
        GameRegistry.registerTileEntity(
            TilePackagedProvider.class,
            ModList.GTNotGood.getResourcePath("wireless_packaged_pattern_provider"));
        GTNGItemList.WirelessPackagedPatternProvider.set(new ItemStack(packagedProvider));
        AEApi.instance()
            .registries()
            .interfaceTerminal()
            .register(TilePackagedProvider.class);
        FMLCommonHandler.instance()
            .bus()
            .register(new PackagedServerActions());
    }

    private static void registerMechanicalUser() {
        GameRegistry.registerBlock(mechanicalUser, "mechanical_user");
        GameRegistry.registerTileEntity(TileMechanicalUser.class, ModList.GTNotGood.getResourcePath("mechanical_user"));
        GTNGItemList.MechanicalUser.set(new ItemStack(mechanicalUser));
    }

    private static void registerFluxBlocks() {
        GameRegistry.registerBlock(fluxPlug, ItemBlockFluxConnector.class, "flux_plug");
        GameRegistry.registerBlock(fluxPoint, ItemBlockFluxConnector.class, "flux_point");
        GameRegistry.registerTileEntity(TileFluxPlug.class, ModList.GTNotGood.getResourcePath("flux_plug"));
        GameRegistry.registerTileEntity(TileFluxPoint.class, ModList.GTNotGood.getResourcePath("flux_point"));
        GTNGItemList.FluxPlug.set(new ItemStack(fluxPlug));
        GTNGItemList.FluxPoint.set(new ItemStack(fluxPoint));
        GameRegistry.registerBlock(fluxLogistics, ItemBlockFluxConnector.class, "flux_logistics_plug");
        GameRegistry
            .registerTileEntity(TileFluxLogistics.class, ModList.GTNotGood.getResourcePath("flux_logistics_plug"));
        GTNGItemList.FluxLogisticsPlug.set(new ItemStack(fluxLogistics));
        FMLCommonHandler.instance()
            .bus()
            .register(new FluxTransferScheduler());
        ForgeChunkManager.setForcedChunkLoadingCallback(GTNotGood.instance, new FluxChunkLoading());
    }

    private static void registerMEContainer() {
        GameRegistry.registerBlock(meContainer, ItemBlockMEBridge.class, "me_container");
        GameRegistry.registerTileEntity(TileMEContainer.class, ModList.GTNotGood.getResourcePath("me_container"));
        GTNGItemList.MEContainer.set(new ItemStack(meContainer));
    }

    private static void registerStockIOInterface() {
        GameRegistry.registerBlock(stockIOInterface, ItemBlockMEBridge.class, "stock_io_interface");
        GameRegistry
            .registerTileEntity(TileStockIOInterface.class, ModList.GTNotGood.getResourcePath("stock_io_interface"));
        GTNGItemList.StockIOInterface.set(new ItemStack(stockIOInterface));
    }

    private static void registerNetworkBlocks() {
        GameRegistry.registerBlock(networkController, "network_controller");
        GameRegistry.registerBlock(networkPipe, "network_pipe");
        GameRegistry.registerBlock(networkConnector, "network_connector");
        GameRegistry.registerTileEntity(TileNetworkNode.class, ModList.GTNotGood.getResourcePath("network_node"));
        GameRegistry
            .registerTileEntity(TileNetworkController.class, ModList.GTNotGood.getResourcePath("network_controller"));
        MinecraftForge.EVENT_BUS.register(new NetworkTopology.Events());
        GTNGItemList.NetworkController.set(new ItemStack(networkController));
        GTNGItemList.NetworkPipe.set(new ItemStack(networkPipe));
        GTNGItemList.NetworkConnector.set(new ItemStack(networkConnector));
    }

    /** Preserves the legacy tile entity IDs used by ME bridge blocks in existing worlds. */
    private static void registerMEBridgeBlocks() {
        GameRegistry.registerBlock(blockMEBridgeSender, ItemBlockMEBridge.class, "blockMEBridgeSender");
        GameRegistry.registerBlock(blockMEBridgeReceiver, ItemBlockMEBridge.class, "blockMEBridgeReceiver");
        GameRegistry.registerTileEntity(TileMEBridgeSender.class, "tileMEBridgeSender");
        GameRegistry.registerTileEntity(TileMEBridgeReceiver.class, "tileMEBridgeReceiver");

        GTNGItemList.MEBridgeSender.set(new ItemStack(blockMEBridgeSender));
        GTNGItemList.MEBridgeReceiver.set(new ItemStack(blockMEBridgeReceiver));
    }
}
