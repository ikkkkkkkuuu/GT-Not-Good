package com.xyp.gtnotgood.utils.machine.factory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Automatic batching must fit energy and integer buffers without changing the configured node ratio. */
public class FactoryBatchingTest {

    @Test
    public void maximumIsACeilingAndOutputAmountsReduceIt() {
        assertEquals(Integer.MAX_VALUE, FactoryBatching.amountLimit(Integer.MAX_VALUE, 1, 0));
        assertEquals(Integer.MAX_VALUE / 144, FactoryBatching.amountLimit(Integer.MAX_VALUE, 144, 0));
        assertEquals(2, FactoryBatching.amountLimit(Integer.MAX_VALUE, 144, Integer.MAX_VALUE - 300L));
        assertEquals(0, FactoryBatching.amountLimit(Integer.MAX_VALUE, 144, Integer.MAX_VALUE));
    }

    @Test
    public void duplicateOutputTotalsCannotWrapIntoANegativeLimit() {
        assertEquals(0, FactoryBatching.amountLimit(Integer.MAX_VALUE, 2L * Integer.MAX_VALUE, 0));
        assertEquals(0, FactoryBatching.amountLimit(Integer.MAX_VALUE, 1, 2L * Integer.MAX_VALUE));
    }

    @Test
    public void powerShortagesChooseSmallerBatchAndFreeRecipesAvoidDivisionByZero() {
        assertEquals(10, FactoryBatching.powerLimit(Integer.MAX_VALUE, 128, 1280));
        assertEquals(0, FactoryBatching.powerLimit(Integer.MAX_VALUE, 128, 127));
        assertEquals(Integer.MAX_VALUE, FactoryBatching.powerLimit(Integer.MAX_VALUE, 1, Long.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, FactoryBatching.powerLimit(Integer.MAX_VALUE, 0, 0));
    }

    @Test
    public void configuredRatiosSurviveAutomaticBatchRoundingAndScarceInputsCanStillRun() {
        assertEquals(24, FactoryBatching.align(25, 6));
        assertEquals(5, FactoryBatching.align(5, 6));
        assertEquals(Integer.MAX_VALUE - 1, FactoryBatching.align(Integer.MAX_VALUE, 2));
        assertEquals(Integer.MAX_VALUE, FactoryBatching.align(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    public void selectedBatchAlwaysFitsAndNextBatchWouldExceedItsTightestBound() {
        for (int amount : new int[] { 1, 2, 144, 1000, 1000000, Integer.MAX_VALUE }) {
            for (int stored : new int[] { 0, 1, 10000, Integer.MAX_VALUE - 1 }) {
                int limit = FactoryBatching.amountLimit(Integer.MAX_VALUE, amount, stored);
                assertTrue((long) limit * amount + stored <= Integer.MAX_VALUE);
                assertTrue(((long) limit + 1) * amount + stored > Integer.MAX_VALUE);
            }
        }
    }
}
