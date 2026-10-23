// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.gui.modularui.multiblock;

import java.util.Objects;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.DynamicLinkedSyncHandler;
import com.cleanroommc.modularui.value.sync.GenericListSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.DynamicSyncedWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.base.GTNGModernMultiBlockBaseGui;
import com.xyp.gtnotgood.common.machines.multiblock.parallelmachine.LargeFurnace;

public final class LargeFurnaceGui extends GTNGModernMultiBlockBaseGui<LargeFurnace> {

    public LargeFurnaceGui(LargeFurnace machine) {
        super(machine);
    }

    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager sync, ModularPanel parent) {
        ListWidget<IWidget, ?> terminal = super.createTerminalTextWidget(sync, parent).name("largeFurnaceTerminal");
        GenericListSyncHandler<NBTTagCompound> outputs = new GenericListSyncHandler<>(multiblock::getBatchOutputRows,
            ignored -> {}, PacketBuffer::readNBTTagCompoundFromBuffer, PacketBuffer::writeNBTTagCompoundToBuffer,
            Objects::equals, value -> (NBTTagCompound) value.copy());
        sync.syncValue("parallelMachineOutputs", outputs);
        DynamicLinkedSyncHandler<?> display = new DynamicLinkedSyncHandler<>(outputs)
            .widgetProvider((manager, value) -> {
                Flow column = Flow.column().name("largeFurnaceBatchOutputs").fullWidth().coverChildrenHeight(0)
                    .crossAxisAlignment(Alignment.CrossAxis.START);
                for (NBTTagCompound row : value.getValue()) column.child(createModuleOutputRow(row));
                return column;
            });
        sync.syncValue("parallelMachineOutputDisplay", display);
        return terminal.child(new DynamicSyncedWidget<>().syncHandler(display).widthRel(0.85f).coverChildrenHeight(0));
    }

}
