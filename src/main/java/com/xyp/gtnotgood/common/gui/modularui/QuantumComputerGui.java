// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from ABKQPO/GT-Not-Leisure, commit 6cbc6927af4f44c445ea7a879796b4764b00988d.
// Modified for compact, fixed-maximum, energy-free GT Not Good machines.
package com.xyp.gtnotgood.common.gui.modularui;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.base.GTNGModernMultiBlockBaseGui;
import com.xyp.gtnotgood.common.machines.multiblock.QuantumComputer;

import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.util.GTUtility;

/** Adapted AE crafting component; see reference/UPSTREAM_PORT_NOTES.md for provenance. */

public class QuantumComputerGui extends GTNGModernMultiBlockBaseGui<QuantumComputer> {

    private static final String WIDTH_SYNC_KEY = "quantumComputerWidth";
    private static final String HEIGHT_SYNC_KEY = "quantumComputerHeight";
    private static final String DEPTH_SYNC_KEY = "quantumComputerDepth";
    private static final String MAXIMUM_PARALLEL_SYNC_KEY = "quantumComputerMaximumParallel";
    private static final String USED_PARALLEL_SYNC_KEY = "quantumComputerUsedParallel";
    private static final String MAXIMUM_STORAGE_SYNC_KEY = "quantumComputerMaximumStorage";
    private static final String USED_STORAGE_SYNC_KEY = "quantumComputerUsedStorage";
    private static final String CUSTOM_NAME_SYNC_KEY = "quantumComputerCustomName";
    private static final String AE_ACTIVE_SYNC_KEY = "quantumComputerAeActive";

    public QuantumComputerGui(QuantumComputer multiblock) {
        super(multiblock);
    }

    @Override
    public ModularPanel build(PosGuiData guiData, PanelSyncManager syncManager, UISettings uiSettings) {
        ModularPanel panel = super.build(guiData, syncManager, uiSettings);
        panel.child(createCirculationLogo())
            .child(createGtnlLogo());
        return panel;
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue(AE_ACTIVE_SYNC_KEY, new BooleanSyncValue(multiblock::isActive));
        syncManager
            .syncValue(WIDTH_SYNC_KEY, new IntSyncValue(multiblock::getWidthForGui, multiblock::setWidthFromGui));
        syncManager
            .syncValue(HEIGHT_SYNC_KEY, new IntSyncValue(multiblock::getHeightForGui, multiblock::setHeightFromGui));
        syncManager
            .syncValue(DEPTH_SYNC_KEY, new IntSyncValue(multiblock::getDepthForGui, multiblock::setDepthFromGui));
        syncManager.syncValue(
            MAXIMUM_PARALLEL_SYNC_KEY,
            new IntSyncValue(multiblock::getMaximumParallelForGui, multiblock::setMaximumParallelFromGui));
        syncManager.syncValue(
            USED_PARALLEL_SYNC_KEY,
            new IntSyncValue(multiblock::getUsedParallelForGui, multiblock::setUsedParallelFromGui));
        syncManager.syncValue(
            MAXIMUM_STORAGE_SYNC_KEY,
            new LongSyncValue(multiblock::getMaximumStorageForGui, multiblock::setMaximumStorageFromGui));
        syncManager.syncValue(
            USED_STORAGE_SYNC_KEY,
            new LongSyncValue(multiblock::getUsedStorageForGui, multiblock::setUsedStorageFromGui));
        syncManager.syncValue(
            CUSTOM_NAME_SYNC_KEY,
            new StringSyncValue(multiblock::getDisplayNameForGui, multiblock::setCustomName).allowC2S());
    }

    @Override
    protected ParentWidget<?> createTerminalParentWidget(ModularPanel panel, PanelSyncManager syncManager) {
        return new ParentWidget<>().size(getTerminalWidgetWidth(), getTerminalWidgetHeight())
            .paddingTop(3)
            .paddingBottom(3)
            .paddingLeft(6)
            .paddingRight(0)
            .background(GTGuiTextures.PICTURE_SCREEN_BLACK)
            .child(
                createTerminalTextWidget(syncManager, panel)
                    .size(getTerminalWidgetWidth() - 8, getTerminalWidgetHeight() - 6)
                    .collapseDisabledChild());
    }

