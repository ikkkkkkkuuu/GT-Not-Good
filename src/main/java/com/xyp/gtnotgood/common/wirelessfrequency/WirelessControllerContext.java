// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.common.wirelessfrequency;

import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

public final class WirelessControllerContext {

    private static final ThreadLocal<MTEMultiBlockBase> CURRENT = new ThreadLocal<>();

    private WirelessControllerContext() {}

    public static MTEMultiBlockBase enter(MTEMultiBlockBase controller) {
        MTEMultiBlockBase previous = CURRENT.get();
        CURRENT.set(controller);
        return previous;
    }

    public static void restore(MTEMultiBlockBase previous) {
        if (previous == null) CURRENT.remove();
        else CURRENT.set(previous);
    }

    public static MTEMultiBlockBase current() {
        return CURRENT.get();
    }
}
