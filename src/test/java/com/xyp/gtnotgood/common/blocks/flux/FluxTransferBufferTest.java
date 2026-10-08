package com.xyp.gtnotgood.common.blocks.flux;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Regression tests for EU conservation, tick limits and invalid packet arithmetic in the Flux port. */
public class FluxTransferBufferTest {

    @Test
    public void allFacesShareOneAmperageBudget() {
        FluxTransferBuffer buffer = new FluxTransferBuffer();
        buffer.beginTick(10);
        assertEquals(2, buffer.receive(8, 2, 32, 3));
        assertEquals(1, buffer.receive(32, 9, 32, 3));
        assertEquals(0, buffer.receive(1, 99, 32, 3));
        assertEquals(48, buffer.stored());
    }

    @Test
    public void settlingBufferDoesNotResetTickBudget() {
        FluxTransferBuffer buffer = new FluxTransferBuffer();
        buffer.beginTick(10);
        assertEquals(4, buffer.receive(32, 4, 32, 4));
        buffer.remove(128);
        buffer.beginTick(10);
        assertEquals(0, buffer.receive(32, 4, 32, 4));
        buffer.beginTick(11);
        assertEquals(4, buffer.receive(32, 4, 32, 4));
    }

    @Test
    public void lowerLimitPreservesCargoAndBlocksAdditionalInput() {
        FluxTransferBuffer original = new FluxTransferBuffer();
        original.receive(2048, 4, 2048, 4);
        FluxTransferBuffer reloaded = new FluxTransferBuffer();
        reloaded.restore(original.stored());
        reloaded.beginTick(100);
        assertEquals(0, reloaded.receive(32, 1, 32, 1));
        assertEquals(8192, reloaded.stored());
        reloaded.remove(8192);
        assertEquals(0, reloaded.stored());
    }

    @Test
    public void invalidAndOvervoltagePacketsDoNotChangeBalance() {
        FluxTransferBuffer buffer = new FluxTransferBuffer();
        assertEquals(0, buffer.receive(0, 1, 32, 4));
        assertEquals(0, buffer.receive(-1, 1, 32, 4));
        assertEquals(0, buffer.receive(128, 1, 32, 4));
        assertEquals(0, buffer.receive(32, Long.MIN_VALUE, 32, 4));
        assertEquals(0, buffer.stored());
    }

    @Test
    public void hugeOffersCannotOverflowAndRemainWholePackets() {
        FluxTransferBuffer buffer = new FluxTransferBuffer();
        long voltage = 2_147_483_648L;
        assertEquals(1_048_576, buffer.receive(voltage, Long.MAX_VALUE, voltage, 1_048_576));
        assertEquals(voltage * 1_048_576, buffer.stored());
    }

    @Test
    public void configuredThroughputHasNoLegacyEightHundredThousandCap() {
        FluxTransferBuffer buffer = new FluxTransferBuffer();
        assertEquals(32, buffer.receive(32768, 32, 32768, 32));
        assertEquals(1048576, buffer.stored());
        assertEquals(0, buffer.receive(32768, 1, 32768, 32));
    }

    @Test(expected = IllegalArgumentException.class)
    public void doubleDebitIsRejected() {
        FluxTransferBuffer buffer = new FluxTransferBuffer();
        buffer.receive(32, 1, 32, 1);
        buffer.remove(32);
        buffer.remove(32);
    }
}
