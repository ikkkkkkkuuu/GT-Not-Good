// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtnotgood.common.crafting.quantum.core;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.config.QuantumCraftingConfig;

public final class AELog {

    public static boolean isCraftingLogEnabled() {
        return QuantumCraftingConfig.enableCalculationLog;
    }

    public static void crafting(String message) {
        GTNotGood.LOG.info("[QuantumCrafting] {}", message);
    }

    public static void craftingDebug(String message) {
        GTNotGood.LOG.debug("[QuantumCrafting] {}", message);
    }

    public static void craftingPerformance(String format, Object... params) {
        GTNotGood.LOG.info("[QuantumCrafting] {}", String.format(format, params));
    }

    public static void info(String format, Object... params) {
        GTNotGood.LOG.info("[QuantumCrafting] {}", String.format(format, params));
    }

    public static void info(Throwable throwable, String message) {
        GTNotGood.LOG.info("[QuantumCrafting] {}", message, throwable);
    }
}
