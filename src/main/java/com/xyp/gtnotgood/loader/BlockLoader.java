package com.xyp.gtnotgood.loader;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import com.xyp.gtnotgood.common.blocks.mebridge.BlockMEBridgeReceiver;
import com.xyp.gtnotgood.common.blocks.mebridge.BlockMEBridgeSender;
import com.xyp.gtnotgood.common.blocks.mebridge.ItemBlockMEBridge;
import com.xyp.gtnotgood.common.mebridge.TileMEBridgeReceiver;
import com.xyp.gtnotgood.common.mebridge.TileMEBridgeSender;
import com.xyp.gtnotgood.common.mecontainer.BlockMEContainer;
import com.xyp.gtnotgood.common.mecontainer.TileMEContainer;
import com.xyp.gtnotgood.common.network.BlockNetwork;
import com.xyp.gtnotgood.common.network.NetworkTopology;
import com.xyp.gtnotgood.common.network.TileNetworkController;
import com.xyp.gtnotgood.common.network.TileNetworkNode;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Registers ordinary Forge blocks and tile entities owned by GT Not Good.
 */
public final class BlockLoader {

    // #tr tile.flux_logistics_plug.name
    // # Flux Logistics Plug
    // # zh_CN 通量物流插头
    public static final com.xyp.gtnotgood.common.flux.BlockFluxLogistics fluxLogistics = new com.xyp.gtnotgood.common.flux.BlockFluxLogistics();

    public static final com.xyp.gtnotgood.common.user.BlockMechanicalUser mechanicalUser = new com.xyp.gtnotgood.common.user.BlockMechanicalUser();

    // #tr tile.flux_plug.name
    // # Flux Plug
    // # zh_CN 通量插头
    public static final com.xyp.gtnotgood.common.flux.BlockFluxConnector fluxPlug = new com.xyp.gtnotgood.common.flux.BlockFluxConnector(
        true,
        "flux_plug");
    // #tr tile.flux_point.name
    // # Flux Point
    // # zh_CN 通量点
    public static final com.xyp.gtnotgood.common.flux.BlockFluxConnector fluxPoint = new com.xyp.gtnotgood.common.flux.BlockFluxConnector(
        false,
        "flux_point");

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
    public static final BlockMEBridgeReceiver blockMEBridgeReceiver = new BlockMEBridgeReceiver();

    private BlockLoader() {}

    /**
     * Registers all non-GregTech blocks and stores their item stacks for recipe and creative-tab use.
     */
    public static void registry() {
        GameRegistry.registerBlock(mechanicalUser, "mechanical_user");
        GameRegistry.registerTileEntity(
            com.xyp.gtnotgood.common.user.TileMechanicalUser.class,
            ModList.GTNotGood.getResourcePath("mechanical_user"));
        GTNGItemList.MechanicalUser.set(new ItemStack(mechanicalUser));
        GameRegistry.registerBlock(fluxPlug, com.xyp.gtnotgood.common.flux.ItemBlockFluxConnector.class, "flux_plug");
        GameRegistry.registerBlock(fluxPoint, com.xyp.gtnotgood.common.flux.ItemBlockFluxConnector.class, "flux_point");
        GameRegistry.registerTileEntity(
            com.xyp.gtnotgood.common.flux.TileFluxPlug.class,
            ModList.GTNotGood.getResourcePath("flux_plug"));
        GameRegistry.registerTileEntity(
            com.xyp.gtnotgood.common.flux.TileFluxPoint.class,
            ModList.GTNotGood.getResourcePath("flux_point"));
        GTNGItemList.FluxPlug.set(new ItemStack(fluxPlug));
        GTNGItemList.FluxPoint.set(new ItemStack(fluxPoint));
        GameRegistry.registerBlock(
            fluxLogistics,
            com.xyp.gtnotgood.common.flux.ItemBlockFluxConnector.class,
            "flux_logistics_plug");
        GameRegistry.registerTileEntity(
            com.xyp.gtnotgood.common.flux.TileFluxLogistics.class,
            ModList.GTNotGood.getResourcePath("flux_logistics_plug"));
        GTNGItemList.FluxLogisticsPlug.set(new ItemStack(fluxLogistics));
        cpw.mods.fml.common.FMLCommonHandler.instance()
            .bus()
            .register(new com.xyp.gtnotgood.common.flux.FluxTransferScheduler());
        net.minecraftforge.common.ForgeChunkManager.setForcedChunkLoadingCallback(
            com.xyp.gtnotgood.GTNotGood.instance,
            new com.xyp.gtnotgood.common.flux.FluxChunkLoading());
        GameRegistry.registerBlock(meContainer, ItemBlockMEBridge.class, "me_container");
        GameRegistry.registerTileEntity(TileMEContainer.class, ModList.GTNotGood.getResourcePath("me_container"));
        GTNGItemList.MEContainer.set(new ItemStack(meContainer));
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
        GameRegistry.registerBlock(blockMEBridgeSender, ItemBlockMEBridge.class, "blockMEBridgeSender");
        GameRegistry.registerBlock(blockMEBridgeReceiver, ItemBlockMEBridge.class, "blockMEBridgeReceiver");
        GameRegistry.registerTileEntity(TileMEBridgeSender.class, "tileMEBridgeSender");
        GameRegistry.registerTileEntity(TileMEBridgeReceiver.class, "tileMEBridgeReceiver");

        GTNGItemList.MEBridgeSender.set(new ItemStack(blockMEBridgeSender));
        GTNGItemList.MEBridgeReceiver.set(new ItemStack(blockMEBridgeReceiver));
    }
}
