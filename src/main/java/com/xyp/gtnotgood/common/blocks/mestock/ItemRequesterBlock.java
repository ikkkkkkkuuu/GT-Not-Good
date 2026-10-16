package com.xyp.gtnotgood.common.blocks.mestock;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

public final class ItemRequesterBlock extends ItemBlock {

    public ItemRequesterBlock(Block block) {
        super(block);
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        // #tr tooltip.mestock.requester
        // # Automatically crafts missing stock for up to 5 item or fluid targets.
        // # zh_CN 自动合成短缺库存，最多设置 5 种物品或流体。
        lines.add(StatCollector.translateToLocal("tooltip.mestock.requester"));
        // #tr tooltip.mestock.requester_configure
        // # Right-click to set stock targets and batch sizes. Missing resources must be craftable.
        // # zh_CN 右键设置目标库存与每单数量，短缺资源须有可用合成配方。
        lines.add(StatCollector.translateToLocal("tooltip.mestock.requester_configure"));
        // #tr tooltip.mestock.requester_budget
        // # Uses one channel. Requesters share a limit of 2 recipe calculations per network.
        // # zh_CN 占用一个频道，同网请求器最多同时进行 2 次配方计算。
        lines.add(StatCollector.translateToLocal("tooltip.mestock.requester_budget"));
    }
}
