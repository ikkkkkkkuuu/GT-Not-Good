package com.xyp.gtnotgood.common.machines.basic;

import java.util.Arrays;
import java.util.List;

import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.layout.Flow;

import gregtech.api.modularui2.common.CommonWidgets;
import gregtech.api.recipe.BasicUIProperties;
import gregtech.common.gui.modularui.singleblock.base.MTEBasicMachineBaseGui;
import it.unimi.dsi.fastutil.chars.CharList;

final class FragmentWorldCollectorGui extends MTEBasicMachineBaseGui<FragmentWorldCollector> {

    FragmentWorldCollectorGui(FragmentWorldCollector machine, BasicUIProperties properties) {
        super(machine, properties);
    }

    @Override
    protected int getBasePanelWidth() {
        return 194;
    }

    @Override
    protected int getBasePanelHeight() {
        return 180;
    }

    @Override
    protected List<CharList> mapOutSlotsToMatrix() {
        return Arrays.asList(CharList.of('c', 'c', 'c', 'c'), CharList.of('c', 'c', 'c', 'c'),
            CharList.of('c', 'c', 'c', 'c'));
    }

    @Override
    protected ParentWidget<?> createItemOutputSlots(ModularPanel panel, PanelSyncManager sync) {
        return super.createItemOutputSlots(panel, sync).width(4 * SLOT_SIZE);
    }

    @Override
    protected boolean doesAddSpecialSlot() {
        return false;
    }

    @Override
    protected Widget<?> createCircuitSlot(PanelSyncManager sync) {
        // Collector recipes do not use molds; keep only the native circuit selector.
        return CommonWidgets.createCircuitSlot(sync, machine);
    }

    @Override
    protected Flow createBottomLeftCornerFlow(ModularPanel panel, PanelSyncManager sync) {
        return super.createBottomLeftCornerFlow(panel, sync).name("collectorNativeAutoOutput");
    }
}
