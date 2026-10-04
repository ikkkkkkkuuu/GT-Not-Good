package com.xyp.gtnotgood.common.wireless;

import gregtech.api.logic.ProcessingLogic;

/** Narrow bridge to the existing GT recipe lifecycle, including ME input transaction boundaries. */
public interface WirelessControllerAccess {

    WirelessRecipeScheduler gtng$getWirelessScheduler();

    ProcessingLogic gtng$getProcessingLogic();

    boolean gtng$checkRecipe();

    boolean gtng$maintenance();

    void gtng$outputAfterRecipe();
}
