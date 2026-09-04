package com.xyp.gtnotgood.common.gui.modularui.multiblock;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.Dialog;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtnotgood.common.gui.modularui.GTNGGuiTextures;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.base.GTNGModernMultiBlockBaseGui;
import com.xyp.gtnotgood.common.machines.multiblock.DimensionallyTranscendentPlasmaFusionComputer;

import gregtech.api.modularui2.GTGuiTextures;

/** Server-authoritative wireless controls for the Dimensionally Transcendent Plasma Fusion Computer. */
public final class DimensionallyTranscendentPlasmaFusionComputerGui
    extends GTNGModernMultiBlockBaseGui<DimensionallyTranscendentPlasmaFusionComputer> {

    private static final String AVAILABLE_SYNC_KEY = "gtng.dtpf.wireless.available";
    private static final String ENABLED_SYNC_KEY = "gtng.dtpf.wireless.enabled";
    private static final String PARALLEL_SYNC_KEY = "gtng.dtpf.wireless.parallel.value";
    private static final String TOGGLE_SYNC_KEY = "gtng.dtpf.wireless.toggle";

    public DimensionallyTranscendentPlasmaFusionComputerGui(DimensionallyTranscendentPlasmaFusionComputer multiblock) {
        super(multiblock);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.syncValue(AVAILABLE_SYNC_KEY, new BooleanSyncValue(multiblock::isWirelessModeAvailable));
        syncManager.syncValue(ENABLED_SYNC_KEY, new BooleanSyncValue(multiblock::isWirelessModeEnabled));
        syncManager.syncValue(
            PARALLEL_SYNC_KEY,
            new IntSyncValue(multiblock::getWirelessParallel, multiblock::setWirelessParallel).allowC2S());
        syncManager.syncValue(
            TOGGLE_SYNC_KEY,
            new InteractionSyncHandler().setOnMousePressed(
                mouseData -> {
                    if (!mouseData.isClient() && mouseData.mouseButton == 0) multiblock.toggleWirelessModeFromServer();
                }));
    }

    @Override
    protected Flow createButtonColumn(ModularPanel panel, PanelSyncManager syncManager) {
        return super.createButtonColumn(panel, syncManager).child(createWirelessButton(syncManager, panel));
    }

    private IWidget createWirelessButton(PanelSyncManager syncManager, ModularPanel parent) {
        BooleanSyncValue available = syncManager.findSyncHandler(AVAILABLE_SYNC_KEY, BooleanSyncValue.class);
        BooleanSyncValue enabled = syncManager.findSyncHandler(ENABLED_SYNC_KEY, BooleanSyncValue.class);
        InteractionSyncHandler toggle = syncManager.findSyncHandler(TOGGLE_SYNC_KEY, InteractionSyncHandler.class);
        IPanelHandler parallelPanel = syncManager.syncedPanel(
            "gtngDTPFWirelessParallelPanel",
            true,
            (manager, handler) -> createWirelessParallelPanel(syncManager, parent));

        ButtonWidget<?> button = new ButtonWidget<>().size(16)
            .marginBottom(2)
            .overlay(
                new DynamicDrawable(
                    () -> enabled.getBoolValue() ? GTNGGuiTextures.OVERLAY_BUTTON_BATTERY_ON
                        : GTNGGuiTextures.OVERLAY_BUTTON_BATTERY_OFF))
            .onMousePressed(mouseButton -> {
                if (mouseButton == 1) {
                    if (parallelPanel.isPanelOpen()) parallelPanel.closePanel();
                    else parallelPanel.openPanel();
                    return false;
                }
                return mouseButton != 0 || !available.getBoolValue();
            })
            .syncHandler(toggle)
            // #tr machine.gtnotgood.dtpf.wireless
            // # Wireless Mode
            // # zh_CN 无线模式
            .tooltip(
                t -> t.addLine(StatCollector.translateToLocal("machine.gtnotgood.dtpf.wireless"))
                    // #tr machine.gtnotgood.dtpf.wireless.requirement
                    // # Requires an Astral Array Fabricator and an MK-V structure
                    // # zh_CN 需要星阵且结构等级达到 MK-V
                    .addLine(
                        EnumChatFormatting.GRAY
                            + StatCollector.translateToLocal("machine.gtnotgood.dtpf.wireless.requirement"))
                    // #tr machine.gtnotgood.dtpf.wireless.parallel_hint
                    // # Right-click to configure wireless parallel
                    // # zh_CN 右键设置无线并行
                    .addLine(
                        EnumChatFormatting.GRAY
                            + StatCollector.translateToLocal("machine.gtnotgood.dtpf.wireless.parallel_hint")))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
        return applyModernStateButton(button, enabled::getBoolValue, available::getBoolValue);
    }

    private ModularPanel createWirelessParallelPanel(PanelSyncManager syncManager, ModularPanel parent) {
        IntSyncValue parallel = syncManager.findSyncHandler(PARALLEL_SYNC_KEY, IntSyncValue.class);
        Dialog<?> panel = new Dialog<>("gtngDTPFWirelessParallelPanel", null);
        panel.relative(parent)
            .leftRel(1)
            .topRel(0.8F)
            .size(122, 54)
            .background(GTGuiTextures.BACKGROUND_POPUP_STANDARD);
        panel.setDisablePanelsBelow(false)
            .setCloseOnOutOfBoundsClick(false)
            .setDraggable(true);
        panel.child(ButtonWidget.panelCloseButton());
        panel.child(
            Flow.column()
                .full()
                .paddingTop(5)
                .paddingLeft(5)
                .paddingRight(5)
                .child(
                    // #tr machine.gtnotgood.dtpf.wireless.parallel
                    // # Wireless Parallel
                    // # zh_CN 无线并行
                    IKey.lang("machine.gtnotgood.dtpf.wireless.parallel")
                        .asWidget()
                        .textAlign(Alignment.Center)
                        .marginBottom(4)
                        .fullWidth())
                .child(
                    new TextFieldWidget().value(parallel)
                        .formatAsInteger(true)
                        .numbersInt(1, Integer.MAX_VALUE)
                        .scrollValues(1, 64, 1024, 65536)
                        .setTextAlignment(Alignment.Center)
                        .size(112, 18)));
        return panel;
    }
}
