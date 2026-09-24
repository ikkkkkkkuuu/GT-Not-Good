package com.xyp.gtnotgood.client.rts;

import com.xyp.gtnotgood.common.packet.RtsSessionMessage;
import com.xyp.gtnotgood.common.rts.session.RtsSessionLease;

/**
 * Client-thread state machine independent of Minecraft rendering. Closing is immediate and terminal
 * for a request generation, so a delayed grant cannot reopen a dismissed or disconnected session.
 */
public final class RtsSessionState {

    /** READY means authorized but no camera/screen has taken ownership yet. */
    public enum Phase {
        CLOSED,
        REQUESTED,
        READY,
        ACTIVE
    }

    private Phase phase = Phase.CLOSED;
    private long generation;
    private int dimension;
    private long lastReplyTick;
    private RtsSessionLease lease;

    /** Begins a fresh generation; callers must finish restoring the previous view before calling. */
    public long begin(int dimension, long tick) {
        if (phase != Phase.CLOSED) throw new IllegalStateException("RTS session already open");
        generation = Math.incrementExact(generation);
        this.dimension = dimension;
        lastReplyTick = tick;
        phase = Phase.REQUESTED;
        return generation;
    }

    /**
     * Applies a reply only to its live request. Connection/world identity is checked by RtsClientState
     * before reaching this pure state machine.
     *
     * @return true if the message belongs to this session, false for stale/mismatched messages
     */
    public boolean receive(RtsSessionMessage message, long tick) {
        if (phase == Phase.CLOSED || message.requestId != generation) return false;
        if (message.action == RtsSessionMessage.GRANTED) {
            if (message.lease == null || message.dimension != dimension
                || (lease != null && message.token != lease.token)) return false;
            lease = message.lease;
            lastReplyTick = tick;
            if (phase == Phase.REQUESTED) phase = Phase.READY;
            return true;
        }
        if (message.action == RtsSessionMessage.DENIED || message.action == RtsSessionMessage.CLOSED) {
            if (lease != null && message.token != 0 && message.token != lease.token) return false;
            close();
            return true;
        }
        return false;
    }

    public void activate() {
        if (phase != Phase.READY) throw new IllegalStateException("RTS view requires server authorization");
        phase = Phase.ACTIVE;
    }

    public void close() {
        phase = Phase.CLOSED;
        lease = null;
    }

    public boolean expired(long tick) {
        return phase != Phase.CLOSED && tick - lastReplyTick > 200;
    }

    public Phase phase() {
        return phase;
    }

    public long requestId() {
        return generation;
    }

    public int dimension() {
        return dimension;
    }

    public RtsSessionLease lease() {
        return lease;
    }
}
