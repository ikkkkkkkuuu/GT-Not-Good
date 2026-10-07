package com.xyp.gtnotgood.common.wireless.monitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;

import net.minecraft.util.EnumChatFormatting;

import org.junit.Test;

import com.xyp.gtnotgood.client.wireless.WirelessMonitorFormat;
import com.xyp.gtnotgood.common.network.WirelessMonitorRequest;
import com.xyp.gtnotgood.common.network.WirelessMonitorSnapshot;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class WirelessMonitorTest {

    private static final UUID OWNER = new UUID(1, 2);

    @Test
    public void calculatesExactSmallChangesAboveLongRange() {
        WirelessEnergyHistory history = new WirelessEnergyHistory();
        BigInteger balance = BigInteger.TEN.pow(100);
        history.add(OWNER, 100, balance);
        assertNull(history.realtime());
        history.add(OWNER, 200, balance.add(BigInteger.valueOf(3200)));
        history.add(OWNER, 300, balance.add(BigInteger.valueOf(640)));
        assertEquals(new BigDecimal("-25.600000"), history.realtime());
        assertEquals(new BigDecimal("3.200000"), history.average());
        assertEquals(balance.add(BigInteger.valueOf(640)), history.energy());
    }

    @Test
    public void averageUsesOnlyLastFiveMinutes() {
        WirelessEnergyHistory history = new WirelessEnergyHistory();
        history.add(OWNER, 0, BigInteger.ZERO);
        history.add(OWNER, 100, BigInteger.valueOf(1_000_000));
        for (int i = 2; i <= 61; i++) history.add(OWNER, i * 100, BigInteger.valueOf(1_000_000L + (i - 1) * 3200L));
        assertEquals(new BigDecimal("32.000000"), history.average());
        assertEquals(new BigDecimal("32.000000"), history.realtime());
    }

    @Test
    public void teamChangeGapAndRollbackResetRates() {
        WirelessEnergyHistory history = new WirelessEnergyHistory();
        history.add(OWNER, 100, BigInteger.TEN);
        history.add(OWNER, 200, BigInteger.valueOf(20));
        history.add(new UUID(3, 4), 300, BigInteger.valueOf(9999));
        assertNull(history.average());
        history.add(new UUID(3, 4), 600, BigInteger.ONE);
        assertNull(history.realtime());
        history.add(new UUID(3, 4), 50, BigInteger.TEN);
        assertNull(history.average());
        history.clear();
        assertNull(history.energy());
    }

    @Test
    public void repeatedTickDoesNotDivideByZeroOrReplaceBalance() {
        WirelessEnergyHistory history = new WirelessEnergyHistory();
        history.add(OWNER, 100, BigInteger.TEN);
        history.add(OWNER, 100, BigInteger.ONE);
        assertEquals(BigInteger.TEN, history.energy());
        assertNull(history.average());
    }

    @Test
    public void formatsExampleAndHugePowerWithoutErrorTier() {
        String consumption = EnumChatFormatting
            .getTextWithoutFormattingCodes(WirelessMonitorFormat.rate(new BigDecimal("-4838.80"), false));
        assertEquals("-4,838.80 EU/t (2.4A EV)", consumption);
        String enormous = EnumChatFormatting
            .getTextWithoutFormattingCodes(WirelessMonitorFormat.rate(new BigDecimal(BigInteger.TEN.pow(100)), false));
        assertTrue(enormous.contains("MAX"));
        assertFalse(enormous.contains("Infinity"));
        assertEquals("29,500,080", WirelessMonitorFormat.number(new BigDecimal("29500080"), false, false));
        assertEquals("1.23×10^100", WirelessMonitorFormat.number(new BigDecimal("1.2345E100"), true, false));
    }

    @Test
    public void snapshotRoundTripPreservesOwnerTickAndHugeBalance() {
        BigInteger balance = BigInteger.TEN.pow(1000)
            .add(BigInteger.valueOf(123));
        WirelessMonitorSnapshot original = new WirelessMonitorSnapshot(19, 91234, OWNER, balance);
        ByteBuf buffer = Unpooled.buffer();
        try {
            original.toBytes(buffer);
            WirelessMonitorSnapshot restored = new WirelessMonitorSnapshot();
            restored.fromBytes(buffer);
            assertEquals(19, restored.requestId);
            assertEquals(91234, restored.tick);
            assertEquals(OWNER, restored.owner);
            assertEquals(balance, restored.energy);
        } finally {
            buffer.release();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsOversizedSnapshotBeforeAllocation() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            buffer.writeZero(32);
            buffer.writeShort(4097);
            buffer.writeZero(4097);
            new WirelessMonitorSnapshot().fromBytes(buffer);
        } finally {
            buffer.release();
        }
    }

    @Test
    public void requestOnlyContainsSessionToken() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            new WirelessMonitorRequest(91).toBytes(buffer);
            assertEquals(8, buffer.readableBytes());
            WirelessMonitorRequest restored = new WirelessMonitorRequest();
            restored.fromBytes(buffer);
            assertEquals(91, restored.requestId);
        } finally {
            buffer.release();
        }
    }
}