    @Override
    protected int getTerminalRowHeight() {
        return 85;
    }

    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager syncManager, ModularPanel parent) {
        BooleanSyncValue activeSyncer = syncManager.findSyncHandler(AE_ACTIVE_SYNC_KEY, BooleanSyncValue.class);
        IntSyncValue widthSyncer = syncManager.findSyncHandler(WIDTH_SYNC_KEY, IntSyncValue.class);
        IntSyncValue heightSyncer = syncManager.findSyncHandler(HEIGHT_SYNC_KEY, IntSyncValue.class);
        IntSyncValue depthSyncer = syncManager.findSyncHandler(DEPTH_SYNC_KEY, IntSyncValue.class);
        IntSyncValue maximumParallelSyncer = syncManager.findSyncHandler(MAXIMUM_PARALLEL_SYNC_KEY, IntSyncValue.class);
        IntSyncValue usedParallelSyncer = syncManager.findSyncHandler(USED_PARALLEL_SYNC_KEY, IntSyncValue.class);
        LongSyncValue maximumStorageSyncer = syncManager.findSyncHandler(MAXIMUM_STORAGE_SYNC_KEY, LongSyncValue.class);
        LongSyncValue usedStorageSyncer = syncManager.findSyncHandler(USED_STORAGE_SYNC_KEY, LongSyncValue.class);

        return new ListWidget<>().fullWidth()
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .child(
                IKey.dynamic(
                    () -> StatCollector
                        .translateToLocal(activeSyncer.getBoolValue() ? activeLabelKey() : inactiveLabelKey()))
                    .asWidget()
                    .color(Color.WHITE.main)
                    .fullWidth())
            .child(
                IKey.dynamic(
                    () -> StatCollector.translateToLocalFormatted(
                        // #tr gtng.compact.machine.quantum_computer.info.0
                        // # Multiblock size: %sx%sx%s
                        // # zh_CN 结构大小：%sx%sx%s
                        "gtng.compact.machine.quantum_computer.info.0",
                        widthSyncer.getIntValue(),
                        heightSyncer.getIntValue(),
                        depthSyncer.getIntValue()))
                    .asWidget()
                    .textAlign(Alignment.CenterLeft)
                    .color(Color.WHITE.main)
                    .fullWidth())
            .child(
                IKey.dynamic(
                    () -> StatCollector.translateToLocalFormatted(
                        // #tr gtng.compact.machine.quantum_computer.info.1
                        // # Co-processors: %s / %s used
                        // # zh_CN 并行：%s / %s 已用
                        "gtng.compact.machine.quantum_computer.info.1",
                        GTUtility.formatShortenedLong(maximumParallelSyncer.getIntValue()),
                        GTUtility.formatShortenedLong(usedParallelSyncer.getIntValue())))
                    .asWidget()
                    .textAlign(Alignment.CenterLeft)
                    .color(Color.WHITE.main)
                    .fullWidth())
            .child(
                IKey.dynamic(
                    () -> StatCollector.translateToLocalFormatted(
                        // #tr gtng.compact.machine.quantum_computer.info.2
                        // # Storage: %s / %s used
                        // # zh_CN 存储：%s / %s 已用
                        "gtng.compact.machine.quantum_computer.info.2",
                        GTUtility.formatShortenedLong(maximumStorageSyncer.getLongValue()),
                        GTUtility.formatShortenedLong(usedStorageSyncer.getLongValue())))
                    .asWidget()
                    .textAlign(Alignment.CenterLeft)
                    .color(Color.WHITE.main)
                    .fullWidth());
    }

    /** Returns the label for a formed and connected AE CPU. */
    private static String activeLabelKey() {
        // #tr gtng.compact.quantum.active
        // # AE network connected
        // # zh_CN 已连接AE网络
        return "gtng.compact.quantum.active";
    }

    /** Returns the label shown before structure formation or network connection. */
    private static String inactiveLabelKey() {
        // #tr gtng.compact.quantum.inactive
        // # Waiting for AE network
        // # zh_CN 等待AE网络连接
        return "gtng.compact.quantum.inactive";
    }

    @Override
    protected Flow createPanelGap(ModularPanel parent, PanelSyncManager syncManager) {
        return Flow.row()
            .fullWidth()
            .height(getTextBoxToInventoryGap())
            .paddingLeft(4)
            .paddingRight(25)
            .child(createCustomNameField(syncManager));
    }

    @Override
    protected IWidget createInventoryRow(ModularPanel panel, PanelSyncManager syncManager) {
        return Flow.row()
            .fullWidth()
            .height(76)
            .childIf(
                multiblock.doesBindPlayerInventory(),
                () -> SlotGroupWidget.playerInventory(false)
                    .marginLeft(4))
            .child(createButtonColumn(panel, syncManager));
    }

    @Override
    protected Flow createButtonColumn(ModularPanel panel, PanelSyncManager syncManager) {
        return Flow.column()
            .width(18)
            .leftRel(1, -2, 1)
            .top(36)
            .child(createStructureUpdateButton(syncManager))
            .childIf(
                multiblock.doesBindPlayerInventory(),
                () -> new ItemSlot()
                    .slot(new ModularSlot(multiblock.inventoryHandler, multiblock.getControllerSlotIndex()) {

                        @Override
                        public int getSlotStackLimit() {
                            return multiblock.getInventoryStackLimit();
                        }
                    }.singletonSlotGroup())
                    .backgroundOverlay(GTGuiTextures.SLOT_ITEM_DARK)
                    .marginTop(4));
    }

    @Override
    protected Widget<? extends Widget<?>> makeLogoWidget(PanelSyncManager syncManager, ModularPanel parent) {
        return new Widget<>().size(0);
    }

    private IWidget createCustomNameField(PanelSyncManager syncManager) {
        StringSyncValue customNameSyncer = syncManager.findSyncHandler(CUSTOM_NAME_SYNC_KEY, StringSyncValue.class);
        return new TextFieldWidget().value(customNameSyncer)
            .setTextAlignment(Alignment.Center)
            .setTextColor(Color.WHITE.main)
            .background(GTGuiTextures.BACKGROUND_TEXT_FIELD)
            .tooltipBuilder(
                // #tr gtng.compact.machine.quantum_computer.info.3
                // # Set Quantum Computer custom name
                // # zh_CN 设置量子计算机自定义名称
                tooltip -> tooltip
                    .addLine(StatCollector.translateToLocal("gtng.compact.machine.quantum_computer.info.3")))
            .size(162, 18);
    }

    private IWidget createCirculationLogo() {
        return new IDrawable.DrawableWidget(new DynamicDrawable(() -> GTNGGuiTextures.OVERLAY_BUTTON_BATCH_MODE))
            .size(18, 18)
            .pos(172, 49);
    }

    private IWidget createGtnlLogo() {
        return new IDrawable.DrawableWidget(GTNGGuiTextures.PICTURE_GODFORGE_LOGO).size(18, 18)
            .pos(172, 67);
    }

}
