package com.xyp.gtnotgood.loader;

import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.common.blocks.machine.AssemblyMatrixBlock;
import com.xyp.gtnotgood.common.blocks.mebridge.BlockMEBridgeReceiver;
import com.xyp.gtnotgood.common.blocks.mebridge.BlockMEBridgeSender;
import com.xyp.gtnotgood.common.blocks.mebridge.ItemBlockMEBridge;
import com.xyp.gtnotgood.common.mebridge.TileMEBridgeReceiver;
import com.xyp.gtnotgood.common.mebridge.TileMEBridgeSender;
import com.xyp.gtnotgood.common.torcherino.block.BlockTorcherino;
import com.xyp.gtnotgood.common.torcherino.block.BlockWirelessTorcherino;
import com.xyp.gtnotgood.common.torcherino.block.TorcherinoTileFactory;
import com.xyp.gtnotgood.common.torcherino.item.ItemBlockTorcherino;
import com.xyp.gtnotgood.common.torcherino.item.ItemBlockWirelessTorcherino;
import com.xyp.gtnotgood.common.torcherino.tile.TileCompressedTorcherino;
import com.xyp.gtnotgood.common.torcherino.tile.TileCompressedWirelessTorcherino;
import com.xyp.gtnotgood.common.torcherino.tile.TileDoubleCompressedTorcherino;
import com.xyp.gtnotgood.common.torcherino.tile.TileDoubleCompressedWirelessTorcherino;
import com.xyp.gtnotgood.common.torcherino.tile.TileTorcherino;
import com.xyp.gtnotgood.common.torcherino.tile.TileWirelessTorcherino;
import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Registers ordinary Forge blocks and tile entities owned by GT Not Good.
 */
public final class BlockLoader {

    public static final BlockMEBridgeSender blockMEBridgeSender = new BlockMEBridgeSender();
    public static final BlockMEBridgeReceiver blockMEBridgeReceiver = new BlockMEBridgeReceiver();
    // #tr tile.AssemblyMatrixBlock.name
    // # Assembly Matrix Block
    // # zh_CN 装配矩阵方块
    public static final AssemblyMatrixBlock assemblyMatrixBlock = new AssemblyMatrixBlock(
        "AssemblyMatrixBlock",
        "AssemblyMatrixBlock",
        1);
    // #tr tile.AdvancedAssemblyMatrixBlock.name
    // # Advanced Assembly Matrix Block
    // # zh_CN 高级装配矩阵方块
    public static final AssemblyMatrixBlock advancedAssemblyMatrixBlock = new AssemblyMatrixBlock(
        "AdvancedAssemblyMatrixBlock",
        "AdvancedAssemblyMatrixBlock",
        2);
    // #tr tile.torcherino.name
    // # Torcherino
    // # zh_CN 加速火把
    public static final BlockTorcherino torcherino = new BlockTorcherino(
        "torcherino",
        "torcherino",
        0.75F,
        new TorcherinoTileFactory() {

            @Override
            public TileTorcherino create() {
                return new TileTorcherino();
            }
        });
    // #tr tile.compressed_torcherino.name
    // # Compressed Torcherino
    // # zh_CN 压缩加速火把
    public static final BlockTorcherino compressedTorcherino = new BlockTorcherino(
        "compressed_torcherino",
        "compressed_torcherino",
        1.0F,
        new TorcherinoTileFactory() {

            @Override
            public TileCompressedTorcherino create() {
                return new TileCompressedTorcherino();
            }
        });
    // #tr tile.double_compressed_torcherino.name
    // # Double-Compressed Torcherino
    // # zh_CN 二重压缩加速火把
    public static final BlockTorcherino doubleCompressedTorcherino = new BlockTorcherino(
        "double_compressed_torcherino",
        "double_compressed_torcherino",
        1.0F,
        new TorcherinoTileFactory() {

            @Override
            public TileDoubleCompressedTorcherino create() {
                return new TileDoubleCompressedTorcherino();
            }
        });
    // #tr tile.wireless_torcherino.name
    // # Wireless Torcherino
    // # zh_CN 无线加速火把
    public static final BlockWirelessTorcherino wirelessTorcherino = new BlockWirelessTorcherino(
        "wireless_torcherino",
        "torcherino",
        0.75F,
        new TorcherinoTileFactory() {

            @Override
            public TileWirelessTorcherino create() {
                return new TileWirelessTorcherino();
            }
        });
    // #tr tile.compressed_wireless_torcherino.name
    // # Compressed Wireless Torcherino
    // # zh_CN 压缩无线加速火把
    public static final BlockWirelessTorcherino compressedWirelessTorcherino = new BlockWirelessTorcherino(
        "compressed_wireless_torcherino",
        "compressed_torcherino",
        1.0F,
        new TorcherinoTileFactory() {

            @Override
            public TileCompressedWirelessTorcherino create() {
                return new TileCompressedWirelessTorcherino();
            }
        });
    // #tr tile.double_compressed_wireless_torcherino.name
    // # Double-Compressed Wireless Torcherino
    // # zh_CN 二重压缩无线加速火把
    public static final BlockWirelessTorcherino doubleCompressedWirelessTorcherino = new BlockWirelessTorcherino(
        "double_compressed_wireless_torcherino",
        "double_compressed_torcherino",
        1.0F,
        new TorcherinoTileFactory() {

            @Override
            public TileDoubleCompressedWirelessTorcherino create() {
                return new TileDoubleCompressedWirelessTorcherino();
            }
        });

