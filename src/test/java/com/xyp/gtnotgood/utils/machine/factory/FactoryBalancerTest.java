package com.xyp.gtnotgood.utils.machine.factory;

import static org.junit.Assert.*;

import org.junit.Test;

import com.xyp.gtnotgood.utils.machine.factory.FactoryBalancer.Fraction;

/** Exact-rate regressions independent of Minecraft recipe registration. */
public class FactoryBalancerTest {

    @Test
    public void ratiosAboveOldLimitAreFilled() {
        assertArrayEquals(
            new int[] { 1000, 1 },
            FactoryBalancer.solve(
                new Fraction[][] { { new Fraction(1, 1000), new Fraction(-1, 1) } },
                2,
                FactoryGraph.MAX_PARALLEL));
    }

    @Test
    public void sharedMaterialSumsProducersOnce() {
        Fraction[][] rows = FactoryBalancer.materialEquations(
            3,
            2,
            new int[] { 0, 1, 2 },
            new Fraction[] { new Fraction(1, 1), new Fraction(1, 1), new Fraction(3, 1) },
            (o, i) -> true);
        assertEquals(1, rows.length);
        assertArrayEquals(new int[] { 2, 1, 1 }, FactoryBalancer.solve(rows, 3, FactoryGraph.MAX_PARALLEL));
    }

    @Test
    public void duplicateOutputSlotsAreSummedBeforeBalancing() {
        Fraction[][] rows = FactoryBalancer.materialEquations(
            2,
            2,
            new int[] { 0, 0, 1 },
            new Fraction[] { new Fraction(1, 1), new Fraction(2, 1), new Fraction(3, 1) },
            (o, i) -> true);
        assertArrayEquals(new int[] { 1, 1 }, FactoryBalancer.solve(rows, 2, FactoryGraph.MAX_PARALLEL));
    }

    @Test
    public void differentDurationsAndProbabilitiesProduceIntegerRatios() {
        // Producer: one output / 20 ticks, 50% chance. Consumer: two inputs / 10 ticks.
        assertArrayEquals(
            new int[] { 8, 1 },
            FactoryBalancer.solve(new Fraction[][] { { new Fraction(1, 40), new Fraction(-2, 10) } }, 2, 64));
    }

    @Test
    public void splitProductionSumsBothConsumers() {
        assertArrayEquals(
            new int[] { 3, 1, 1 },
            FactoryBalancer
                .solve(new Fraction[][] { { new Fraction(1, 1), new Fraction(-1, 1), new Fraction(-2, 1) } }, 3, 64));
    }

    @Test
    public void inconsistentCyclesAndExcessParallelAreRejected() {
        assertNull(
            FactoryBalancer.solve(
                new Fraction[][] { { new Fraction(1, 1), new Fraction(-2, 1) },
                    { new Fraction(-2, 1), new Fraction(1, 1) } },
                2,
                64));
        assertNull(FactoryBalancer.solve(new Fraction[][] { { new Fraction(1, 100), new Fraction(-1, 1) } }, 2, 64));
    }

    @Test
    public void independentNodesDefaultToOneAndBalancedCyclesHavePositiveSolution() {
        assertArrayEquals(new int[] { 1, 1 }, FactoryBalancer.solve(new Fraction[0][], 2, 64));
        assertArrayEquals(
            new int[] { 2, 1 },
            FactoryBalancer.solve(
                new Fraction[][] { { new Fraction(1, 1), new Fraction(-2, 1) },
                    { new Fraction(-1, 1), new Fraction(2, 1) } },
                2,
                64));
    }
}
