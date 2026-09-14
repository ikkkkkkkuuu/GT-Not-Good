package com.xyp.gtnotgood.common.user;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.item.InvWrapper;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;

/** Compact inventory screen; all configuration changes are applied by server-side button handlers. */
final class MechanicalUserGui {

    private MechanicalUserGui() {}

    static ModularPanel build(TileMechanicalUser tile, PanelSyncManager sync) {
        IntSyncValue mode = new IntSyncValue(() -> tile.mode);
        BooleanSyncValue left = new BooleanSyncValue(() -> tile.leftClick);
        BooleanSyncValue first = new BooleanSyncValue(() -> tile.firstSlotOnly);
        IntSyncValue interval = new IntSyncValue(tile::interval);
        sync.syncValue("mode", mode);
        sync.syncValue("left", left);
        sync.syncValue("first", first);
        sync.syncValue("interval", interval);
        sync.registerSlotGroup("buffer", 3);
        sync.registerSlotGroup("upgrade", 1);
        InvWrapper items = new InvWrapper(tile);
        ModularPanel panel = ModularPanel.defaultPanel("mechanical_user", 194, 232);
        panel.child(
            IKey.lang("tile.mechanical_user.name")
                .asWidget()
                .pos(8, 6));
        for (int i = 0; i < 9; i++) {
            panel.child(
                new ItemSlot().slot(new ModularSlot(items, i).slotGroup("buffer"))
                    .pos(70 + i % 3 * 18, 20 + i / 3 * 18));
        }
        panel.child(
            new ItemSlot().slot(
                new ModularSlot(items, 9).slotGroup("upgrade")
                    .filter(stack -> stack.getItem() instanceof ItemUserSpeedUpgrade))
                .pos(151, 38));
        // #tr gui.mechanical_user.upgrades
        // # Speed
        // # zh_CN 加速
        panel.child(
            IKey.lang("gui.mechanical_user.upgrades")
                .asWidget()
                .pos(148, 22));
        panel.child(
            new ButtonWidget<>().pos(8, 76)
                .size(178, 18)
                .overlay(IKey.dynamic(() -> modeName(mode.getIntValue())))
                .syncHandler(
                    new com.cleanroommc.modularui.value.sync.InteractionSyncHandler().setOnMousePressed(button -> {
                        if (!button.isClient()) {
                            tile.mode = (tile.mode + 1) % 6;
                            tile.markDirty();
                        }

                    })));
        panel.child(
            new ButtonWidget<>().pos(8, 96)
                .size(178, 18)
                .overlay(IKey.dynamic(() -> clickName(left.getBoolValue())))
                .syncHandler(
                    new com.cleanroommc.modularui.value.sync.InteractionSyncHandler().setOnMousePressed(button -> {
                        if (!button.isClient()) {
                            tile.leftClick = !tile.leftClick;
                            tile.markDirty();
                        }

                    })));
        panel.child(
            new ButtonWidget<>().pos(8, 116)
                .size(178, 18)
                .overlay(IKey.dynamic(() -> slotName(first.getBoolValue())))
                .syncHandler(
                    new com.cleanroommc.modularui.value.sync.InteractionSyncHandler().setOnMousePressed(button -> {
                        if (!button.isClient()) {
                            tile.firstSlotOnly = !tile.firstSlotOnly;
                            tile.markDirty();
                        }

                    })));
        // #tr gui.mechanical_user.interval
        // # Interval: %1$s ticks
        // # zh_CN 间隔：%1$s tick
        panel.child(
            IKey.dynamic(
                () -> StatCollector.translateToLocalFormatted("gui.mechanical_user.interval", interval.getIntValue()))
                .asWidget()
                .pos(8, 137));
        panel.child(SlotGroupWidget.playerInventory(7, true));
        return panel;
    }

    private static String clickName(boolean left) {
        if (left) {
            // #tr gui.mechanical_user.left
            // # Left Click
            // # zh_CN 模拟左键
            return StatCollector.translateToLocal("gui.mechanical_user.left");
        }
        // #tr gui.mechanical_user.right
        // # Right Click
        // # zh_CN 模拟右键
        return StatCollector.translateToLocal("gui.mechanical_user.right");
    }

    private static String slotName(boolean first) {
        if (first) {
            // #tr gui.mechanical_user.first
            // # Upper Left Slot Only
            // # zh_CN 仅使用左上角槽位
            return StatCollector.translateToLocal("gui.mechanical_user.first");
        }
        // #tr gui.mechanical_user.random
        // # Random Slot
        // # zh_CN 随机非空槽位
        return StatCollector.translateToLocal("gui.mechanical_user.random");
    }

    private static String modeName(int mode) {
        switch (mode) {
            case 1:
                // #tr gui.mechanical_user.place
                // # Place Block
                // # zh_CN 放置方块
                return StatCollector.translateToLocal("gui.mechanical_user.place");
            case 2:
                // #tr gui.mechanical_user.on_block
                // # Use Item on Block
                // # zh_CN 在方块上使用物品
                return StatCollector.translateToLocal("gui.mechanical_user.on_block");
            case 3:
                // #tr gui.mechanical_user.activate
                // # Activate Block with Item
                // # zh_CN 用物品激活方块
                return StatCollector.translateToLocal("gui.mechanical_user.activate");
            case 4:
                // #tr gui.mechanical_user.item
                // # Use Item
                // # zh_CN 使用物品
                return StatCollector.translateToLocal("gui.mechanical_user.item");
            case 5:
                // #tr gui.mechanical_user.entity
                // # Entity
                // # zh_CN 实体交互
                return StatCollector.translateToLocal("gui.mechanical_user.entity");
            default:
                // #tr gui.mechanical_user.generic
                // # Generic Click
                // # zh_CN 一般点击
                return StatCollector.translateToLocal("gui.mechanical_user.generic");
        }
    }
}
