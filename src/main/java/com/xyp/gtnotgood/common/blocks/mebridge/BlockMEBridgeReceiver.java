package com.xyp.gtnotgood.common.blocks.mebridge;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** 跨维度 ME 网桥 - 接收端方块（选频道连入）。 */
public class BlockMEBridgeReceiver extends BlockMEBridgeBase {

    // #tr tile.mebridge.receiver.name
    // # ME Bridge Receiver
    // # zh_CN ME 网桥 - 接收端
    public BlockMEBridgeReceiver() {
        super("mebridge.receiver", "MEBridgeReceiver");
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileMEBridgeReceiver();
    }

    @Override
    public String[] getTooltipKeys() {
        return new String[] {
            // #tr tooltip.mebridge.receiver.0
            // # §bReceives an ME network from a sender channel.
            // # zh_CN §b通过发起端频道接入 ME 网络。
            "tooltip.mebridge.receiver.0",
            // #tr tooltip.mebridge.receiver.1
            // # §7Joins the sender's network across dimensions.
            // # zh_CN §7可跨维度并入发起端所在的网络。
            "tooltip.mebridge.receiver.1",
            // #tr tooltip.mebridge.receiver.2
            // # §7Right-click to select a sender channel.
            // # zh_CN §7右键选择发起端频道。
            "tooltip.mebridge.receiver.2",
            // #tr tooltip.mebridge.receiver.3
            // # §eKeep both endpoints' chunks loaded to maintain the connection.
            // # zh_CN §e连接两端的区块均须保持加载。
            "tooltip.mebridge.receiver.3" };
    }
}
