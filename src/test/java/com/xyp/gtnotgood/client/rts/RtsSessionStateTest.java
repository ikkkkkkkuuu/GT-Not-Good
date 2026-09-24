package com.xyp.gtnotgood.client.rts;

import static org.junit.Assert.*;

import org.junit.Test;

import com.xyp.gtnotgood.common.packet.RtsSessionMessage;
import com.xyp.gtnotgood.common.rts.session.RtsSessionLease;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/** Exercises asynchronous cancellation, replay, server revocation and malformed wire data. */
public class RtsSessionStateTest {

    private static RtsSessionMessage grant(long request, long token, int dimension) {
        return RtsSessionMessage.grant(new RtsSessionLease(request, token, dimension, 10.5, 64, -4.5, 128));
    }

    @Test
    public void cancelledOpenAndOldRepliesNeverReactivateTheView() {
        RtsSessionState state = new RtsSessionState();
        long old = state.begin(0, 10);
        state.close();
        assertFalse(state.receive(grant(old, 12, 0), 11));
        long current = state.begin(0, 12);
        assertFalse(state.receive(grant(old, 12, 0), 13));
        assertFalse(state.receive(grant(current, 13, -1), 13));
        assertEquals(RtsSessionState.Phase.REQUESTED, state.phase());
        assertTrue(state.receive(grant(current, 13, 0), 14));
        state.activate();
        assertFalse(state.receive(grant(current, 999, 0), 15));
        assertFalse(state.receive(RtsSessionMessage.control(RtsSessionMessage.CLOSED, old, 12, 0), 16));
        assertFalse(state.receive(RtsSessionMessage.control(RtsSessionMessage.CLOSED, current, 999, 0), 16));
        assertEquals(RtsSessionState.Phase.ACTIVE, state.phase());
        assertTrue(state.receive(RtsSessionMessage.control(RtsSessionMessage.CLOSED, current, 13, 0), 17));
        assertFalse(state.receive(grant(current, 13, 0), 18));
        assertNull(state.lease());
    }

    @Test
    public void heartbeatDoesNotRecaptureViewAndTimeoutStartsAtLastValidReply() {
        RtsSessionState state = new RtsSessionState();
        long request = state.begin(0, 0);
        assertFalse(state.expired(200));
        assertTrue(state.expired(201));
        state.receive(grant(request, 7, 0), 205);
        state.activate();
        state.receive(grant(request, 7, 0), 220);
        assertEquals(RtsSessionState.Phase.ACTIVE, state.phase());
        assertFalse(state.expired(420));
        assertTrue(state.expired(421));
        state.close();
        state.close();
        assertFalse(state.expired(1000));
    }

    @Test(expected = IllegalStateException.class)
    public void unauthorizedViewCannotActivate() {
        new RtsSessionState().activate();
    }

    @Test
    public void wireRoundTripPreservesServerBoundsAndRejectsEveryTruncation() {
        ByteBuf encoded = Unpooled.buffer();
        try {
            grant(5, Long.MIN_VALUE, -1).toBytes(encoded);
            RtsSessionMessage decoded = new RtsSessionMessage();
            ByteBuf copy = encoded.copy();
            try {
                decoded.fromBytes(copy);
            } finally {
                copy.release();
            }
            assertEquals(Long.MIN_VALUE, decoded.token);
            assertEquals(-1, decoded.dimension);
            assertEquals(-4.5, decoded.lease.anchorZ, 0);
            for (int length = 0; length < encoded.readableBytes(); length++) {
                ByteBuf partial = encoded.copy(0, length);
                try {
                    new RtsSessionMessage().fromBytes(partial);
                    fail("Accepted truncated packet of length " + length);
                } catch (IllegalArgumentException expected) {
                    // Exact packet size must be validated before optional fields are read.
                } finally {
                    partial.release();
                }
            }
            encoded.writeByte(0);
            try {
                new RtsSessionMessage().fromBytes(encoded);
                fail("Accepted trailing bytes");
            } catch (IllegalArgumentException expected) {
                assertNotNull(expected.getMessage());
            }
        } finally {
            encoded.release();
        }
    }

    @Test
    public void invalidServerCoordinatesCannotBecomeALease() {
        for (double invalid : new double[] { Double.NaN, Double.POSITIVE_INFINITY, 30000001 }) {
            try {
                new RtsSessionLease(1, 1, 0, invalid, 64, 0, 128);
                fail("Accepted invalid anchor");
            } catch (IllegalArgumentException expected) {
                assertNotNull(expected.getMessage());
            }
        }
    }
}
