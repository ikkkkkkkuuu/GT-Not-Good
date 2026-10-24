// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.common.gui.modularui.multiblock;

import com.xyp.gtnotgood.common.gui.modularui.multiblock.base.GTNGModernMultiBlockBaseGui;
import com.xyp.gtnotgood.common.machines.multiblock.modularmachine.KerrNewmanHomogenizer;

import gregtech.api.modularui2.GTGuiTextures;

public final class KerrNewmanHomogenizerGui extends GTNGModernMultiBlockBaseGui<KerrNewmanHomogenizer> {

    public KerrNewmanHomogenizerGui(KerrNewmanHomogenizer machine) {
        super(machine);
        withMachineModeIcons(GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_DEFAULT,
            GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_SEPARATOR, GTGuiTextures.OVERLAY_BUTTON_MACHINEMODE_WASHPLANT);
    }
}
