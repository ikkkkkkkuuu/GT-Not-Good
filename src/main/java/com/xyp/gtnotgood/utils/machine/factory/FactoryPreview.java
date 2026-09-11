package com.xyp.gtnotgood.utils.machine.factory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiPredicate;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.util.GTRecipe;

/** Read-only expected-rate preview. Material transfers follow actual graph edges rather than global cancellation. */
public final class FactoryPreview {

    private FactoryPreview() {}

    /** One recipe input or output rate; non-consumables never enter this flow network. */
    private static final class Port {

        FactoryGraph.Node node;
        ItemStack item;
        FluidStack fluid;
        GTRecipe.RecipeItemInput match;
        double rate;

        String name() {
            return item != null ? item.getDisplayName() : fluid.getLocalizedName() + " L";
        }
    }

    /** Typed preview quantities preserve item NBT and fluid identity instead of grouping by translated name. */
    public static final class Ingredient {

        public ItemStack item;
        public FluidStack fluid;
        public ItemStack display;
        public double rate;
        public boolean internal;

        public String name() {
            return item != null ? item.getDisplayName() : fluid.getLocalizedName();
        }

        public String amount() {
            if (!Double.isFinite(rate)) return "?";
            if (rate != 0 && Math.abs(rate) < 0.001) return java.math.BigDecimal.valueOf(rate)
                .round(new java.math.MathContext(3))
                .stripTrailingZeros()
                .toPlainString();
            return new java.text.DecimalFormat("#,##0.###", java.text.DecimalFormatSymbols.getInstance(Locale.ROOT))
                .format(rate);
        }
    }

    /** Independent input/output grids show the net boundary of the selected draft. */
    public static final class Snapshot {

        public final List<Ingredient> inputs = new ArrayList<>();
        public final List<Ingredient> outputs = new ArrayList<>();
        public String info = "";
        public boolean hasChance;

        /** A deterministic AE promise cannot include stochastic production or buffers that are retained internally. */
        public FactoryText exportIssue() {
            if (hasChance) return FactoryText.PATTERN_CHANCE;
            if (outputs.stream()
                .anyMatch(entry -> entry.internal)) return FactoryText.PATTERN_INTERNAL;
            return null;
        }
    }

    /**
     * Describes the configured node ratios only; runtime automatic batching never changes the preview or AE pattern.
     */
    public static Snapshot describe(FactoryGraph graph) {
        Snapshot result = new Snapshot();
        List<Port> inputs = new ArrayList<>(), outputs = new ArrayList<>();
        long totalPower = 0;
        try {
            for (FactoryGraph.Node node : graph.nodes) {
                FactoryRecipeCatalog.Entry entry = FactoryRecipeCatalog.get(node.recipe);
                if (entry == null) {
                    result.info = FactoryText.PREVIEW_INVALID.text();
                    return result;
                }
                for (int i = 0; i < entry.recipe.mOutputs.length; i++)
                    if (entry.recipe.mOutputs[i] != null && entry.recipe.mOutputs[i].stackSize > 0
                        && entry.recipe.getOutputChance(i) > 0
                        && entry.recipe.getOutputChance(i) < 10000) result.hasChance = true;
                long[] timing = FactoryGraph.timing(
                    node.customEUt < 0 ? entry.recipe.mEUt : node.customEUt,
                    entry.recipe.mDuration,
                    Math.max(1, node.parallel),
                    node.overclocks);
                totalPower = Math.addExact(totalPower, timing[0]);
                double batches = (double) Math.max(1, node.parallel) / timing[1];
                for (GTRecipe.RecipeItemInput input : entry.consumableRecipe.getCachedCombinedItemInputs()) {
                    // Keep the representative from the original inputs for display, and GT's matcher for routing.
                    for (ItemStack item : entry.consumableRecipe.mInputs) if (input.matchesType(item)) {
                        Port port = port(node, item, null, input.inputAmount * batches);
                        port.match = input;
                        inputs.add(port);
                        break;
                    }
                }
                for (FluidStack fluid : entry.recipe.mFluidInputs)
                    if (fluid != null) inputs.add(port(node, null, fluid, fluid.amount * batches));
                for (int i = 0; i < entry.recipe.mOutputs.length; i++) {
                    ItemStack item = entry.recipe.mOutputs[i];
                    if (item != null) outputs.add(
                        port(node, item, null, item.stackSize * batches * entry.recipe.getOutputChance(i) / 10000.0));
                }
                for (FluidStack fluid : entry.recipe.mFluidOutputs)
                    if (fluid != null) outputs.add(port(node, null, fluid, fluid.amount * batches));
            }
        } catch (ArithmeticException invalid) {
            result.info = FactoryText.LIMIT.text();
            return result;
        }
        result.info = totalPower + " EU/t";
        if (!graph.hasTargetsForAllComponents()) result.info += " | " + FactoryText.NO_TARGET.text();
        double[] supply = outputs.stream()
            .mapToDouble(p -> p.rate)
            .toArray();
        double[] demand = inputs.stream()
            .mapToDouble(p -> p.rate)
            .toArray();
        BiPredicate<Integer, Integer> connects = (o, i) -> {
            Port out = outputs.get(o), in = inputs.get(i);
            if (!in.node.sources.contains(out.node.id)) return false;
            return out.item != null ? in.match != null && in.match.matchesType(out.item)
                : in.fluid != null && in.fluid.isFluidEqual(out.fluid);
        };
        allocate(supply, demand, connects);
        Map<Integer, List<FactoryGraph.Node>> cycles = FactoryCycles.groups(graph);
        for (int i = 0; i < demand.length; i++)
            if (demand[i] > 1e-15) addIngredient(result.inputs, inputs.get(i), demand[i], false);
        for (int o = 0; o < supply.length; o++) {
            if (supply[o] <= 1e-15) continue;
            boolean internal = false;
            for (int i = 0; i < inputs.size(); i++) {
                int from = outputs.get(o).node.id, to = inputs.get(i).node.id;
                boolean sameCycle = cycles.containsKey(from) && cycles.get(from) == cycles.get(to);
                internal |= connects.test(o, i) && !sameCycle;
            }
            addIngredient(result.outputs, outputs.get(o), supply[o], internal);
        }
        return result;
    }

