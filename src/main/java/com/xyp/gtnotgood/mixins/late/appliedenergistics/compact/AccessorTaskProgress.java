// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from ABKQPO/GT-Not-Leisure, commit 6cbc6927af4f44c445ea7a879796b4764b00988d.
// Modified for compact, fixed-maximum, energy-free GT Not Good machines.
package com.xyp.gtnotgood.mixins.late.appliedenergistics.compact;

import java.util.LinkedList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import appeng.me.cluster.implementations.CraftingCPUCluster;

/** Adapted AE crafting component; see reference/UPSTREAM_PORT_NOTES.md for provenance. */
@Mixin(value = CraftingCPUCluster.TaskProgress.class, remap = false)
public interface AccessorTaskProgress {

    @Accessor
    long getValue();

    @Accessor
    void setValue(long value);

    @Accessor("diagnosticSessionCrafts")
    LinkedList<?> getDiagnosticSessionCrafts();
}
