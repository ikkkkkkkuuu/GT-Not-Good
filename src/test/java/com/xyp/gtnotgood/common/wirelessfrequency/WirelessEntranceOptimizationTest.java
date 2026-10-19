package com.xyp.gtnotgood.common.wirelessfrequency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.Test;

import com.xyp.gtnotgood.common.wirelessfrequency.AutomaticEntrancePlanner.Action;
import com.xyp.gtnotgood.common.wirelessfrequency.AutomaticEntrancePlanner.Decision;
import com.xyp.gtnotgood.common.wirelessfrequency.AutomaticEntrancePlanner.Observation;
import com.xyp.gtnotgood.common.wirelessfrequency.AutomaticEntrancePlanner.Status;

public class WirelessEntranceOptimizationTest {

    @Test
    public void batchHonorsDemandBudgetAndEntranceLimit() {
        assertEquals(3, AutomaticEntrancePlanner.batchSize(observation(10, 1, 0, 0, 65, 8)));
        assertEquals(2, AutomaticEntrancePlanner.batchSize(observation(10, 1, 223, 0, 128, 8)));
        assertEquals(1, AutomaticEntrancePlanner.batchSize(observation(10, 1, 0, 0, 128, 1)));
        assertEquals(8, AutomaticEntrancePlanner.batchSize(observation(10, 1, 0, 0, 512, 16)));

        AutomaticEntrancePlanner<Object> planner = new AutomaticEntrancePlanner<>();
        assertEquals(Action.NONE, planner.decide(observation(0, 1, 0, 0, 65, 0),
            eligible -> { throw new AssertionError("Selection must stop at the entrance limit"); }).action());
        assertEquals(Status.ENTRANCE_LIMIT, planner.status());
    }

    @Test
    public void batchTrialWaitsForNewAllocationThenKeepsAllGains() {
        AutomaticEntrancePlanner<Object> planner = new AutomaticEntrancePlanner<>();
        List<Object> candidates = Arrays.asList(new Object(), new Object(), new Object());
        planner.decide(observation(0, 1, 0, 0, 65, 8), eligible -> choose(candidates, eligible));
        Observation before = observation(10, 1, 0, 0, 65, 8);
        Decision<Object> add = planner.decide(before, eligible -> choose(candidates, eligible));
        assertEquals(Action.ADD, add.action());
        assertEquals(candidates, add.targets());

        List<Integer> ids = new ArrayList<>(Arrays.asList(7, 8, 9));
        planner.created(add.targets(), ids, before);
        ids.clear();
        assertTrue(planner.hasTrial());
        assertTrue(planner.isPending(8));
        assertEquals(Arrays.asList(7, 8, 9), planner.pendingEntrances());
        assertEquals(Action.NONE, planner.decide(observation(11, 1, 64, 64, 1, 5), eligible -> null).action());

        Decision<Object> keep = planner.decide(observation(12, 2, 64, 64, 1, 5), eligible -> null);
        assertEquals(Action.KEEP, keep.action());
        assertEquals(candidates, keep.targets());
        assertEquals(Arrays.asList(7, 8, 9), keep.entranceIds());
        assertFalse(planner.hasTrial());
    }

    @Test
    public void batchTrialRollsBackAndRejectsEveryTarget() {
        AutomaticEntrancePlanner<Object> planner = new AutomaticEntrancePlanner<>();
        List<Object> candidates = Arrays.asList(new Object(), new Object(), new Object());
        planner.decide(observation(0, 1, 0, 0, 65, 8), eligible -> choose(candidates, eligible));
        Observation before = observation(10, 1, 0, 0, 65, 8);
        Decision<Object> add = planner.decide(before, eligible -> choose(candidates, eligible));
        planner.created(add.targets(), Arrays.asList(7, 8, 9), before);

        Decision<Object> remove = planner.decide(observation(11, 2, 0, 0, 65, 5), eligible -> null);
        assertEquals(Action.REMOVE, remove.action());
        assertEquals(candidates, remove.targets());
        assertEquals(Arrays.asList(7, 8, 9), remove.entranceIds());
        assertEquals(Status.ROLLING_BACK, planner.status());
        assertEquals(Action.NONE, planner.decide(observation(12, 2, 0, 0, 65, 8),
            eligible -> { throw new AssertionError("Removal must wait for another allocation"); }).action());
        assertEquals(Action.NONE, planner.decide(observation(13, 3, 0, 0, 65, 8), eligible -> {
            for (Object candidate : candidates) assertFalse(eligible.test(candidate));
            return choose(candidates, eligible);
        }).action());
        assertEquals(Status.NO_CANDIDATE, planner.status());
    }

