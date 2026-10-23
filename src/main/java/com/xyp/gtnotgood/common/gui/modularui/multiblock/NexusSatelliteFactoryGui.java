// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.gui.modularui.multiblock;

import java.util.Objects;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.DynamicLinkedSyncHandler;
import com.cleanroommc.modularui.value.sync.GenericListSyncHandler;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.DynamicSyncedWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtnotgood.common.gui.modularui.GTNGGuiTextures;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.base.GTNGModernMultiBlockBaseGui;
import com.xyp.gtnotgood.common.machines.multiblock.NexusSatelliteFactory;

import gregtech.api.modularui2.GTGuiTextures;

public final class NexusSatelliteFactoryGui extends GTNGModernMultiBlockBaseGui<NexusSatelliteFactory> {

    public NexusSatelliteFactoryGui(NexusSatelliteFactory machine) {
        super(machine);
        withMachineModeIcons(GTNGGuiTextures.OVERLAY_BUTTON_MACHINE_MODE);
    }

    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager sync, ModularPanel parent) {
        ListWidget<IWidget, ?> terminal = super.createTerminalTextWidget(sync, parent).name("satelliteTerminal");
        GenericListSyncHandler<NBTTagCompound> outputs = new GenericListSyncHandler<>(multiblock::getBatchOutputRows,
            ignored -> {}, PacketBuffer::readNBTTagCompoundFromBuffer, PacketBuffer::writeNBTTagCompoundToBuffer,
            Objects::equals, value -> (NBTTagCompound) value.copy());
        sync.syncValue("lightHunterOutputs", outputs);
        DynamicLinkedSyncHandler<?> display = new DynamicLinkedSyncHandler<>(outputs)
            .widgetProvider((manager, value) -> {
                Flow column = Flow.column().name("satelliteBatchOutputs").fullWidth().coverChildrenHeight(0)
                    .crossAxisAlignment(Alignment.CrossAxis.START);
                for (NBTTagCompound row : value.getValue()) column.child(createModuleOutputRow(row));
                return column;
            });
        sync.syncValue("lightHunterOutputDisplay", display);
        return terminal.child(new DynamicSyncedWidget<>().syncHandler(display).widthRel(0.85f).coverChildrenHeight(0));
    }

    @Override
    protected ButtonWidget<?> createPowerPanelButton(PanelSyncManager sync, ModularPanel parent) {
        return super.createPowerPanelButton(sync, parent).name("satelliteBatchSettings")
            .addTooltipLine(IKey.lang("gtng.light_hunter.satellite.minimum_duration"));
    }

    @Override
    protected IWidget makeTitleTextWidget() {
        IntSyncValue duration = new IntSyncValue(multiblock::getMinimumDuration, multiblock::setMinimumDuration)
            .allowC2S();
        Flow header = Flow.column().fullWidth().height(60).child(super.makeTitleTextWidget());
        // #tr gtng.light_hunter.satellite.minimum_duration
        // # Minimum batch duration (ticks)
        // # zh_CN 最短批次时间（tick）
        header.child(
            IKey.lang("gtng.light_hunter.satellite.minimum_duration").asWidget().scale(0.8f).fullWidth().height(14));
        return header.child(
            new TextFieldWidget().name("satelliteMinimumDuration").value(duration).numbersInt(1, Integer.MAX_VALUE)
                .setTextAlignment(Alignment.Center).background(GTGuiTextures.BACKGROUND_TEXT_FIELD).size(110, 18));
    }

    @Override
    protected String getMachineModeSelectorPrefix() {
        return "satellite";
    }
}
