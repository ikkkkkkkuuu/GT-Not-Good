package com.xyp.gtnotgood.common.gui.modularui.multiblock;

import com.xyp.gtnotgood.common.gui.modularui.multiblock.base.GTNGModernMultiBlockBaseGui;
import com.xyp.gtnotgood.common.machines.multiblock.modularmachine.FieldExtruderFactory;

import gregtech.api.modularui2.GTGuiTextures;

public final class FieldExtruderFactoryGui extends GTNGModernMultiBlockBaseGui<FieldExtruderFactory> {

    public FieldExtruderFactoryGui(FieldExtruderFactory machine) {
        super(machine);
        withMachineModeIcons(GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_FORMING,
            GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_COMPRESSING);
    }
}
