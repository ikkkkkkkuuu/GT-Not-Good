package com.xyp.gtnotgood.utils.machine.factory;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.junit.Test;

/** Loop grouping and common-period arithmetic underpin seed-free, net-material batches. */
public class FactoryCyclesTest {

    @Test
    public void onlyClosedComponentsAreGrouped() {
        FactoryGraph graph = new FactoryGraph();
        for (int i = 0; i < 4; i++) graph.add("recipe");
        graph.find(1).sources.add(0);
        graph.find(0).sources.add(1);
        graph.find(2).sources.add(1);
        graph.find(3).sources.add(3);
        Map<Integer, List<FactoryGraph.Node>> groups = FactoryCycles.groups(graph);
        assertSame(groups.get(0), groups.get(1));
        assertEquals(
            2,
            groups.get(0)
                .size());
        assertFalse(groups.containsKey(2));
        assertEquals(
            1,
            groups.get(3)
                .size());
    }

    @Test
    public void fullPeriodHasWholeBatchCountsAndRejectsOverflow() {
        assertEquals(60, FactoryCycles.commonPeriod(new int[] { 12, 20, 15 }));
        try {
            FactoryCycles.commonPeriod(new int[] { Integer.MAX_VALUE, 2 });
            fail("Overflow must be rejected before any real input is consumed");
        } catch (ArithmeticException expected) {}
    }

    @Test
    public void loopNetSettlementRequiresOnlyRealDeficit() {
        double[] output = { 40, 30 };
        double[] input = { 30, 50 };
        FactoryPreview.allocate(output, input, (o, i) -> o.equals(i));
        assertArrayEquals(new double[] { 10, 0 }, output, 0);
        assertArrayEquals(new double[] { 0, 20 }, input, 0);
    }
}
