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
            // # Exports items or fluids according to their stock in the ME network.
            // # zh_CN 根据 ME 网络中的库存，向相邻容器输出物品或流体。
            lines.add(StatCollector.translateToLocal("tooltip.mestock.threshold"));
        } else if (kind == Kind.ThresholdLevelEmitter) {
            // #tr tooltip.mestock.emitter
            // # Controls redstone with two stock thresholds. Keeps its state between them.
            // # zh_CN 根据库存上下限控制红石信号，处于两阈值之间时保持状态。
            lines.add(StatCollector.translateToLocal("tooltip.mestock.emitter"));
        } else {
            // #tr tooltip.mestock.terminal
            // # Configures and monitors loaded requesters on the same ME network.
            // # zh_CN 配置并监控同一 ME 网络中已加载的自动请求器。
            lines.add(StatCollector.translateToLocal("tooltip.mestock.terminal"));
        }
        // #tr tooltip.mestock.configure
        // # Right-click to configure.
        // # zh_CN 右键打开配置界面。
        lines.add(StatCollector.translateToLocal("tooltip.mestock.configure"));
        if (kind == Kind.ThresholdExportBus) {
            // #tr tooltip.mestock.threshold_modes
            // # Above mode keeps the reserve. Below mode exports while stock is at or below the threshold.
            // # zh_CN 高于模式只输出超出部分；低于模式在库存不高于阈值时输出。
            lines.add(StatCollector.translateToLocal("tooltip.mestock.threshold_modes"));
        } else if (kind == Kind.ThresholdLevelEmitter) {
            // #tr tooltip.mestock.emitter_match
            // # Monitors one exact item or fluid type. The lower threshold must not exceed the upper.
            // # zh_CN 精确匹配一种物品或流体，下限不能高于上限。
            lines.add(StatCollector.translateToLocal("tooltip.mestock.emitter_match"));
        }
    }
}
