// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtnotgood.common.crafting.quantum.crafting.graph;

import com.github.bsideup.jabel.Desugar;

import it.unimi.dsi.fastutil.objects.Reference2LongMap;

@Desugar
public record SccPlan(Reference2LongMap<CraftingGraphNode> plannedOutputs) {

    public long plannedOutput(CraftingGraphNode node) {
        return this.plannedOutputs.getLong(node);
    }
}
