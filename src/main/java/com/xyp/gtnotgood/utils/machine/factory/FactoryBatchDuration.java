package com.xyp.gtnotgood.utils.machine.factory;

import java.util.Arrays;
import java.util.List;
import java.util.function.ToIntFunction;

/** Serial stages add, concurrent branches overlap, and a closed component visits each recipe once. */
public final class FactoryBatchDuration {

    private FactoryBatchDuration() {}

    /**
     * Computes a whole-page processing barrier without treating wholeLineBatch membership as a material edge.
     * Each genuine cycle is collapsed to one round containing all of its recipe durations.
     *
     * @param nodes    page nodes and their actual material routes
     * @param duration processing ticks after overclocking
     * @return longest path in the condensed graph, at least one tick
     * @throws ArithmeticException if the total cannot fit a persisted processing duration
     */
    public static int ticks(List<FactoryGraph.Node> nodes, ToIntFunction<FactoryGraph.Node> duration) {
        int size = nodes.size();
        boolean[][] reach = new boolean[size][size];
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++) reach[i][j] = i == j || nodes.get(j).sources.contains(nodes.get(i).id);
        for (int k = 0; k < size; k++)
            for (int i = 0; i < size; i++) for (int j = 0; j < size; j++) reach[i][j] |= reach[i][k] && reach[k][j];
        int[] component = new int[size], costs = new int[size];
        Arrays.fill(component, -1);
        int count = 0;
        for (int i = 0; i < size; i++) {
            if (component[i] >= 0) continue;
            for (int j = i; j < size; j++) if (reach[i][j] && reach[j][i]) {
                component[j] = count;
                costs[count] = Math.addExact(costs[count], Math.max(1, duration.applyAsInt(nodes.get(j))));
            }
            count++;
        }
        boolean[][] predecessors = new boolean[count][count];
        for (int i = 0; i < size; i++) for (int j = 0; j < size; j++)
            if (component[i] != component[j] && nodes.get(j).sources.contains(nodes.get(i).id))
                predecessors[component[j]][component[i]] = true;
        int[] finishes = new int[count];
        int result = 1;
        for (int i = 0; i < count; i++) result = Math.max(result, finish(i, costs, predecessors, finishes));
        return result;
    }

    private static int finish(int stage, int[] costs, boolean[][] predecessors, int[] finishes) {
        if (finishes[stage] > 0) return finishes[stage];
        int start = 0;
        for (int i = 0; i < finishes.length; i++)
            if (predecessors[stage][i]) start = Math.max(start, finish(i, costs, predecessors, finishes));
        return finishes[stage] = Math.addExact(start, costs[stage]);
    }
}
