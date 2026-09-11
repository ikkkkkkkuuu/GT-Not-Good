package com.xyp.gtnotgood.utils.machine.factory;

import static org.junit.Assert.*;

import org.junit.Test;

/** Atomic BOX route scaling regressions independent of the Forge recipe registry. */
public class FactoryRoutingTest {

    @Test
    public void largeParallelSurvivesSaveAndEffectiveProductCannotWrap() {
        FactoryGraph graph = new FactoryGraph();
        graph.add("a");
        assertEquals(1, graph.nodes.get(0).parallel);
        graph.nodes.get(0).parallel = Integer.MAX_VALUE;
        assertEquals(Integer.MAX_VALUE, graph.copy().nodes.get(0).parallel);
        assertEquals(Integer.MAX_VALUE, FactoryGraph.effectiveParallel(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(10000, FactoryGraph.effectiveParallel(1000, 10));
    }

    @Test
    public void doublingOverflowLeavesEveryRouteUnchanged() {
        FactoryGraph graph = new FactoryGraph();
        graph.add("a");
        graph.add("b");
        graph.nodes.get(0).parallel = 4;
        graph.nodes.get(1).parallel = Integer.MAX_VALUE;
        assertFalse(FactoryRouting.scale(graph, true));
        assertEquals(4, graph.nodes.get(0).parallel);
        assertEquals(Integer.MAX_VALUE, graph.nodes.get(1).parallel);
    }

    @Test
    public void oddHalvingLeavesEveryRouteUnchanged() {
        FactoryGraph graph = new FactoryGraph();
        graph.add("a");
        graph.add("b");
        graph.nodes.get(0).parallel = 8;
        graph.nodes.get(1).parallel = 3;
        assertFalse(FactoryRouting.scale(graph, false));
        assertEquals(8, graph.nodes.get(0).parallel);
        assertEquals(3, graph.nodes.get(1).parallel);
    }

    @Test
    public void scalingRoundTripPreservesConfiguration() {
        FactoryGraph graph = new FactoryGraph();
        graph.add("a");
        graph.nodes.get(0).parallel = 16;
        graph.nodes.get(0).overclocks = 2;
        graph.nodes.get(0).customEUt = 128;
        String before = FactoryRouting.encode(graph);
        assertTrue(FactoryRouting.scale(graph, true));
        assertTrue(FactoryRouting.scale(graph, false));
        assertEquals(before, FactoryRouting.encode(graph));
    }

    @Test(expected = IllegalArgumentException.class)
    public void foreignCodeIsRejectedBeforeRegistryLookup() {
        FactoryRouting.decode("arbitrary nbt");
    }
}
