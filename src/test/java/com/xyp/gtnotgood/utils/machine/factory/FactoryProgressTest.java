package com.xyp.gtnotgood.utils.machine.factory;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Verifies production-path timing and earned progress independently of registered recipes and client rendering. */
public class FactoryProgressTest {

    private FactoryGraph graph(int count) {
        FactoryGraph graph = new FactoryGraph();
        for (int i = 0; i < count; i++) graph.add("recipe");
        return graph;
    }

    private void job(FactoryRuntime runtime, int id, int duration, int remaining) {
        runtime.state(id).duration = duration;
        runtime.state(id).remaining = remaining;
    }

    @Test
    public void serialJobsAddAfterOverclockingAndParallelDoesNotShortenTime() {
        FactoryGraph graph = graph(2);
        graph.find(1).sources.add(0);
        graph.find(0).overclocks = 1;
        graph.find(0).parallel = 100;
        FactoryProgress progress = new FactoryProgress(graph, node -> node.id == 0 ? 80 : 10);
        assertEquals(30, progress.totalTicks());
    }

    @Test
    public void forkAndJoinUseLongestBranchWithoutCountingSharedSourcesTwice() {
        FactoryGraph graph = graph(4);
        graph.find(1).sources.add(0);
        graph.find(2).sources.add(0);
        graph.find(3).sources.add(1);
        graph.find(3).sources.add(2);
        int[] durations = { 10, 20, 40, 5 };
        assertEquals(55, new FactoryProgress(graph, node -> durations[node.id]).totalTicks());
    }

    @Test
    public void closedComponentUsesExecutorPeriodWithSerialPrefixAndSuffix() {
        FactoryGraph graph = graph(4);
        graph.find(1).sources.add(0);
        graph.find(1).sources.add(2);
        graph.find(2).sources.add(1);
        graph.find(3).sources.add(2);
        int[] durations = { 5, 12, 20, 7 };
        assertEquals(72, new FactoryProgress(graph, node -> durations[node.id]).totalTicks());
    }

    @Test
    public void refillingUpstreamDoesNotResetDownstreamProgressAndSaveRestoresCredits() {
        FactoryGraph graph = graph(2);
        graph.find(1).sources.add(0);
        FactoryProgress progress = new FactoryProgress(graph, node -> node.id == 0 ? 20 : 10);
        FactoryRuntime runtime = new FactoryRuntime();
        job(runtime, 0, 20, 1);
        progress.advance(runtime.states);
        assertEquals(20, progress.progressTicks());
        job(runtime, 0, 20, 20);
        job(runtime, 1, 10, 10);
        progress.advance(runtime.states);
        assertEquals(21, progress.progressTicks());
        FactoryProgress restored = new FactoryProgress(graph, node -> node.id == 0 ? 20 : 10);
        restored.read(progress.write());
        assertEquals(21, restored.progressTicks());
        job(runtime, 0, 20, 0);
        job(runtime, 1, 10, 1);
        restored.advance(runtime.states);
        assertEquals(30, restored.progressTicks());
        job(runtime, 1, 10, 0);
        restored.advance(runtime.states);
        assertEquals(30, restored.progressTicks());
        job(runtime, 0, 20, 20);
        restored.advance(runtime.states);
        assertEquals(1, restored.progressTicks());
    }

    @Test
    public void idleNodesCannotAdvanceProgressAndEmptyGraphHasNoDuration() {
        FactoryGraph graph = graph(2);
        graph.find(1).sources.add(0);
        FactoryProgress progress = new FactoryProgress(graph, node -> 20);
        FactoryRuntime runtime = new FactoryRuntime();
        job(runtime, 0, 20, 11);
        progress.advance(runtime.states);
        assertEquals(10, progress.progressTicks());
        job(runtime, 0, 20, 0);
        progress.advance(runtime.states);
        assertEquals(10, progress.progressTicks());
        assertEquals(0, new FactoryProgress(graph(0), node -> 20).totalTicks());
    }

    @Test
    public void serialTotalsUseLongInsteadOfWrappingAtIntegerLimit() {
        FactoryGraph graph = graph(2);
        graph.find(1).sources.add(0);
        assertEquals(2L * Integer.MAX_VALUE, new FactoryProgress(graph, node -> Integer.MAX_VALUE).totalTicks());
    }
}
