/*
 * Adapted from Flux Networks BaseTransferHandler / FluxTransferHandler.
 * Copyright (c) 2018 Ollie Lansdell. MIT; see META-INF/licenses/Flux-Networks-MIT.txt.
 */
package com.xyp.gtnotgood.common.flux;

/**
 * EU-only port of Flux Networks' bounded buffer and per-tick transfer accounting.
 * Unlike the upstream FE conversion layer, all values are exact GT EU. Existing cargo is never
 * truncated when a limit is reduced. Call {@link #beginTick(long)} before receiving energy.
 */
public final class FluxTransferBuffer {

    private long buffer;
    private long added;
    private long acceptedAmperes;
    private long tick = Long.MIN_VALUE;

    public void beginTick(long worldTick) {
        if (tick == worldTick) return;
        tick = worldTick;
        added = 0;
        acceptedAmperes = 0;
    }

    /**
     * Accepts whole packets bounded by configured voltage, amperage and free buffer space.
     *
     * @return accepted amperes; rejected packets remain with the emitter
     */
    public long receive(long voltage, long amperes, long voltageLimit, long ampLimit) {
        if (voltage <= 0 || voltage > voltageLimit || amperes <= 0) return 0;
        long limit = Math.multiplyExact(voltageLimit, ampLimit);
        long valid = Math.max(0, Math.min(limit - added, limit - buffer));
        long accepted = Math.max(0, Math.min(amperes, Math.min(ampLimit - acceptedAmperes, valid / voltage)));
        long amount = accepted * voltage;
        buffer += amount;
        added += amount;
        acceptedAmperes += accepted;
        return accepted;
    }

    public long stored() {
        return buffer;
    }

    /** Loads saved cargo without applying today's potentially lower transfer limit. */
    public void restore(long amount) {
        buffer = Math.max(0, amount);
    }

    /** Removes only committed energy; invalid bookkeeping fails instead of wrapping or duplicating EU. */
    public void remove(long amount) {
        if (amount < 0 || amount > buffer) throw new IllegalArgumentException("Invalid flux buffer debit");
        buffer -= amount;
    }
}