    private static void addIngredient(List<Ingredient> entries, Port port, double rate, boolean internal) {
        for (Ingredient entry : entries) {
            boolean same = port.item != null
                ? entry.item != null && port.item.isItemEqual(entry.item)
                    && ItemStack.areItemStackTagsEqual(port.item, entry.item)
                : entry.fluid != null && port.fluid.isFluidEqual(entry.fluid);
            if (same && entry.internal == internal) {
                entry.rate += rate;
                return;
            }
        }
        Ingredient entry = new Ingredient();
        entry.rate = rate;
        entry.internal = internal;
        entry.item = port.item == null ? null : port.item.copy();
        entry.fluid = port.fluid == null ? null : port.fluid.copy();
        // Hide GT's original recipe quantity: the widget draws only the parallel-adjusted throughput.
        entry.display = entry.item != null ? entry.item.copy()
            : gregtech.api.util.GTUtility.getFluidDisplayStack(entry.fluid, true, true);
        if (entry.item != null) entry.display.stackSize = 1;
        entries.add(entry);
    }

    private static Port port(FactoryGraph.Node node, ItemStack item, FluidStack fluid, double rate) {
        Port port = new Port();
        port.node = node;
        port.item = item;
        port.fluid = fluid;
        port.rate = rate;
        return port;
    }

    /** Residual arc used to reassign shared producers when another consumer has fewer eligible sources. */
    private static final class Arc {

        final int to, reverse;
        double remaining;

        Arc(int to, int reverse, double remaining) {
            this.to = to;
            this.reverse = reverse;
            this.remaining = remaining;
        }
    }

    /** Subtracts maximum feasible internal flow from supply and demand; disconnected materials never cancel. */
    static void allocate(double[] supply, double[] demand, BiPredicate<Integer, Integer> connects) {
        int sink = supply.length + demand.length + 1;
        List<List<Arc>> graph = new ArrayList<>();
        for (int i = 0; i <= sink; i++) graph.add(new ArrayList<>());
        Arc[] sources = new Arc[supply.length], targets = new Arc[demand.length];
        for (int o = 0; o < supply.length; o++) sources[o] = add(graph, 0, 1 + o, supply[o]);
        for (int i = 0; i < demand.length; i++) targets[i] = add(graph, 1 + supply.length + i, sink, demand[i]);
        for (int o = 0; o < supply.length; o++) for (int i = 0; i < demand.length; i++) {
            if (connects.test(o, i)) add(graph, 1 + o, 1 + supply.length + i, Math.min(supply[o], demand[i]));
        }
        int[] parent = new int[sink + 1], edge = new int[sink + 1], queue = new int[sink + 1];
        while (true) {
            Arrays.fill(parent, -1);
            parent[0] = 0;
            queue[0] = 0;
            int head = 0, tail = 1;
            while (head < tail && parent[sink] < 0) {
                int from = queue[head++];
                for (int e = 0; e < graph.get(from)
                    .size(); e++) {
                    Arc arc = graph.get(from)
                        .get(e);
                    if (arc.remaining <= 1e-15 || parent[arc.to] >= 0) continue;
                    parent[arc.to] = from;
                    edge[arc.to] = e;
                    queue[tail++] = arc.to;
                }
            }
            if (parent[sink] < 0) break;
            double amount = Double.POSITIVE_INFINITY;
            for (int at = sink; at != 0; at = parent[at]) amount = Math.min(
                amount,
                graph.get(parent[at])
                    .get(edge[at]).remaining);
            for (int at = sink; at != 0; at = parent[at]) {
                Arc arc = graph.get(parent[at])
                    .get(edge[at]);
                arc.remaining -= amount;
                graph.get(at)
                    .get(arc.reverse).remaining += amount;
            }
        }
        for (int o = 0; o < supply.length; o++) supply[o] = sources[o].remaining;
        for (int i = 0; i < demand.length; i++) demand[i] = targets[i].remaining;
    }

    private static Arc add(List<List<Arc>> graph, int from, int to, double amount) {
        Arc forward = new Arc(
            to,
            graph.get(to)
                .size(),
            amount);
        Arc reverse = new Arc(
            from,
            graph.get(from)
                .size(),
            0);
        graph.get(from)
            .add(forward);
        graph.get(to)
            .add(reverse);
        return forward;
    }
}
