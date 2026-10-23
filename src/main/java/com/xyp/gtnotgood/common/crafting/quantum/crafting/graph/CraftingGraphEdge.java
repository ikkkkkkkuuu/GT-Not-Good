// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtnotgood.common.crafting.quantum.crafting.graph;

import org.jetbrains.annotations.Nullable;

import com.xyp.gtnotgood.common.crafting.quantum.api.stacks.AEKey;

public final class CraftingGraphEdge {

    private final AEKey inputKey;
    private final long amountPerCraft;
    private final @Nullable CraftingGraphNode producer;
    private boolean cycleCut;
    private long seedDemand;
    private long seedRequired;
    private long seedAllocated;
    private long seedMissing;
    private long reserveDemand;

    public CraftingGraphEdge(AEKey inputKey, long amountPerCraft, @Nullable CraftingGraphNode producer) {
        this.inputKey = inputKey;
        this.amountPerCraft = amountPerCraft;
        this.producer = producer;
    }

    public AEKey inputKey() {
        return inputKey;
    }

    public long amountPerCraft() {
        return amountPerCraft;
    }

    public @Nullable CraftingGraphNode producer() {
        return producer;
    }

    public boolean cycleCut() {
        return cycleCut;
    }

    public long seedDemand() {
        return seedDemand;
    }

    public void markCycleCut(long seedDemand) {
        this.cycleCut = true;
        this.seedDemand = seedDemand;
        this.seedRequired = seedDemand;
    }

    public long seedRequired() {
        return seedRequired;
    }

    public long seedAllocated() {
        return seedAllocated;
    }

    public long seedMissing() {
        return seedMissing;
    }

    public long reserveDemand() {
        return reserveDemand;
    }

    public void recordSeedResult(long allocated, long missing) {
        this.seedAllocated = allocated;
        this.seedMissing = missing;
    }

    public void setReserveDemand(long reserveDemand) {
        this.reserveDemand = reserveDemand;
    }

    public void resetPlanning() {
        this.cycleCut = false;
        this.seedDemand = 0;
        this.seedRequired = 0;
        this.seedAllocated = 0;
        this.seedMissing = 0;
        this.reserveDemand = 0;
    }
}
