package com.xyp.gtnotgood.utils.machine.factory;

import static org.junit.Assert.*;

import org.junit.Test;

/** Preview routing and target selection regressions independent of Minecraft registry bootstrap. */
public class FactoryPreviewTest {

    @Test
    public void disconnectedMaterialDoesNotCancel() {
        double[] output = { 10 }, input = { 4 };
        FactoryPreview.allocate(output, input, (o, i) -> false);
        assertArrayEquals(new double[] { 10 }, output, 1e-9);
        assertArrayEquals(new double[] { 4 }, input, 1e-9);
    }

    @Test
    public void linkedLoopCancelsAndShortageRemainsExternal() {
        double[] output = { 10, 5 }, input = { 10, 8 };
        FactoryPreview.allocate(output, input, (o, i) -> o.equals(i));
        assertArrayEquals(new double[] { 0, 0 }, output, 1e-9);
        assertArrayEquals(new double[] { 0, 3 }, input, 1e-9);
    }

    @Test
    public void sharedProducerIsReassignedToAvoidFalseShortage() {
        double[] output = { 5, 5 }, input = { 5, 5 };
        FactoryPreview.allocate(output, input, (o, i) -> o == 0 || i == 0);
        assertArrayEquals(new double[] { 0, 0 }, output, 1e-9);
        assertArrayEquals(new double[] { 0, 0 }, input, 1e-9);
    }

    @Test
    public void eachComponentNeedsTargetAndTargetSurvivesCopy() {
        FactoryGraph graph = new FactoryGraph();
        graph.add("a");
        graph.add("b");
        graph.find(1).sources.add(0);
        assertFalse(graph.hasTargetsForAllComponents());
        graph.find(1).target = true;
        assertTrue(graph.hasTargetsForAllComponents());
        FactoryGraph installed = graph.copy();
        graph.find(1).target = false;
        assertTrue(installed.find(1).target);
        installed.add("separate");
        assertFalse(installed.hasTargetsForAllComponents());
        installed.find(2).target = true;
        assertTrue(installed.hasTargetsForAllComponents());
    }
}
