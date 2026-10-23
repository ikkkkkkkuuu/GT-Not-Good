// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.gui.modularui.multiblock;

import com.xyp.gtnotgood.common.gui.modularui.multiblock.base.GTNGModernMultiBlockBaseGui;
import com.xyp.gtnotgood.common.machines.multiblock.modularmachine.NyarlathotepsTentacle;

import gregtech.api.modularui2.GTGuiTextures;

public final class NyarlathotepsTentacleGui extends GTNGModernMultiBlockBaseGui<NyarlathotepsTentacle> {

    public NyarlathotepsTentacleGui(NyarlathotepsTentacle machine) {
        super(machine);
        withMachineModeIcons(GTGuiTextures.OVERLAY_BUTTON_ASSEMBLER_MODE, GTGuiTextures.OVERLAY_SLOT_CIRCUIT);
    }
}
