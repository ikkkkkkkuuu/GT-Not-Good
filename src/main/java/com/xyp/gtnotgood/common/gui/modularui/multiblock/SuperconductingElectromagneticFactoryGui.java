package com.xyp.gtnotgood.common.gui.modularui.multiblock;

import com.xyp.gtnotgood.common.gui.modularui.multiblock.base.GTNGModernMultiBlockBaseGui;
import com.xyp.gtnotgood.common.machines.multiblock.modularmachine.SuperconductingElectromagneticFactory;

import gregtech.api.modularui2.GTGuiTextures;

public final class SuperconductingElectromagneticFactoryGui
    extends GTNGModernMultiBlockBaseGui<SuperconductingElectromagneticFactory> {

    public SuperconductingElectromagneticFactoryGui(SuperconductingElectromagneticFactory machine) {
        super(machine);
        withMachineModeIcons(GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_POLARIZER,
            GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_SEPARATOR, GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_CHEMBATH,
            GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_ACCELERATOR, GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_ARC);
    }
}
