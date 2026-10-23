package com.xyp.gtnotgood.common.machines.hatch.lighthunter;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

public interface ThreadModifierHost {

    IGregTechTileEntity getBaseMetaTileEntity();

    boolean hasThreadModifier(SatelliteThreadModifierHatch hatch);

    boolean isThreadStructureFormed();
}
