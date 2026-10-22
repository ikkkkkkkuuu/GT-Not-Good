package com.xyp.gtnotgood.common.recipe.machine;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.Arrays;

import org.junit.Test;

public class LightHunterRecipeRulesTest {

    @Test
    public void nativeStationAndThreadLimitsAreRetained() {
        assertEquals(128, LightHunterRecipeRules.BASE_THREADS);
        assertEquals(274877906816L, LightHunterRecipeRules.NORMAL_PARALLEL_QUOTA);
        assertEquals(800, LightHunterRecipeRules.STATION_DURATION);
        assertEquals(536870912L, LightHunterRecipeRules.STATION_EUT);
        assertEquals(64, LightHunterRecipeRules.MAX_ASTRAL_ARRAYS);
        assertEquals(1024, LightHunterRecipeRules.COMPRESSED_ARRAY_EQUIVALENT);
    }

    @Test
    public void optionalThreadHatchAdds128ThreadsPerOrdinaryArray() {
        assertEquals(2, LightHunterRecipeRules.THREAD_MULTIPLIER);
        assertEquals(128, LightHunterRecipeRules.additionalThreads(1));
        assertEquals(8192, LightHunterRecipeRules.additionalThreads(64));
        assertEquals(0, LightHunterRecipeRules.additionalThreads(-1));
        assertEquals(8192, LightHunterRecipeRules.additionalThreads(Integer.MAX_VALUE));
        assertEquals(128, LightHunterRecipeRules.totalThreads(0));
        assertEquals(256, LightHunterRecipeRules.totalThreads(128));
        assertEquals(8320, LightHunterRecipeRules.totalThreads(8192));
        assertEquals(17867063943040L, LightHunterRecipeRules.parallelQuota(8192));
    }

    @Test
    public void threadAndQuotaArithmeticCannotOverflowAtIntegerLimits() {
        assertEquals(128, LightHunterRecipeRules.totalThreads(Integer.MIN_VALUE));
        assertEquals(Integer.MAX_VALUE, LightHunterRecipeRules.totalThreads(Integer.MAX_VALUE));
        assertEquals(4611686014132420609L, LightHunterRecipeRules.parallelQuota(Integer.MAX_VALUE));
        assertEquals(LightHunterRecipeRules.NORMAL_PARALLEL_QUOTA, LightHunterRecipeRules.parallelQuota(0));
    }

    @Test
    public void fairAllocationRedistributesQuotaAfterSmallRecipesAreSatisfied() {
        assertArrayEquals(new long[] { 1, 4, 4 },
            LightHunterRecipeRules.fairAllocation(new long[] { 1, 100, 100 }, 10));
        assertArrayEquals(new long[] { 0, 8, 8 },
            LightHunterRecipeRules.fairAllocation(new long[] { 0, 100, 100 }, 16));
        assertArrayEquals(new long[] { 1, 2, 3 },
            LightHunterRecipeRules.fairAllocation(new long[] { 1, 2, 3 }, Long.MAX_VALUE));
    }

    @Test
    public void fairAllocationLeavesRemaindersSmallerThanTheNumberOfActiveRecipes() {
        assertArrayEquals(new long[] { 3, 3, 3 },
            LightHunterRecipeRules.fairAllocation(new long[] { 100, 100, 100 }, 10));
        assertArrayEquals(new long[] { 0, 0, 0 },
            LightHunterRecipeRules.fairAllocation(new long[] { 100, 100, 100 }, 2));
        assertArrayEquals(new long[0], LightHunterRecipeRules.fairAllocation(new long[0], 10));
    }

    @Test
    public void redistributionHandlesExactly64RemainingRecipesWithoutBitShiftOverflow() {
        long[] wants = new long[65];
        Arrays.fill(wants, 1000);
        wants[64] = 0;
        long[] actual = LightHunterRecipeRules.fairAllocation(wants, 6500);
        for (int index = 0; index < 64; index++) assertEquals(101, actual[index]);
        assertEquals(0, actual[64]);
    }

    @Test
    public void maximumLongDemandsRemainWithinQuota() {
        long[] wants = new long[130];
        Arrays.fill(wants, Long.MAX_VALUE);
        long[] actual = LightHunterRecipeRules.fairAllocation(wants, Long.MAX_VALUE);
        BigInteger sum = BigInteger.ZERO;
        for (long allocation : actual) {
            assertTrue(allocation >= 0);
            sum = sum.add(BigInteger.valueOf(allocation));
        }
        assertTrue(sum.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0);
        assertTrue(BigInteger.valueOf(Long.MAX_VALUE).subtract(sum).longValueExact() < wants.length);
    }

    @Test
    public void normalBatchUsesOriginalEnergyAndMinimumDuration() {
        LightHunterRecipeRules.NormalRates small = LightHunterRecipeRules.normalRates(BigInteger.valueOf(399), 20, 20);
        assertEquals(19, small.eut());
        assertEquals(20, small.duration());
        LightHunterRecipeRules.NormalRates exact = LightHunterRecipeRules.normalRates(BigInteger.valueOf(1000), 20, 20);
        assertEquals(20, exact.eut());
        assertEquals(50, exact.duration());
        assertEquals(50, LightHunterRecipeRules.normalRates(BigInteger.valueOf(1009), 20, 20).duration());
        assertEquals(51, LightHunterRecipeRules.normalRates(BigInteger.valueOf(1010), 20, 20).duration());
    }

    @Test
    public void batchEnergyAboveLongRangeIsCalculatedBeforeNarrowing() {
        BigInteger energy = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(100));
        LightHunterRecipeRules.NormalRates rates = LightHunterRecipeRules.normalRates(energy, Long.MAX_VALUE, 20);
        assertEquals(Long.MAX_VALUE, rates.eut());
        assertEquals(100, rates.duration());
    }

    @Test(expected = ArithmeticException.class)
    public void durationOverflowIsRejectedBeforeRecipeInputsMayBeCommitted() {
        LightHunterRecipeRules.normalRates(BigInteger.valueOf(Integer.MAX_VALUE).add(BigInteger.ONE), 1, 20);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeParallelDemandIsRejected() {
        LightHunterRecipeRules.fairAllocation(new long[] { -1 }, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroVoltageCannotCreateABatch() {
        LightHunterRecipeRules.normalRates(BigInteger.ONE, 0, 20);
    }
}
