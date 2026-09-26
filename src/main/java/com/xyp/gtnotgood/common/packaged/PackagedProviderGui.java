// SPDX-License-Identifier: LGPL-3.0-only
// GTNG port modifications (c) 2026 GTNG contributors.
// Upstream authors and exact source mappings: META-INF/ae2lt-port/CODE_PORT_NOTES.md
package com.xyp.gtnotgood.common.packaged;

import java.util.function.Supplier;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.item.InvWrapper;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.xyp.gtnotgood.utils.enums.ModList;

/**
 * LGPL-3.0: layout adapted from AE2LTPP PackagedPatternProviderScreen/menu/style and AE2 UpgradesPanel.
 * Original authors and exact revisions are recorded in META-INF/ae2lt-port and reference/UPSTREAM_PORT_NOTES.md.
 * The 176x245 source crop, 9x4 grid and external core slot are preserved without scaling the atlas.
 */
public final class PackagedProviderGui {

    private static final UITexture FRAME = texture("ex_pattern_provider", 256, 256, 0, 0, 176, 245);
    private static final UITexture CORE_FRAME = texture("extra_panels", 128, 128, 0, 0, 28, 30);
    private static final UITexture BUTTON = texture("states", 256, 256, 176, 128, 18, 20);
    private static final UITexture HOVER = texture("states", 256, 256, 212, 128, 18, 20);

    private PackagedProviderGui() {}

    private static UITexture texture(String name, int w, int h, int x, int y, int width, int height) {
        return UITexture.builder()
            .location(ModList.ModIds.GT_NOT_GOOD, "gui/packaged/" + name)
            .imageSize(w, h)
            .subAreaXYWH(x, y, width, height)
            .build();
    }

    private static UITexture icon(String name) {
        return texture(name, 16, 16, 0, 0, 16, 16);
    }

