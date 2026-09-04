package com.xyp.gtnotgood.common.blocks.mebridge;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.xyp.gtnotgood.common.mebridge.TileMEBridgeReceiver;

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
        return new String[] { "tooltip.mebridge.receiver.0", "tooltip.mebridge.receiver.1",
            "tooltip.mebridge.receiver.2", "tooltip.mebridge.receiver.3" };
    }

    // #tr tooltip.mebridge.receiver.0
    // # §bConnects to a channel broadcast by a Sender.
    // # zh_CN §b连接到发起端广播的频道。
    // #tr tooltip.mebridge.receiver.1
    // # §7Becomes part of the Sender's ME network across dimensions.
    // # zh_CN §7跨维度并入发起端所在的 ME 网络。
    // #tr tooltip.mebridge.receiver.2
    // # §7Right-click to type or pick a channel.
    // # zh_CN §7右键输入或从列表选择频道。
    // #tr tooltip.mebridge.receiver.3
    // # §eKeep both ends' chunks loaded for a stable link.
    // # zh_CN §e两端区块需常驻加载以保持连接稳定。
}
