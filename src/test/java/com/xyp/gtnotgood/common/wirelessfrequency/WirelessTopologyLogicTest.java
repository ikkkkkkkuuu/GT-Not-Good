package com.xyp.gtnotgood.common.wirelessfrequency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.xyp.gtnotgood.common.wirelessfrequency.AutomaticEntrancePlanner.Action;
import com.xyp.gtnotgood.common.wirelessfrequency.AutomaticEntrancePlanner.Observation;
import com.xyp.gtnotgood.common.wirelessfrequency.ChannelBudget.Position;
import com.xyp.gtnotgood.common.wirelessfrequency.PhysicalClusterTracker.Cluster;
import com.xyp.gtnotgood.common.wirelessfrequency.PhysicalClusterTracker.Topology;

public class WirelessTopologyLogicTest {

    @Test
    public void controllerFacesShareOneChannelBudget() {
        Position first = new Position(0, 10, 20, 30);
        Position adjacent = new Position(0, 11, 20, 30);
        Position otherDimension = new Position(1, 11, 20, 30);
        assertEquals(320, ChannelBudget.capacityOf(new HashSet<>(Arrays.asList(first, adjacent))));
        assertEquals(384, ChannelBudget.capacityOf(new HashSet<>(Arrays.asList(first, otherDimension))));

        ChannelBudget budget = new ChannelBudget(1);
        assertTrue(budget.hasRemaining());
        budget.recordAllocation();
        assertFalse(budget.hasRemaining());
        assertEquals(1, budget.allocated());
    }

    @Test
    public void mergingDifferentFrequenciesPreservesClaimsAfterSplit() {
        Object left = new Object();
        Object right = new Object();
        Map<Object, Set<Object>> neighbours = new HashMap<>();
        neighbours.put(left, Collections.emptySet());
        neighbours.put(right, Collections.emptySet());
        Topology<Object> topology = new Topology<Object>() {

            @Override
            public boolean isLive(Object node) {
                return neighbours.containsKey(node);
            }

            @Override
            public Iterable<Object> neighbours(Object node) {
                return neighbours.get(node);
            }

            @Override
            public boolean blocksWireless(Object node) {
                return false;
            }
        };
        PhysicalClusterTracker<Object, String> tracker = new PhysicalClusterTracker<>(10);
        tracker.refresh(topology, Arrays.asList(left, right), node -> Collections.singleton(node == left ? "a" : "b"),
            frequency -> frequency);
        neighbours.put(left, Collections.singleton(right));
        neighbours.put(right, Collections.singleton(left));
        tracker.refresh(topology, Arrays.asList(left, right), node -> Collections.emptySet(), frequency -> frequency);

        Cluster<Object, String> merged = tracker.clusterOf(left);
        assertSame(merged, tracker.clusterOf(right));
        assertTrue(merged.conflicted());
        assertEquals(Collections.singleton("a"), tracker.claimsOf(left));
        assertEquals(Collections.singleton("b"), tracker.claimsOf(right));

        neighbours.put(left, Collections.emptySet());
        neighbours.put(right, Collections.emptySet());
        tracker.refresh(topology, Arrays.asList(left, right), node -> Collections.emptySet(), frequency -> frequency);
        assertEquals(Collections.singleton("a"), tracker.clusterOf(left).frequencies());
        assertEquals(Collections.singleton("b"), tracker.clusterOf(right).frequencies());
    }

    @Test
    public void entranceTrialKeepsOnlyAnObservedChannelGain() {
        AutomaticEntrancePlanner<Object> planner = new AutomaticEntrancePlanner<>();
        Object first = new Object();
        Observation before = new Observation(1, 1, 10, true, true, true, 32, 0, 0, 1, 1);
        assertEquals(Action.NONE,
            planner.decide(new Observation(1, 1, 0, true, true, true, 32, 0, 0, 1, 1), eligible -> first).action());
        assertEquals(Action.ADD, planner.decide(before, eligible -> first).action());
        planner.created(Collections.singletonList(first), Collections.singletonList(7), before);
        assertEquals(Action.KEEP,
            planner.decide(new Observation(1, 2, 11, true, true, true, 32, 1, 1, 0, 1), eligible -> null).action());

        Observation next = new Observation(1, 3, 12, true, true, true, 32, 1, 1, 1, 1);
        assertEquals(Action.ADD, planner.decide(next, eligible -> first).action());
        planner.created(Collections.singletonList(first), Collections.singletonList(8), next);
        assertEquals(Action.REMOVE,
            planner.decide(new Observation(1, 4, 13, true, true, true, 32, 1, 1, 1, 1), eligible -> null).action());
        assertEquals(AutomaticEntrancePlanner.Status.ROLLING_BACK, planner.status());
    }
}
