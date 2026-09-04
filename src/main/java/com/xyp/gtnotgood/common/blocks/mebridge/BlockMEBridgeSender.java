package com.xyp.gtnotgood.common.blocks.mebridge;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.xyp.gtnotgood.common.mebridge.TileMEBridgeSender;

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
        return new String[] { "tooltip.mebridge.sender.0", "tooltip.mebridge.sender.1", "tooltip.mebridge.sender.2",
            "tooltip.mebridge.sender.3" };
    }

    // #tr tooltip.mebridge.sender.0
    // # §bBroadcasts a channel that receivers connect to.
    // # zh_CN §b广播一个频道,供接收端连接。
    // #tr tooltip.mebridge.sender.1
    // # §7Connect it to your main ME network with a cable.
    // # zh_CN §7用线缆把它接入你的主 ME 网络。
    // #tr tooltip.mebridge.sender.2
    // # §7Any number of receivers on the same channel join this network (cross-dimensional).
    // # zh_CN §7任意多个同频道的接收端会并入此网络(跨维度)。
    // #tr tooltip.mebridge.sender.3
    // # §eKeep its chunk loaded for the link to stay active.
    // # zh_CN §e需保持其区块常驻加载,连接才持续有效。
}
