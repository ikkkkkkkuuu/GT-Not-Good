package com.xyp.gtnotgood.common.flux;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

/** Explains the port's GT EU direction, automatic owner binding and configurable packet limits. */
public final class ItemBlockFluxConnector extends ItemBlock {

    public ItemBlockFluxConnector(Block block) {
        super(block);
    }

    /** Reset legacy portable settings on the server so old and newly crafted stacks can merge. */
    @Override
    public void onUpdate(ItemStack stack, net.minecraft.world.World world, net.minecraft.entity.Entity entity, int slot,
        boolean held) {
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
            // # Select an ME bridge channel; switch between restocking and automatic collection.
            // # zh_CN 选择 ME 网桥频道，可切换定量供货与自动回收。
            tooltip.add(StatCollector.translateToLocal("flux.tooltip.logistics"));
            return;
        }
        if (((BlockFluxConnector) field_150939_a).plug) {
            // #tr flux.tooltip.plug
            // # GT EU input -> GTNH wireless network
            // # zh_CN GT EU 输入 -> GTNH 无线电网
            tooltip.add(StatCollector.translateToLocal("flux.tooltip.plug"));
        } else {
            // #tr flux.tooltip.point
            // # GTNH wireless network -> GT EU output
            // # zh_CN GTNH 无线电网 -> GT EU 输出
            tooltip.add(StatCollector.translateToLocal("flux.tooltip.point"));
        }
        // #tr flux.tooltip.owner
        // # Automatically uses the placing player's GT wireless team
        // # zh_CN 自动连接放置者所属的 GT 无线电网团队
        tooltip.add(StatCollector.translateToLocal("flux.tooltip.owner"));
        // #tr flux.tooltip.settings
        // # Placement detects voltage once (fallback: 32 V), at 1 A; settings remain editable.
        // # zh_CN 放置时识别电压（无匹配时 32 V），默认 1 A；之后可手动调整。
        tooltip.add(StatCollector.translateToLocal("flux.tooltip.settings"));
        // #tr flux.tooltip.rating
        // # Throughput: voltage x amperage EU/t
        // # zh_CN 每 tick 吞吐上限：电压 × 安培数 EU
        tooltip.add(StatCollector.translateToLocal("flux.tooltip.rating"));
        if (!((BlockFluxConnector) field_150939_a).plug) {
            // #tr flux.tooltip.overvoltage
            // # GT overvoltage and cable amperage rules still apply
            // # zh_CN 仍遵循 GT 的过压与线缆安培数规则
            tooltip.add(StatCollector.translateToLocal("flux.tooltip.overvoltage"));
        }
    }
}