    static ModularPanel build(TilePackagedProvider tile, PanelSyncManager sync) {
        BooleanSyncValue auto = new BooleanSyncValue(() -> tile.autoReturn);
        BooleanSyncValue essentia = new BooleanSyncValue(() -> tile.networkEssentia);
        BooleanSyncValue infusionCore = new BooleanSyncValue(tile::hasInfusionCore);
        sync.syncValue("infusionCore", infusionCore);
        sync.syncValue("networkEssentia", essentia);
        IntSyncValue essentiaSpeed = new IntSyncValue(() -> tile.essentiaSpeed);
        sync.syncValue("essentiaSpeed", essentiaSpeed);
        IntSyncValue jobs = new IntSyncValue(tile::queuedJobs);
        IntSyncValue count = new IntSyncValue(() -> tile.targets.size());
        IntSyncValue priority = new IntSyncValue(() -> tile.priority);
        StringSyncValue altar = new StringSyncValue(() -> tile.altarStatus.key);
        StringSyncValue missingAspect = new StringSyncValue(() -> tile.arcaneMissingAspect);
        IntSyncValue missingUnits = new IntSyncValue(() -> tile.arcaneMissingUnits);
        sync.syncValue("arcaneMissingAspect", missingAspect);
        sync.syncValue("arcaneMissingUnits", missingUnits);
        IntSyncValue lock = new IntSyncValue(() -> tile.craftingLock.ordinal());
        BooleanSyncValue locked = new BooleanSyncValue(tile::craftingLocked);
        BooleanSyncValue visible = new BooleanSyncValue(() -> tile.terminalVisible);
        sync.syncValue("lock", lock);
        sync.syncValue("locked", locked);
        sync.syncValue("terminalVisible", visible);
        sync.syncValue("altar", altar);
        sync.syncValue("return", auto);
        sync.syncValue("jobs", jobs);
        sync.syncValue("targets", count);
        sync.syncValue("priority", priority);
        sync.registerSlotGroup("patterns", 9);
        sync.registerSlotGroup("returns", 9);
        sync.registerSlotGroup("core", 1);
        var targetsPanel = sync.syncedPanel("targets_panel", true, (manager, handler) -> targetPanel(tile, manager));
        var priorityPanel = sync
            .syncedPanel("priority_panel", true, (manager, handler) -> priorityPanel(tile, manager));
        InvWrapper inventory = new InvWrapper(tile);
        ModularPanel panel = ModularPanel.defaultPanel("wireless_packaged_provider", 176, 245)
            .background(FRAME)
            .disableHoverBackground();
        panel.child(
            IKey.lang("tile.wireless_packaged_pattern_provider.name")
                .color(0xff404040)
                .asWidget()
                .pos(8, 6)
                .size(160, 9));
        // #tr gui.packaged.patterns
        // # Patterns
        // # zh_CN 样板
        panel.child(
            IKey.lang("gui.packaged.patterns")
                .color(0xff404040)
                .asWidget()
                .pos(8, 31));
        // #tr gui.packaged.returns
        // # Return Inventory
        // # zh_CN 回收物品栏
        panel.child(
            IKey.lang("gui.packaged.returns")
                .color(0xff404040)
                .asWidget()
                .pos(8, 116));
        panel.child(
            IKey.lang("container.inventory")
                .color(0xff404040)
                .asWidget()
                .pos(8, 150));
        for (int slot = 0; slot < TilePackagedProvider.PATTERNS; slot++) {
            panel.child(new ItemSlot().slot(new ModularSlot(inventory, slot) {

                @Override
                public int getItemStackLimit(net.minecraft.item.ItemStack stack) {
                    return 1;
                }
            }.slotGroup("patterns")
                .filter(
                    stack -> stack != null
                        && stack.getItem() instanceof appeng.api.implementations.ICraftingPatternItem))
                .background(IDrawable.EMPTY)
                .pos(7 + slot % 9 * 18, 41 + slot / 9 * 18));
        }
        for (int slot = 36; slot < 45; slot++) {
            panel.child(
                new ItemSlot().slot(
                    new ModularSlot(inventory, slot).slotGroup("returns")
                        .canPut(false))
                    .background(IDrawable.EMPTY)
                    .pos(7 + (slot - 36) * 18, 126));
        }
        panel.child(
            CORE_FRAME.asWidget()
                .pos(172, 72)
                .size(28, 30)
                .excludeAreaInRecipeViewer());
        panel.child(new ItemSlot().slot(new ModularSlot(inventory, TilePackagedProvider.CORE) {

            @Override
            public int getItemStackLimit(net.minecraft.item.ItemStack stack) {
                return 1;
            }

            @Override
            public boolean canTakeStack(EntityPlayer player) {
                return tile.queuedJobs() == 0 && super.canTakeStack(player);
            }
        }.slotGroup("core")
            .canDragInto(false)
            .filter(stack -> stack.getItem() instanceof ItemPackagedCore))
            .background(IDrawable.EMPTY)
            // The original atlas slot begins at U=0; its five-pixel padding is vertical only.
            .pos(172, 77)
            // #tr gui.packaged.core_slot
            // # Packaged Core (locked while jobs are active)
            // # zh_CN 封包核心（有任务时锁定）
            .tooltip(
                t -> t.addLine(IKey.lang("gui.packaged.core_slot"))
                    .addLine(
                        IKey.dynamic(
                            () -> missingUnits.getIntValue() > 0
                                ? missingAspect.getValue() + ": -" + missingUnits.getIntValue()
                                : ""))
                    .addLine(IKey.dynamic(() -> StatCollector.translateToLocal(altar.getValue())))));
        panel.child(
            SlotGroupWidget.playerInventory((index, slot) -> slot.background(IDrawable.EMPTY))
                .pos(7, 160));

        IDrawable lockIcon = (context, x, y, w, h,
            theme) -> texture("states", 256, 256, PackagedCraftingLock.read(lock.getIntValue()).iconX, 0, 16, 16)
                .draw(context, x, y, w, h, theme);
        panel.child(
            toolbar(
                -18,
                1,
                lockIcon,
                () -> StatCollector.translateToLocal(PackagedCraftingLock.read(lock.getIntValue()).key),
                () -> {}).name("crafting_lock")
                    .syncHandler(new InteractionSyncHandler().setOnMousePressed(mouse -> {
                        if (!mouse.isClient() && tile.canConfigure(sync.getPlayer())) {
                            tile.setCraftingLock(
                                PackagedCraftingLock
                                    .read(tile.craftingLock.ordinal() + (mouse.mouseButton == 1 ? -1 : 1)));
                        }
                    })));
        IDrawable terminalIcon = (context, x, y, w, h,
            theme) -> texture("states", 256, 256, visible.getBoolValue() ? 64 : 80, 80, 16, 16)
                .draw(context, x, y, w, h, theme);
        panel.child(toolbar(-18, 23, terminalIcon, () -> {
            // #tr gui.packaged.terminal
            // # Show in Pattern Access Terminal: %s
            // # zh_CN 在样板终端中显示：%s
            return StatCollector.translateToLocalFormatted("gui.packaged.terminal", state(visible.getBoolValue()));
        }, () -> {
            if (tile.canConfigure(sync.getPlayer())) {
                tile.terminalVisible = !tile.terminalVisible;
                tile.markDirty();
            }
        }).name("terminal_visible"));
        panel.child(
            new ButtonWidget<>().pos(5, 17)
                .size(160, 12)
                .background(IDrawable.EMPTY)
                // #tr gui.packaged.locked
                // # Crafting locked - click to reset
                // # zh_CN 合成已锁定，点击重置
                .overlay(
                    IKey.lang("gui.packaged.locked")
                        .color(0xff404040))
                .setEnabledIf(widget -> locked.getBoolValue())
                .syncHandler(
                    new InteractionSyncHandler().setOnMousePressed(
                        mouse -> {
                            if (!mouse.isClient() && tile.canConfigure(sync.getPlayer())) tile.resetCraftingLock();
                        })));
        panel.child(toolbar(-18, 45, icon("frequency_select"), () -> {
            // #tr gui.packaged.targets
            // # Connected targets: %s
            // # zh_CN 已连接目标：%s
            return StatCollector.translateToLocalFormatted("gui.packaged.targets", count.getIntValue());
        }, () -> {}).name("targets")
            .onMousePressed(button -> {
                if (button != 0) return false;
                targetsPanel.openPanel();
                panel.setEnabled(false);
                return true;
            }));
        IDrawable returnIcon = (context, x, y, w, h,
            theme) -> icon(auto.getBoolValue() ? "auto_input_on" : "auto_input_off").draw(context, x, y, w, h, theme);
        panel.child(toolbar(-18, 67, returnIcon, () -> {
            // #tr gui.packaged.auto_return
            // # Auto return: %s | Active jobs: %s
            // # zh_CN 自动回收：%s | 进行中：%s
            return StatCollector
                .translateToLocalFormatted("gui.packaged.auto_return", state(auto.getBoolValue()), jobs.getIntValue());
        }, () -> {
            if (tile.canConfigure(sync.getPlayer())) {
                tile.autoReturn = !tile.autoReturn;
                tile.markDirty();
            }
        }));
        panel.child(toolbar(-18, 89, icon("frequency_connect"), () -> {
            if (essentia.getBoolValue()) {
                // #tr gui.packaged.essentia_ae
                // # AE essentia: %sx | Left: source; right: speed (idle only)
                // # zh_CN AE 源质：%s 倍 | 左键切来源，右键调速（空闲时）
                return StatCollector.translateToLocalFormatted("gui.packaged.essentia_ae", essentiaSpeed.getIntValue());
            }
            // #tr gui.packaged.essentia_altar
            // # Essentia: altar supply (click for AE; requires Thaumic Energistics)
            // # zh_CN 源质：祭坛供给（点击切换 AE；需神秘能源）
            return StatCollector.translateToLocal("gui.packaged.essentia_altar");
        }, () -> {}).name("essentia_mode")
            .setEnabledIf(widget -> infusionCore.getBoolValue())
            .syncHandler(new InteractionSyncHandler().setOnMousePressed(mouse -> {
                if (mouse.isClient() || !tile.canConfigure(sync.getPlayer())) return;
                if (mouse.mouseButton == 0) tile.setNetworkEssentia(!tile.networkEssentia);
                else if (mouse.mouseButton == 1) tile.cycleEssentiaSpeed();
            })));
        panel.child(toolbar(152, -5, texture("states", 256, 256, 144, 64, 16, 16), () -> {
            // #tr gui.packaged.priority
            // # Priority: %s
            // # zh_CN 优先级：%s
            return StatCollector.translateToLocalFormatted("gui.packaged.priority", priority.getIntValue());
        }, () -> {}).name("priority")
            .size(20, 20)
            .background(texture("states", 256, 256, 160, 192, 20, 20))
            .hoverBackground(texture("states", 256, 256, 160, 224, 22, 22))
            .onMousePressed(mouse -> {
                priorityPanel.openPanel();
                panel.setEnabled(false);
                return true;
            }));
        return panel;
    }

