// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from ABKQPO/GT-Not-Leisure, commit 6cbc6927af4f44c445ea7a879796b4764b00988d.
// Modified for compact, fixed-maximum, energy-free GT Not Good machines.
package com.xyp.gtnotgood.mixins.late.AppliedEnergistics.compact;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.xyp.gtnotgood.utils.crafting.CraftingBatchPlanner.SessionSegment;

import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.diagnostics.CraftingDiagnosticSessionId;

/**
 * Adapts one private AE diagnostics count segment to the planner's bulk-consumption contract without reflection.
 */
@Mixin(value = CraftingCPUCluster.TaskProgress.SessionCraftCount.class, remap = false)
/** Adapted AE crafting component; see reference/UPSTREAM_PORT_NOTES.md for provenance. */
public interface AccessorSessionCraftCount extends SessionSegment<CraftingDiagnosticSessionId> {

    /**
     * Returns the diagnostics session owning this contiguous craft segment.
     *
     * @return owning session identifier
     */
    @Override
    @Accessor("sessionId")
    CraftingDiagnosticSessionId getSessionId();

    /**
     * Returns the unconsumed craft count in this segment.
     *
     * @return positive remaining count
     */
    @Override
    @Accessor("remaining")
    long getRemaining();

    /**
     * Stores the positive remainder after a batch consumes part of this segment.
     *
     * @param remaining positive unconsumed count
     */
    @Override
    @Accessor("remaining")
    void setRemaining(long remaining);
}
