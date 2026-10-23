package com.xyp.gtnotgood.common.gui.modularui.multiblock;

import com.xyp.gtnotgood.common.gui.modularui.multiblock.base.GTNGModernMultiBlockBaseGui;
import com.xyp.gtnotgood.common.machines.multiblock.modularmachine.MageAssembler;

import gregtech.api.modularui2.GTGuiTextures;

public final class MageAssemblerGui extends GTNGModernMultiBlockBaseGui<MageAssembler> {

    public MageAssemblerGui(MageAssembler machine) {
        super(machine);
        withMachineModeIcons(GTGuiTextures.OVERLAY_BUTTON_ASSEMBLER_MODE, GTGuiTextures.OVERLAY_BUTTON_PRECISE_MODE,
            GTGuiTextures.OVERLAY_SLOT_CIRCUIT);
    }
}
