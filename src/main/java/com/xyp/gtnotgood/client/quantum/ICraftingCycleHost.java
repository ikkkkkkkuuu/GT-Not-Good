// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtnotgood.client.quantum;

import java.util.List;

import com.xyp.gtnotgood.common.crafting.cycle.RecursivePatternFinder.Cycle;

import appeng.api.storage.data.IAEStack;

/**
 * A screen confirming a request, told the crafting cycles found for it.
 */
public interface ICraftingCycleHost {

    /**
     * @param manual asked for by the player, to be shown in full
     */
    void gtng$cyclesFound(IAEStack<?> request, List<Cycle> cycles, boolean manual);
}
