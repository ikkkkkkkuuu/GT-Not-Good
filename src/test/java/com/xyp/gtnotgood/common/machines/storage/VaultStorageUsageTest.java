package com.xyp.gtnotgood.common.machines.storage;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import java.util.Random;

import org.junit.Test;

public class VaultStorageUsageTest {

    @Test
    public void simulationsPreserveCapacityAndPartialBytesRemainUsable() {
        VaultStorageUsage usage = new VaultStorageUsage(9, 8);
        assertEquals(8, usage.insertableAmount(0, 100, 8));
        assertEquals(8, usage.insertableAmount(0, 100, 8));
        assertEquals(0, usage.usedBytes());
        usage.update(0, 1, 8);
        assertEquals(7, usage.insertableAmount(1, 100, 8));
        assertEquals(0, usage.insertableAmount(0, 1, 2048));
        usage.update(1, 8, 8);
        assertEquals(0, usage.insertableAmount(8, 1, 8));
        usage.update(8, 0, 8);
        assertEquals(2048, usage.insertableAmount(0, 10_000, 2048));
    }

    @Test
    public void hugeCapacityNeverWrapsAcceptedQuantity() {
        VaultStorageUsage usage = new VaultStorageUsage(Long.MAX_VALUE, 8);
        assertEquals(Long.MAX_VALUE, usage.insertableAmount(0, Long.MAX_VALUE, 2048));
        usage.update(0, Long.MAX_VALUE - 2, 8);
        assertEquals(2, usage.insertableAmount(Long.MAX_VALUE - 2, 100, 8));
        assertEquals(0, usage.insertableAmount(Long.MAX_VALUE - 2, -1, 8));
    }

    @Test
    public void itemAndFluidRoundingReleasesTypeOverheadOnDepletion() {
        VaultStorageUsage usage = new VaultStorageUsage(1_000, 8);
        usage.update(0, 8, 8);
        usage.update(0, 2049, 2048);
        assertEquals(19, usage.usedBytes());
        usage.update(8, 9, 8);
        assertEquals(20, usage.usedBytes());
        usage.update(2049, 2048, 2048);
        assertEquals(19, usage.usedBytes());
        usage.update(9, 0, 8);
        assertEquals(9, usage.usedBytes());
        usage.update(2048, 0, 2048);
        assertEquals(0, usage.usedBytes());
        usage.update(0, 1, 8);
        assertEquals(9, usage.usedBytes());
    }

    @Test
    public void legacyOverCapacityTotalsRemainFullUntilEnoughIsExtracted() {
        VaultStorageUsage usage = new VaultStorageUsage(100, 8);
        usage.update(0, 800, 8);
        usage.update(0, 800, 8);
        assertEquals(100, usage.usedBytes());
        usage.update(800, 0, 8);
        assertEquals(100, usage.usedBytes());
        usage.update(800, 799, 8);
        assertEquals(1, usage.insertableAmount(799, 100, 8));
        assertEquals(0, usage.insertableAmount(0, 1, 8));
        usage.update(799, 720, 8);
        assertEquals(98, usage.usedBytes());
    }

    @Test
    public void totalsBeyondLongRangeSurviveExtractionAndReload() {
        VaultStorageUsage usage = new VaultStorageUsage(Long.MAX_VALUE, 8);
        for (int i = 0; i < 16; i++) usage.update(0, Long.MAX_VALUE, 8);
        assertEquals(Long.MAX_VALUE, usage.usedBytes());
        for (int i = 0; i < 15; i++) usage.update(Long.MAX_VALUE, 0, 8);
        assertEquals(1_152_921_504_606_846_984L, usage.usedBytes());
        usage.clear();
        assertEquals(0, usage.usedBytes());
        usage.update(0, 2048, 2048);
        assertEquals(9, usage.usedBytes());
    }

    @Test
    public void mixedTransfersMatchFullInventoryRecount() {
        long capacity = 50_000;
        VaultStorageUsage usage = new VaultStorageUsage(capacity, 8);
        long[] quantities = new long[100];
        Random random = new Random(4857);
        for (int operation = 0; operation < 2_000; operation++) {
            int type = random.nextInt(quantities.length);
            int units = type < 50 ? 8 : 2048;
            long after = random.nextInt(4) == 0 ? 0 : random.nextInt(100_000);
            usage.update(quantities[type], after, units);
            quantities[type] = after;
            BigInteger expected = BigInteger.ZERO;
            for (int i = 0; i < quantities.length; i++) {
                if (quantities[i] == 0) continue;
                long divisor = i < 50 ? 8 : 2048;
                expected = expected.add(BigInteger.valueOf(8 + (quantities[i] + divisor - 1) / divisor));
            }
            assertEquals(expected.min(BigInteger.valueOf(capacity)).longValue(), usage.usedBytes());
        }
    }
}
