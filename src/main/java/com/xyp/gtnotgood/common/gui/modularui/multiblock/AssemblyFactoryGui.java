package com.xyp.gtnotgood.common.gui.modularui.multiblock;

import com.xyp.gtnotgood.common.gui.modularui.multiblock.base.GTNGModernMultiBlockBaseGui;
import com.xyp.gtnotgood.common.machines.multiblock.AssemblyFactory;

import gregtech.api.modularui2.GTGuiTextures;

/**
 * Main Assembly Factory panel with a two-state GTNC-style recipe-mode button.
 */
public class AssemblyFactoryGui extends GTNGModernMultiBlockBaseGui<AssemblyFactory> {

    public AssemblyFactoryGui(AssemblyFactory multiblock) {
        super(multiblock);
    }

    /** Adds one icon for component mode and one for Assembly Line mode. */
    @Override
    protected void setMachineModeIcons() {
        machineModeIcons.clear();
        machineModeIcons.add(GTGuiTextures.OVERLAY_BUTTON_MODE[0]);
        machineModeIcons.add(GTGuiTextures.OVERLAY_BUTTON_MODE[1]);
    }

}