    @Test
    public void candidatePrefersCaptureWithinCapacityOverRank() {
        List<Node> nodes = nodes(9);
        connect(nodes.get(0), nodes.get(1));
        connect(nodes.get(0), nodes.get(2));
        for (int index = 2; index < 8; index++) connect(nodes.get(index), nodes.get(index + 1));
        Set<Node> members = identitySet(nodes);
        List<Node> missing = Arrays.asList(nodes.get(0), nodes.get(8));
        Predicate<Node> eligible = node -> node.id == 1 || node.id == 2;
        Comparator<Node> order = Comparator.comparingInt(node -> node.id);

        assertSame(nodes.get(2), EntranceCandidates.select(members, missing, Collections.emptyList(),
            node -> node.neighbours, eligible, node -> node.id == 1 ? 0 : 1, node -> 32, order));
        assertSame(nodes.get(1), EntranceCandidates.select(members, missing, Collections.emptyList(),
            node -> node.neighbours, eligible, node -> node.id == 1 ? 0 : 1, node -> node.id == 2 ? 1 : 32, order));
    }

    @Test
    public void batchReusesDistancesAfterOccupyingEntrance() {
        List<Node> nodes = nodes(51);
        for (int index = 0; index < 50; index++) connect(nodes.get(index), nodes.get(index + 1));
        Set<Node> occupied = identitySet(Collections.singletonList(nodes.get(0)));
        int[] visitsToOriginalEntrance = { 0 };
        EntranceCandidates.Batch<Node> batch = new EntranceCandidates.Batch<>(identitySet(nodes), occupied, node -> {
            if (node == nodes.get(0)) visitsToOriginalEntrance[0]++;
            return node.neighbours;
        });
        List<Node> missing = Arrays.asList(nodes.get(10), nodes.get(40));
        Comparator<Node> order = Comparator.comparingInt(node -> node.id);
        Node first = batch.select(missing, node -> !occupied.contains(node), node -> 0, node -> 32, order);
        assertSame(nodes.get(40), first);
        occupied.add(first);
        batch.occupy(first);
        assertSame(nodes.get(10),
            batch.select(missing, node -> !occupied.contains(node), node -> 0, node -> 32, order));
        assertEquals(1, visitsToOriginalEntrance[0]);
    }

    private static Observation observation(long tick, long revision, int used, int served, int missing, int remaining) {
        return new Observation(1, revision, tick, true, true, true, 256, used, served, missing, remaining);
    }

    private static Object choose(List<Object> candidates, Predicate<Object> eligible) {
        return candidates.stream().filter(eligible).findFirst().orElse(null);
    }

    private static List<Node> nodes(int count) {
        List<Node> nodes = new ArrayList<>();
        for (int index = 0; index < count; index++) nodes.add(new Node(index));
        return nodes;
    }

    private static Set<Node> identitySet(List<Node> nodes) {
        Set<Node> result = Collections.newSetFromMap(new IdentityHashMap<>());
        result.addAll(nodes);
        return result;
    }

    private static void connect(Node left, Node right) {
        left.neighbours.add(right);
        right.neighbours.add(left);
    }

    private static class Node {

        final int id;
        final List<Node> neighbours = new ArrayList<>();

        Node(int id) {
            this.id = id;
        }
    }
}
