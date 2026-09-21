// SPDX-License-Identifier: LGPL-3.0-only
// New GTNG TC4 integration (c) 2026 GTNG contributors.
package com.xyp.gtnotgood.common.packaged;

import appeng.api.config.Actionable;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.IMEMonitor;
import appeng.me.GridAccessException;
import thaumcraft.api.aspects.Aspect;
import thaumicenergistics.common.storage.AEEssentiaStack;
import thaumicenergistics.common.storage.AEEssentiaStackType;

/** Optional ThE linkage, reached only after the loaded-mod check; never initializes without ThE installed. */
final class ThaumicEnergisticsSupply {

    private ThaumicEnergisticsSupply() {}

    /** Debit one actual stored unit for one TC4 drain call; shortage leaves TC4's recipe balance untouched. */
    @SuppressWarnings("unchecked")
    static boolean extractOne(TilePackagedProvider provider, Aspect aspect) {
        try {
            IMEMonitor<AEEssentiaStack> monitor = (IMEMonitor<AEEssentiaStack>) provider.getProxy()
                .getStorage()
                .getMEMonitor(AEEssentiaStackType.ESSENTIA_STACK_TYPE);
            var extracted = monitor
                .extractItems(new AEEssentiaStack(aspect, 1), Actionable.MODULATE, new MachineSource(provider));
            return extracted != null && extracted.getStackSize() == 1;
        } catch (GridAccessException ignored) {
            return false;
        }
    }
}