    /** Toolbar art comes from the original AE2 atlas; no generic MUI button is drawn behind it. */
    private static ButtonWidget<?> toolbar(int x, int y, IDrawable icon, Supplier<String> tooltip, Runnable action) {
        return new ButtonWidget<>().pos(x, y)
            .size(18, 20)
            .background(BUTTON)
            .hoverBackground(HOVER)
            .overlay((context, px, py, w, h, theme) -> icon.draw(context, px + 1, py + 1, 16, 16, theme))
            .hoverOverlay((context, px, py, w, h, theme) -> icon.draw(context, px + 1, py + 2, 16, 16, theme))
            .excludeAreaInRecipeViewer()
            .tooltip(t -> t.addLine(IKey.dynamic(tooltip)))
            .syncHandler(
                new InteractionSyncHandler().setOnMousePressed(mouse -> { if (!mouse.isClient()) action.run(); }));
    }

    private static String state(boolean enabled) {
        if (enabled) {
            // #tr gui.packaged.enabled
            // # Enabled
            // # zh_CN 启用
            return StatCollector.translateToLocal("gui.packaged.enabled");
        }
        // #tr gui.packaged.disabled
        // # Disabled
        // # zh_CN 关闭
        return StatCollector.translateToLocal("gui.packaged.disabled");
    }