    private BlockLoader() {}

    /**
     * Registers all non-GregTech blocks and stores their item stacks for recipe and creative-tab use.
     */
    public static void registry() {
        GameRegistry.registerBlock(blockMEBridgeSender, ItemBlockMEBridge.class, "blockMEBridgeSender");
        GameRegistry.registerBlock(blockMEBridgeReceiver, ItemBlockMEBridge.class, "blockMEBridgeReceiver");
        GameRegistry.registerBlock(assemblyMatrixBlock, "AssemblyMatrixBlock");
        GameRegistry.registerBlock(advancedAssemblyMatrixBlock, "AdvancedAssemblyMatrixBlock");
        GameRegistry.registerBlock(torcherino, ItemBlockTorcherino.class, "torcherino");
        GameRegistry.registerBlock(compressedTorcherino, ItemBlockTorcherino.class, "compressed_torcherino");
        GameRegistry
            .registerBlock(doubleCompressedTorcherino, ItemBlockTorcherino.class, "double_compressed_torcherino");
        if (Config.enableWirelessTorcherino) {
            GameRegistry.registerBlock(wirelessTorcherino, ItemBlockWirelessTorcherino.class, "wireless_torcherino");
            GameRegistry.registerBlock(
                compressedWirelessTorcherino,
                ItemBlockWirelessTorcherino.class,
                "compressed_wireless_torcherino");
            GameRegistry.registerBlock(
                doubleCompressedWirelessTorcherino,
                ItemBlockWirelessTorcherino.class,
                "double_compressed_wireless_torcherino");
        }
        GameRegistry.registerTileEntity(TileMEBridgeSender.class, "tileMEBridgeSender");
        GameRegistry.registerTileEntity(TileMEBridgeReceiver.class, "tileMEBridgeReceiver");
        GameRegistry.registerTileEntity(TileTorcherino.class, "gtnotgood.tile_torcherino");
        GameRegistry.registerTileEntity(TileCompressedTorcherino.class, "gtnotgood.tile_compressed_torcherino");
        GameRegistry
            .registerTileEntity(TileDoubleCompressedTorcherino.class, "gtnotgood.tile_double_compressed_torcherino");
        if (Config.enableWirelessTorcherino) {
            GameRegistry.registerTileEntity(TileWirelessTorcherino.class, "gtnotgood.tile_wireless_torcherino");
            GameRegistry.registerTileEntity(
                TileCompressedWirelessTorcherino.class,
                "gtnotgood.tile_compressed_wireless_torcherino");
            GameRegistry.registerTileEntity(
                TileDoubleCompressedWirelessTorcherino.class,
                "gtnotgood.tile_double_compressed_wireless_torcherino");
        }

        GTNGItemList.MEBridgeSender.set(new ItemStack(blockMEBridgeSender));
        GTNGItemList.MEBridgeReceiver.set(new ItemStack(blockMEBridgeReceiver));
        GTNGItemList.AssemblyMatrixBlock.set(new ItemStack(assemblyMatrixBlock));
        GTNGItemList.AdvancedAssemblyMatrixBlock.set(new ItemStack(advancedAssemblyMatrixBlock));
        GTNGItemList.Torcherino.set(new ItemStack(torcherino));
        GTNGItemList.CompressedTorcherino.set(new ItemStack(compressedTorcherino));
        GTNGItemList.DoubleCompressedTorcherino.set(new ItemStack(doubleCompressedTorcherino));
        if (Config.enableWirelessTorcherino) {
            GTNGItemList.WirelessTorcherino.set(new ItemStack(wirelessTorcherino));
            GTNGItemList.CompressedWirelessTorcherino.set(new ItemStack(compressedWirelessTorcherino));
            GTNGItemList.DoubleCompressedWirelessTorcherino.set(new ItemStack(doubleCompressedWirelessTorcherino));
        }
    }
}
