package com.xyp.gtnotgood.utils.machine.factory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Tracks one traversal of a continuously scheduled line without imposing a batch barrier on its jobs.
 * Serial costs add and concurrent branches use their longest path. Closed components use the same
 * common period as the net-material executor. Credits survive upstream refills and power interruptions;
 * they describe work completed across the line, not the age of an individually traceable output item.
 */
public final class FactoryProgress {

    /** One node or an atomic closed component in the condensed, acyclic production graph. */
    private static final class Stage {

        int id;
        int duration;
        int credited;
        final Set<Stage> sources = new HashSet<>();
    }

    private final Map<Integer, Stage> byNode = new HashMap<>();
    private final List<Stage> stages = new ArrayList<>();
    private final long total;

    /**
     * Builds a cached timing plan for an installed graph. Parallel counts do not shorten batch duration.
     *
     * @param graph        installed routing, unchanged for the lifetime of this plan
     * @param baseDuration original recipe duration lookup, before node overclocking
     * @throws ArithmeticException if a closed component exceeds the executor's supported period
     */
    public FactoryProgress(FactoryGraph graph, ToIntFunction<FactoryGraph.Node> baseDuration) {
        Map<Integer, List<FactoryGraph.Node>> cycles = FactoryCycles.groups(graph);
        for (FactoryGraph.Node node : graph.nodes) {
            if (byNode.containsKey(node.id)) continue;
            List<FactoryGraph.Node> members = cycles.getOrDefault(node.id, java.util.Collections.singletonList(node));
            Stage stage = new Stage();
            stage.id = members.get(0).id;
            int[] durations = new int[members.size()];
            for (int i = 0; i < members.size(); i++) {
                FactoryGraph.Node member = members.get(i);
                durations[i] = (int) FactoryGraph.timing(0, baseDuration.applyAsInt(member), 1, member.overclocks)[1];
                byNode.put(member.id, stage);
            }
            stage.duration = FactoryCycles.commonPeriod(durations);
            stages.add(stage);
        }
        for (FactoryGraph.Node node : graph.nodes) {
            Stage stage = byNode.get(node.id);
            for (int id : node.sources) {
                Stage source = byNode.get(id);
                if (source != null && source != stage) stage.sources.add(source);
            }
        }
        total = longestPath(false);
    }

    /**
     * Records the tick about to be advanced by the runtime. Call only after energy payment succeeds.
     * A new traversal starts when all stages completed and another job is actually running.
     *
     * @param jobs current jobs before FactoryRuntime.advance decrements their remaining ticks
     */
    public void advance(Map<Integer, FactoryRuntime.State> jobs) {
        boolean active = jobs.values()
            .stream()
            .anyMatch(job -> job.remaining > 0);
        if (!active) return;
        if (stages.stream()
            .allMatch(stage -> stage.credited == stage.duration)) for (Stage stage : stages) stage.credited = 0;
        for (Map.Entry<Integer, FactoryRuntime.State> entry : jobs.entrySet()) {
            Stage stage = byNode.get(entry.getKey());
            FactoryRuntime.State job = entry.getValue();
            if (stage == null || job.remaining <= 0) continue;
            stage.credited = Math.max(stage.credited, Math.min(stage.duration, job.duration - job.remaining + 1));
        }
    }

    public long totalTicks() {
        return total;
    }

    public long progressTicks() {
        return total - longestPath(true);
    }

    private long longestPath(boolean remaining) {
        Map<Stage, Long> cache = new LinkedHashMap<>();
        long result = 0;
        for (Stage stage : stages) result = Math.max(result, finish(stage, remaining, cache));
        return result;
    }

    private long finish(Stage stage, boolean remaining, Map<Stage, Long> cache) {
        Long known = cache.get(stage);
        if (known != null) return known;
        long start = 0;
        for (Stage source : stage.sources) start = Math.max(start, finish(source, remaining, cache));
        long result = start + stage.duration - (remaining ? stage.credited : 0);
        cache.put(stage, result);
        return result;
    }

    /** Saves earned stage credits separately from node jobs, which may already be processing their next refill. */
    public NBTTagCompound write() {
        NBTTagCompound tag = new NBTTagCompound();
        for (Stage stage : stages) tag.setInteger(Integer.toString(stage.id), stage.credited);
        return tag;
    }

    public void read(NBTTagCompound tag) {
        for (Stage stage : stages)
            stage.credited = Math.max(0, Math.min(stage.duration, tag.getInteger(Integer.toString(stage.id))));
    }
}
