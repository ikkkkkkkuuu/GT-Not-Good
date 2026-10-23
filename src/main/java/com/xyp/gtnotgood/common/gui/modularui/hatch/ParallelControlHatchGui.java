package com.xyp.gtnotgood.common.gui.modularui.hatch;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtnotgood.common.machines.hatch.parallel.ParallelControlHatch;

import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.gui.modularui.hatch.base.MTEHatchBaseGui;

public final class ParallelControlHatchGui extends MTEHatchBaseGui<ParallelControlHatch> {

    public ParallelControlHatchGui(ParallelControlHatch machine) {
        super(machine);
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager sync) {
        IntSyncValue value = new IntSyncValue(machine::getParallel, machine::setParallel).allowC2S();
        sync.syncValue("parallel_multiplier", value);
        // #tr gtng.parallel_hatch.setting
        // # Parallel multiplier
        // # zh_CN 并行倍率
        return getEmptyContent().child(IKey.lang("gtng.parallel_hatch.setting").asWidget().pos(20, 0))
            .child(new TextFieldWidget().name("parallelMultiplier").value(value)
                .numbersInt(1, machine.getMaximumParallel()).background(GTGuiTextures.BACKGROUND_TEXT_FIELD)
                .size(120, 18).pos(20, 18));
    }
}
