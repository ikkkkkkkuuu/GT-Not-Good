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

    /**
     * Reserves only missing primal Vis after simulating every aspect. Integer essentia is converted to 0.01-Vis
     * credit, retaining fractional change. If another storage handler supplies less than simulated, extracted
     * credit stays in the provider's persistent ledger; retries cannot double-charge or discard it.
     * The caller spends this reservation only after the native wand payment succeeds.
     */
    @SuppressWarnings("unchecked")
    static boolean reserveArcane(TilePackagedProvider provider, thaumcraft.api.aspects.AspectList missing, int unit) {
        if (unit <= 0 || !provider.getProxy()
            .isActive()) return false;
        try {
            IMEMonitor<AEEssentiaStack> monitor = (IMEMonitor<AEEssentiaStack>) provider.getProxy()
                .getStorage()
                .getMEMonitor(AEEssentiaStackType.ESSENTIA_STACK_TYPE);
            MachineSource source = new MachineSource(provider);
            for (Aspect aspect : missing.getAspects()) {
                long needed = Math
                    .max(0, missing.getAmount(aspect) - provider.arcaneVisCredit.getOrDefault(aspect.getTag(), 0L));
                long units = (needed + unit - 1) / unit;
                if (units == 0) continue;
                AEEssentiaStack simulated = monitor
                    .extractItems(new AEEssentiaStack(aspect, units), Actionable.SIMULATE, source);
                if (simulated == null || simulated.getStackSize() < units) {
                    provider.arcaneMissingAspect = aspect.getName();
                    provider.arcaneMissingUnits = (int) Math
                        .min(Integer.MAX_VALUE, units - (simulated == null ? 0 : simulated.getStackSize()));
                    return false;
                }
            }
            for (Aspect aspect : missing.getAspects()) {
                long credit = provider.arcaneVisCredit.getOrDefault(aspect.getTag(), 0L);
                long units = (Math.max(0, missing.getAmount(aspect) - credit) + unit - 1) / unit;
                if (units == 0) continue;
                AEEssentiaStack extracted = monitor
                    .extractItems(new AEEssentiaStack(aspect, units), Actionable.MODULATE, source);
                if (extracted != null && extracted.getStackSize() > 0) {
                    credit += extracted.getStackSize() * unit;
                    provider.arcaneVisCredit.put(aspect.getTag(), credit);
                    provider.markDirty();
                }
                if (credit < missing.getAmount(aspect)) return false;
            }
            return true;
        } catch (GridAccessException ignored) {
            return false;
        }
    }

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
