package com.xyp.gtnotgood.common.mestock;

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
        // # Keeps five item/fluid targets in ME storage by automatically ordering missing resources.
        // # zh_CN 自动下单补齐 ME 库存；可设置 5 种物品或流体。
        lines.add(StatCollector.translateToLocal("tooltip.mestock.requester"));
        // #tr tooltip.mestock.requester_budget
        // # Shared network scheduler; at most two calculations at once. One channel.
        // # zh_CN 同网共享调度，每网最多同时计算两单；占用一个频道。
        lines.add(StatCollector.translateToLocal("tooltip.mestock.requester_budget"));
    }
}
