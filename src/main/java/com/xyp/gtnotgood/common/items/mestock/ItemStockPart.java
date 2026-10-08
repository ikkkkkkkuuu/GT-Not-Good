package com.xyp.gtnotgood.common.items.mestock;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.xyp.gtnotgood.common.items.GTNGItem;
import com.xyp.gtnotgood.common.parts.mestock.PartRequesterTerminal;
import com.xyp.gtnotgood.common.parts.mestock.PartThresholdExportBus;
import com.xyp.gtnotgood.common.parts.mestock.PartThresholdLevelEmitter;

import appeng.api.AEApi;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartItem;

/** Registered cable items share placement, while their stock behavior lives in independent parts. */
public final class ItemStockPart extends GTNGItem implements IPartItem {

    public enum Kind {
        ThresholdExportBus,
        ThresholdLevelEmitter,
        RequesterTerminal
    }

    private final Kind kind;

    public ItemStockPart(Kind kind) {
        super(kind == Kind.ThresholdExportBus ? "threshold_export_bus"
            : kind == Kind.ThresholdLevelEmitter ? "threshold_level_emitter" : "requester_terminal");
        this.kind = kind;
        if (kind == Kind.ThresholdExportBus) {
            // #tr item.threshold_export_bus.name
            // # ME Threshold Export Bus
            // # zh_CN ME 阈值输出总线
            setUnlocalizedName("threshold_export_bus");
        } else if (kind == Kind.ThresholdLevelEmitter) {
            // #tr item.threshold_level_emitter.name
            // # ME Threshold Level Emitter
            // # zh_CN ME 双阈值发信器
            setUnlocalizedName("threshold_level_emitter");
        } else {
            // #tr item.requester_terminal.name
            // # ME Requester Terminal
            // # zh_CN ME 自动请求终端
            setUnlocalizedName("requester_terminal");
        }
    }

    @Override
    public IPart createPartFromItemStack(ItemStack stack) {
        return switch (kind) {
            case ThresholdExportBus -> new PartThresholdExportBus(stack);
            case ThresholdLevelEmitter -> new PartThresholdLevelEmitter(stack);
            case RequesterTerminal -> new PartRequesterTerminal(stack);
        };
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        return AEApi.instance().partHelper().placeBus(stack, x, y, z, side, player, world);
    }

    @Override
    public int getSpriteNumber() {
        return 0;
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        if (kind == Kind.ThresholdExportBus) {
            // #tr tooltip.mestock.threshold
            // # Export above/below network stock thresholds; above mode preserves the reserve.
            // # zh_CN 按 ME 库存上下限输出；高于阈值时只输出超出部分。
            lines.add(StatCollector.translateToLocal("tooltip.mestock.threshold"));
        } else if (kind == Kind.ThresholdLevelEmitter) {
            // #tr tooltip.mestock.emitter
            // # Holds its signal between two thresholds. Items and fluids; exact matching.
            // # zh_CN 上下限之间保持红石状态；支持物品与流体精确匹配。
            lines.add(StatCollector.translateToLocal("tooltip.mestock.emitter"));
        } else {
            // #tr tooltip.mestock.terminal
            // # Configure and monitor loaded requesters on this ME network.
            // # zh_CN 集中配置和查看同一 ME 网络中已加载的自动请求器。
            lines.add(StatCollector.translateToLocal("tooltip.mestock.terminal"));
        }
    }
}
