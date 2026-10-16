package com.xyp.gtnotgood.common.blocks.mebridge;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** 跨维度 ME 网桥 - 发起端方块（写频道名广播）。 */
public class BlockMEBridgeSender extends BlockMEBridgeBase {

    // #tr tile.mebridge.sender.name
    // # ME Bridge Sender
    // # zh_CN ME 网桥 - 发起端
    public BlockMEBridgeSender() {
        super("mebridge.sender", "MEBridgeSender");
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileMEBridgeSender();
    }

    @Override
    public String[] getTooltipKeys() {
        return new String[] {
            // #tr tooltip.mebridge.sender.0
            // # §bShares its ME network with receivers across dimensions.
            // # zh_CN §b通过频道与接收端跨维度共享 ME 网络。
            "tooltip.mebridge.sender.0",
            // #tr tooltip.mebridge.sender.1
            // # §7Connect to the main ME network, then right-click to name the channel.
            // # zh_CN §7接入主 ME 网络，右键设置频道名称。
            "tooltip.mebridge.sender.1",
            // #tr tooltip.mebridge.sender.2
            // # §7Multiple receivers can join the same sender channel.
            // # zh_CN §7同一发起端频道可连接多个接收端。
            "tooltip.mebridge.sender.2",
            // #tr tooltip.mebridge.sender.3
            // # §eKeep the sender and receiver chunks loaded to maintain the connection.
            // # zh_CN §e发起端与接收端的区块均须保持加载。
            "tooltip.mebridge.sender.3" };
    }
}
