package com.xyp.gtnotgood.utils.machine.factory;

import static org.junit.Assert.*;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

/** Regression coverage for graph isolation, accumulation, paid-tick completion and durable material accounting. */
public class FactoryStateTest {

    @Test
    public void customNodeCostPersistsAndLegacyNodesUseRecipeDefault() {
        FactoryGraph draft = new FactoryGraph();
        draft.add("recipe");
        draft.find(0).customEUt = 123456789012L;
        FactoryGraph installed = draft.copy();
        assertEquals(123456789012L, installed.find(0).customEUt);
        draft.find(0).customEUt = 0;
        assertEquals(123456789012L, installed.find(0).customEUt);
        assertEquals(
            0L,
            draft.copy()
                .find(0).customEUt);
        NBTTagCompound legacy = draft.write();
        legacy.getTagList("nodes", 10)
            .getCompoundTagAt(0)
            .removeTag("customEUt");
        draft.read(legacy);
        assertEquals(-1L, draft.find(0).customEUt);
        assertArrayEquals(new long[] { 8192, 5 }, FactoryGraph.timing(32, 20, 4 * 16, 1));
    }

    @Test
    public void emptyDraftSurvivesSaveAndRecipeReplacementKeepsNodeConfiguration() {
        FactoryGraph draft = new FactoryGraph();
        draft.add("upstream");
        draft.add("");
        FactoryGraph.Node node = draft.find(1);
        node.sources.add(0);
        node.parallel = 7;
        node.overclocks = 3;
        node.x = 120;
        node.y = 80;
        FactoryGraph restored = draft.copy();
        assertEquals("", restored.find(1).recipe);
        restored.find(1).recipe = "nei-import";
        FactoryGraph saved = restored.copy();
        assertEquals("nei-import", saved.find(1).recipe);
        assertEquals(7, saved.find(1).parallel);
        assertEquals(3, saved.find(1).overclocks);
        assertEquals(120, saved.find(1).x);
        assertEquals(80, saved.find(1).y);
        assertTrue(saved.find(1).sources.contains(0));
        assertEquals("", draft.find(1).recipe);
    }

    @Test
    public void submittedGraphIsIsolatedAndDeletedIdsAreNotReused() {
        FactoryGraph draft = new FactoryGraph();
        draft.add("a");
        draft.add("b");
        draft.find(1).sources.add(0);
        FactoryGraph installed = draft.copy();
        draft.remove(0);
        draft.add("c");
        assertNotNull(installed.find(0));
        assertTrue(installed.find(1).sources.contains(0));
        assertFalse(draft.find(1).sources.contains(0));
        assertNotNull(draft.find(2));
    }

    @Test
    public void saveRoundTripPreservesCyclesAndClampsUnsafeSettings() {
        FactoryGraph graph = new FactoryGraph();
        graph.add("a");
        graph.add("b");
        graph.find(0).sources.add(1);
        graph.find(1).sources.add(0);
        graph.find(0).sources.add(99);
        graph.find(0).parallel = Integer.MAX_VALUE;
        graph.find(1).overclocks = -100;
        FactoryGraph restored = graph.copy();
        assertEquals(Integer.MAX_VALUE, restored.find(0).parallel);
        assertEquals(0, restored.find(1).overclocks);
        assertEquals(1, restored.find(0).sources.size());
        assertTrue(restored.find(1).sources.contains(0));
        for (int i = 0; i < 100; i++) restored.add("x");
        assertEquals(FactoryGraph.MAX_NODES, restored.nodes.size());
    }

    @Test
    public void overclocksStopAtOneTickAndRejectOverflow() {
        assertArrayEquals(new long[] { 512, 1 }, FactoryGraph.timing(8, 16, 4, 14));
        try {
            FactoryGraph.timing(Long.MAX_VALUE, 20, 2, 0);
            fail("Overflow must be rejected before inputs are consumed");
        } catch (ArithmeticException expected) {}
    }

    @Test
    public void incompleteJobSurvivesSaveAndExposesOutputsOnlyAfterPaidTicks() {
        FactoryRuntime runtime = new FactoryRuntime();
        FactoryRuntime.State job = runtime.state(3);
        job.eut = 32;
        job.remaining = job.duration = 2;
        assertTrue(job.items.isEmpty());
        runtime.advance();
        assertEquals(32, runtime.totalEUt());
        NBTTagCompound saved = runtime.write();
        FactoryRuntime restored = new FactoryRuntime();
        restored.read(saved);
        assertEquals(1, restored.state(3).remaining);
        assertEquals(32, restored.totalEUt());
        restored.advance();
        restored.advance();
        assertEquals(0, restored.totalEUt());
        assertEquals(0, restored.state(3).remaining);
    }

    @Test
    public void upstreamBatchesAccumulateAndRemainAvailableDuringNextJob() {
        FactoryRuntime runtime = new FactoryRuntime();
        for (int i = 0; i < 4; i++) {
            FactoryRuntime.State job = new FactoryRuntime.State();
            job.remaining = job.duration = 1;
            job.eut = 8;
            job.pendingItems.add(stack(100));
            runtime.checkCapacity(0, job);
            runtime.start(0, job);
            if (i > 0) assertEquals(i * 100, runtime.state(0).items.get(0).stackSize);
            runtime.advance();
        }
        assertEquals(400, runtime.state(0).items.get(0).stackSize);
        runtime.state(0).items.get(0).stackSize -= 400;
        assertTrue(runtime.empty());
    }

    @Test
    public void reservationsCannotOverflowExistingBuffers() {
        FactoryRuntime runtime = new FactoryRuntime();
        runtime.state(0).items.add(stack(Integer.MAX_VALUE));
        FactoryRuntime.State job = new FactoryRuntime.State();
        job.pendingItems.add(stack(1));
        try {
            runtime.checkCapacity(0, job);
            fail("Reservation must reject an overflowing output");
        } catch (ArithmeticException expected) {}
        assertEquals(Integer.MAX_VALUE, runtime.state(0).items.get(0).stackSize);
    }

    /** Equal unregistered stacks exercise stack arithmetic without a Forge launch classloader. */
    private static ItemStack stack(int amount) {
        return new ItemStack((Item) null, amount, 0);
    }
}
