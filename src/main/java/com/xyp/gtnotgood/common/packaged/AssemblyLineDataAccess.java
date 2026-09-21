package com.xyp.gtnotgood.common.packaged;

import java.util.List;

import gregtech.api.metatileentity.implementations.MTEHatchDataAccess;

/** Read-only bridge to the advanced assembly line's installed data access hatches. */
public interface AssemblyLineDataAccess {

    List<MTEHatchDataAccess> gtnotgood$getDataAccessHatches();
}
