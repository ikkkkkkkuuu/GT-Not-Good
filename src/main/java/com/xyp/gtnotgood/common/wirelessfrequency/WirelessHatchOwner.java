// SPDX-License-Identifier: LGPL-3.0-only

package com.xyp.gtnotgood.common.wirelessfrequency;

import java.util.Set;

import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

public interface WirelessHatchOwner {

    void wirelessFrequency$setController(MTEMultiBlockBase controller);

    Set<MTEMultiBlockBase> wirelessFrequency$getControllers();
}