    /** AE2 19.2.17 priority page geometry and number-entry step sizes, adapted to MUI synchronization. */
    private static ModularPanel priorityPanel(TilePackagedProvider tile, PanelSyncManager sync) {
        ModularPanel panel = ModularPanel.defaultPanel("packaged_priority", 176, 125)
            .background(texture("priority", 256, 256, 0, 0, 176, 125));
        addReturnTab(panel, tile, 150);
        IntSyncValue priority = new IntSyncValue(
            () -> tile.priority,
            value -> { if (tile.canConfigure(sync.getPlayer())) tile.setPriority(value); }).allowC2S();
        sync.syncValue("priority", priority);
        // #tr gui.packaged.priority_title
        // # Priority
        // # zh_CN 优先级
        panel.child(
            IKey.lang("gui.packaged.priority_title")
                .color(0xff404040)
                .asWidget()
                .pos(8, 6));
        panel.child(
            new com.cleanroommc.modularui.widgets.textfield.TextFieldWidget().pos(60, 55)
                .size(61, 12)
                .background(IDrawable.EMPTY)
                .setTextColor(0xffffffff)
                .value(priority)
                .numbersInt(Integer.MIN_VALUE, Integer.MAX_VALUE));
        int[] x = { 20, 48, 82, 120 };
        int[] widths = { 22, 28, 32, 38 };
        int[] steps = { 1, 10, 100, 1000 };
        int[] alternate = { 1, 8, 64, 512 };
        UITexture normal = UITexture.builder()
            .location(ModList.ModIds.GT_NOT_GOOD, "gui/packaged/button")
            .imageSize(200, 20)
            .adaptable(3)
            .build();
        UITexture hover = UITexture.builder()
            .location(ModList.ModIds.GT_NOT_GOOD, "gui/packaged/button_highlighted")
            .imageSize(200, 20)
            .adaptable(3)
            .build();
        for (int row = 0; row < 2; row++) for (int col = 0; col < 4; col++) {
            final int direction = row == 0 ? 1 : -1;
            final int step = col;
            panel.child(
                new ButtonWidget<>().name("priority_" + row + "_" + col)
                    .pos(x[col], row == 0 ? 30 : 72)
                    .size(widths[col], 20)
                    .background(normal)
                    .hoverBackground(hover)
                    .overlay(IKey.str((row == 0 ? "+" : "-") + steps[col]))
                    .syncHandler(new InteractionSyncHandler().setOnMousePressed(mouse -> {
                        if (!mouse.isClient() && tile.canConfigure(sync.getPlayer())) {
                            long value = (long) tile.priority
                                + direction * (mouse.shift || mouse.ctrl ? alternate[step] : steps[step]);
                            tile.setPriority((int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value)));
                        }
                    })));
        }
        return panel;
    }

    /** A bound-target list uses the upstream five-row background; active lanes cannot be removed. */
    private static ModularPanel targetPanel(TilePackagedProvider tile, PanelSyncManager sync) {
        ModularPanel panel = ModularPanel.defaultPanel("packaged_targets", 195, 157)
            .background(texture("wireless_overloaded_list", 256, 256, 0, 0, 195, 157));
        addReturnTab(panel, tile, 173);
        final int[] page = { 0 };
        IntSyncValue pageValue = new IntSyncValue(() -> page[0]);
        IntSyncValue pages = new IntSyncValue(() -> tile.targets.size());
        sync.syncValue("page", pageValue);
        sync.syncValue("pages", pages);
        // #tr gui.packaged.target_title
        // # Connected Targets
        // # zh_CN 已连接目标
        panel.child(
            IKey.lang("gui.packaged.target_title")
                .color(0xff404040)
                .asWidget()
                .pos(9, 9));
        for (int row = 0; row < 5; row++) {
            final int offset = row;
            StringSyncValue label = new StringSyncValue(() -> {
                int index = page[0] + offset;
                if (index >= tile.targets.size()) return "";
                PackagedTarget target = tile.targets.get(index);
                return target.x + ", " + target.y + ", " + target.z + (tile.busy(target) ? " *" : "");
            });
            sync.syncValue("target" + row, label);
            panel.child(
                new ButtonWidget<>().name("target_row_" + row)
                    .pos(9, 38 + row * 21)
                    .size(160, 20)
                    .background(texture("wireless_overloaded_list", 256, 256, 0, 158, 160, 20))
                    .hoverBackground(texture("wireless_overloaded_list", 256, 256, 0, 180, 160, 20))
                    .overlay(
                        IKey.dynamic(label::getValue)
                            .color(0xff404040))
                    .setEnabledIf(
                        widget -> !label.getValue()
                            .isEmpty())
                    .tooltip(t -> {
                        // #tr gui.packaged.remove_target
                        // # Right click: unlink. Shift-right click: release job. Cancel AE requests separately.
                        // # zh_CN 右键断开；Shift右键中断并释放任务，需另行取消AE订单。
                        t.addLine(IKey.lang("gui.packaged.remove_target"));
                        // #tr gui.packaged.arcane_record
                        // # Arcane core: left-click to record this table; bring a blank pattern.
                        // # zh_CN 奥术核心：左击录入此工作台配方，需携带空白样板。
                        t.addLine(IKey.lang("gui.packaged.arcane_record"));
                        // #tr gui.packaged.interrupt_materials
                        // # Assembly lines stop. Consumed inputs are lost; clear leftovers before restarting.
                        // # zh_CN 装配线停机，已消耗材料不退还；重启前清理剩余材料。
                        t.addLine(IKey.lang("gui.packaged.interrupt_materials"));
                    })
                    .syncHandler(new InteractionSyncHandler().setOnMousePressed(mouse -> {
                        if (!mouse.isClient() && mouse.mouseButton == 0
                            && tile.canConfigure(sync.getPlayer())
                            && ModList.Thaumcraft.isModLoaded()) {
                            ArcaneWorkbenchPatterns.capture(tile, sync.getPlayer(), page[0] + offset);
                        }
                        if (!mouse.isClient() && mouse.mouseButton == 1 && tile.canConfigure(sync.getPlayer())) {
                            if (mouse.shift) {
                                boolean released = tile.releaseInterrupted(page[0] + offset);
                                // #tr gui.packaged.recovery_result
                                // # Interrupted: %s. Remaining items stay at the target; cancel its AE requests.
                                // # zh_CN 中断结果：%s。剩余材料保留在目标内，请取消对应AE订单。
                                sync.getPlayer()
                                    .addChatMessage(
                                        new net.minecraft.util.ChatComponentTranslation(
                                            "gui.packaged.recovery_result",
                                            released));
                            } else tile.removeTarget(page[0] + offset);
                            page[0] = Math.min(page[0], Math.max(0, tile.targets.size() - 5));
                        }
                    })));
        }
        var scroll = new com.cleanroommc.modularui.value.sync.DoubleSyncValue(
            () -> page[0],
            value -> page[0] = Math.max(0, Math.min(Math.max(0, tile.targets.size() - 5), (int) Math.round(value))))
                .allowC2S();
        sync.syncValue("scroll", scroll);
        panel.child(
            new com.cleanroommc.modularui.widgets.SliderWidget().name("target_scroll")
                .value(scroll)
                .setAxis(com.cleanroommc.modularui.api.GuiAxis.Y)
                .sliderSize(12, 15)
                .sliderTexture(
                    (context, x, y, w, h,
                        theme) -> texture(
                            pages.getIntValue() > 5 ? "big_scroller" : "big_scroller_disabled",
                            12,
                            15,
                            0,
                            0,
                            12,
                            15).draw(context, x, y, w, h, theme))
                .pos(175, 38)
                .size(12, 105)
                .background(IDrawable.EMPTY)
                .onUpdateListener(widget -> {
                    int maximum = Math.max(1, pages.getIntValue() - 5);
                    if (widget.getMax() != maximum) widget.bounds(0, maximum);
                }));
        panel.child(
            IKey.dynamic(
                () -> Math.min(pages.getIntValue(), pageValue.getIntValue() + 1) + "-"
                    + Math.min(pages.getIntValue(), pageValue.getIntValue() + 5)
                    + " / "
                    + pages.getIntValue())
                .color(0xff404040)
                .asWidget()
                .pos(70, 144));
        return panel;
    }

    /** Original parent-item tab; changing pages hides the main screen instead of stacking its art underneath. */
    private static void addReturnTab(ModularPanel panel, TilePackagedProvider tile, int x) {
        panel.onCloseAction(
            () -> panel.getScreen()
                .getMainPanel()
                .setEnabled(true));
        panel.child(
            new ButtonWidget<>().pos(x, -22)
                .size(22, 22)
                .background(texture("states", 256, 256, 160, 192, 20, 20))
                .hoverBackground(texture("states", 256, 256, 160, 224, 22, 22))
                .overlay(
                    new com.cleanroommc.modularui.drawable.ItemDrawable(tile.getVisualRepresentation()).asIcon()
                        .size(16))
                .excludeAreaInRecipeViewer()
                // #tr gui.packaged.back
                // # Return to the Packaged Pattern Provider
                // # zh_CN 返回封包样板供应器
                .tooltip(t -> t.addLine(IKey.lang("gui.packaged.back")))
                .onMousePressed(mouse -> {
                    panel.closeIfOpen();
                    return true;
                }));
    }
}
