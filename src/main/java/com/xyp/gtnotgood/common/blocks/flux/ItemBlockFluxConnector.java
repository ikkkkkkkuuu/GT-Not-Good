package com.xyp.gtnotgood.common.blocks.flux;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

/** Explains the port's GT EU direction, automatic owner binding and configurable packet limits. */
public final class ItemBlockFluxConnector extends ItemBlock {

    public ItemBlockFluxConnector(Block block) {
        super(block);
    }

    /** Reset legacy portable settings on the server so old and newly crafted stacks can merge. */
    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean held) {
        super.onUpdate(stack, world, entity, slot, held);
        if (!world.isRemote && world.getTotalWorldTime() % 20 == 0) {
            FluxDropData.normalize(stack, field_150939_a instanceof BlockFluxLogistics);
        }
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        if (field_150939_a instanceof BlockFluxLogistics) {
            // #tr flux.tooltip.logistics
            // # Supplies or collects items and fluids through a selected ME bridge channel.
            // # zh_CN 通过选中的 ME 网桥频道供应或回收物品与流体。
            tooltip.add(StatCollector.translateToLocal("flux.tooltip.logistics"));
            // #tr flux.tooltip.logistics_configure
            // # Right-click to select the channel and switch between supply and collection.
            // # zh_CN 右键选择频道，并切换定量供货或自动回收。
            tooltip.add(StatCollector.translateToLocal("flux.tooltip.logistics_configure"));
            return;
        }
        if (((BlockFluxConnector) field_150939_a).plug) {
            // #tr flux.tooltip.plug
            // # Transfers incoming GT EU to the GTNH wireless energy network.
            // # zh_CN 将输入的 GT EU 存入 GTNH 无线电网。
            tooltip.add(StatCollector.translateToLocal("flux.tooltip.plug"));
        } else {
            // #tr flux.tooltip.point
            // # Supplies GT EU from the GTNH wireless energy network.
            // # zh_CN 从 GTNH 无线电网取电并输出 GT EU。
            tooltip.add(StatCollector.translateToLocal("flux.tooltip.point"));
        }
        // #tr flux.tooltip.owner
        // # Uses the placing player's GT wireless team balance.
        // # zh_CN 使用放置者所属 GT 无线团队的电量。
        tooltip.add(StatCollector.translateToLocal("flux.tooltip.owner"));
        // #tr flux.tooltip.settings
        // # Detects voltage on placement, or uses 32 V if unknown. Starts at 1 A; right-click to edit.
        // # zh_CN 放置时识别电压，无法识别时采用 32 V；默认 1 A，右键调整。
        tooltip.add(StatCollector.translateToLocal("flux.tooltip.settings"));
        // #tr flux.tooltip.rating
        // # Transfer limit: voltage x amperage EU/t.
        // # zh_CN 传输上限：电压 × 安培数 EU/t。
        tooltip.add(StatCollector.translateToLocal("flux.tooltip.rating"));
        if (!((BlockFluxConnector) field_150939_a).plug) {
            // #tr flux.tooltip.overvoltage
            // # Output obeys GT overvoltage and cable amperage limits.
            // # zh_CN 输出仍受 GT 过压与线缆安培数限制。
            tooltip.add(StatCollector.translateToLocal("flux.tooltip.overvoltage"));
        }
    }
}
