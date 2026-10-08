package com.xyp.gtnotgood.utils.machine.factory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.util.GTRecipe;

/** Closed graph components run as net-material batches, so internal circulation needs no physical startup seed. */
public final class FactoryCycles {

    private FactoryCycles() {}

    /** Finds strongly connected components, including self-loops, without depending on recipe registration. */
    public static Map<Integer, List<FactoryGraph.Node>> groups(FactoryGraph graph) {
        int n = graph.nodes.size();
        boolean[][] reach = new boolean[n][n];
        for (int a = 0; a < n; a++) for (int b = 0; b < n; b++) {
            FactoryGraph.Node from = graph.nodes.get(a), to = graph.nodes.get(b);
            reach[a][b] = to.sources.contains(from.id)
                || (from.wholeLineBatch && to.wholeLineBatch && from.page == to.page);
        }
        for (int k = 0; k < n; k++)
            for (int a = 0; a < n; a++) for (int b = 0; b < n; b++) reach[a][b] |= reach[a][k] && reach[k][b];
        Map<Integer, List<FactoryGraph.Node>> result = new HashMap<>();
        for (int a = 0; a < n; a++) {
            if (!reach[a][a] || result.containsKey(graph.nodes.get(a).id)) continue;
            List<FactoryGraph.Node> group = new ArrayList<>();
            for (int b = 0; b < n; b++) if (reach[a][b] && reach[b][a]) group.add(graph.nodes.get(b));
            for (FactoryGraph.Node node : group) result.put(node.id, group);
        }
        return result;
    }

    /** Fully planned outputs are held until the common period has been paid; only net inputs are consumed. */
    public static final class Plan {

        public GTRecipe inputs;
        public final Map<Integer, FactoryRuntime.State> jobs = new LinkedHashMap<>();
        public long eut;
        public long voltage;
    }

    /** A typed amount belongs to its actual node, so cancellation cannot cross missing graph edges. */
    private static final class Port {

        FactoryGraph.Node node;
        ItemStack item;
        FluidStack fluid;
        GTRecipe.RecipeItemInput match;

        long amount() {
            return item != null ? item.stackSize : fluid.amount;
        }

        void amount(int value) {
            if (item != null) item.stackSize = value;
            else fluid.amount = value;
        }
    }

    /** Builds a whole number of each node's batches over the least common period, with exact overflow checks. */
    public static Plan prepare(List<FactoryGraph.Node> group, int multiplier, Random random) {
        if (!group.isEmpty() && group.stream().allMatch(node -> node.wholeLineBatch))
            return scale(FactoryWholeBatch.prepare(group), multiplier);
        Plan plan = new Plan();
        int[] durations = new int[group.size()];
        for (int i = 0; i < group.size(); i++) {
            FactoryGraph.Node node = group.get(i);
            FactoryRecipeCatalog.Entry entry = FactoryRecipeCatalog.get(node.recipe);
            if (entry == null) throw new ArithmeticException("Missing cycle recipe");
            durations[i] = (int) FactoryGraph.timing(1, entry.recipe.mDuration, 1, node.overclocks)[1];
        }
        int period = FactoryRuntime.batchPeriod(group);
        List<Port> inputs = new ArrayList<>(), outputs = new ArrayList<>();
        for (int i = 0; i < group.size(); i++) {
            FactoryGraph.Node node = group.get(i);
            FactoryRecipeCatalog.Entry entry = FactoryRecipeCatalog.get(node.recipe);
            int parallel = FactoryGraph.effectiveParallel(node.parallel, multiplier);
            int repetitions = Math.multiplyExact(parallel, period / durations[i]);
            long base = node.customEUt < 0 ? entry.recipe.mEUt : node.customEUt;
            FactoryRuntime.State job = FactoryRuntime.prepare(entry.recipe, repetitions, node.overclocks, random, 0);
            job.duration = job.remaining = period;
            long[] timing = FactoryGraph.timing(base, entry.recipe.mDuration, parallel, node.overclocks);
            job.eut = timing[0];
            plan.eut = Math.addExact(plan.eut, job.eut);
            plan.voltage = Math.max(plan.voltage, job.eut / parallel);
            plan.jobs.put(node.id, job);
            if (plan.inputs == null) plan.inputs = entry.consumableRecipe.copy();
            for (ItemStack input : entry.consumableRecipe.mInputs) {
                Port port = new Port();
                port.node = node;
                port.item = input.copy();
                port.item.stackSize = Math.multiplyExact(input.stackSize, repetitions);
                port.match = new GTRecipe.RecipeItemInput(input, entry.recipe.isNBTSensitive);
                inputs.add(port);
            }
            for (FluidStack input : entry.recipe.mFluidInputs) {
                Port port = new Port();
                port.node = node;
                port.fluid = input.copy();
                port.fluid.amount = Math.multiplyExact(input.amount, repetitions);
                inputs.add(port);
            }
            for (ItemStack output : job.pendingItems) {
                Port port = new Port();
                port.node = node;
                port.item = output;
                outputs.add(port);
            }
            for (FluidStack output : job.pendingFluids) {
                Port port = new Port();
                port.node = node;
                port.fluid = output;
                outputs.add(port);
            }
        }
        double[] supply = outputs.stream().mapToDouble(Port::amount).toArray();
        double[] demand = inputs.stream().mapToDouble(Port::amount).toArray();
        FactoryPreview.allocate(supply, demand, (o, i) -> {
            Port out = outputs.get(o), in = inputs.get(i);
            if (!in.node.sources.contains(out.node.id)) return false;
            return out.item != null ? in.match != null && in.match.matchesType(out.item)
                : in.fluid != null && in.fluid.isFluidEqual(out.fluid);
        });
        for (int i = 0; i < inputs.size(); i++) inputs.get(i).amount((int) demand[i]);
        for (int o = 0; o < outputs.size(); o++) outputs.get(o).amount((int) supply[o]);
        // Use strict NBT matching when any member needs it; never weaken another node's external-input check.
        plan.inputs.isNBTSensitive = group.stream()
            .anyMatch(node -> FactoryRecipeCatalog.get(node.recipe).recipe.isNBTSensitive);
        plan.inputs.mInputs = inputs.stream().filter(p -> p.item != null && p.item.stackSize > 0).map(p -> p.item)
            .toArray(ItemStack[]::new);
        plan.inputs.mFluidInputs = inputs.stream().filter(p -> p.fluid != null && p.fluid.amount > 0).map(p -> p.fluid)
            .toArray(FluidStack[]::new);
        for (FactoryRuntime.State job : plan.jobs.values()) {
            job.pendingItems.removeIf(item -> item.stackSize <= 0);
            job.pendingFluids.removeIf(fluid -> fluid.amount <= 0);
        }
        return plan;
    }

    /** Copies a cached net batch; runtime buffers must never mutate the reusable plan. */
    public static Plan scale(Plan base, int multiplier) {
        if (multiplier < 1) throw new ArithmeticException("Invalid batch multiplier");
        Plan plan = new Plan();
        plan.voltage = base.voltage;
        plan.inputs = base.inputs.copy();
        for (ItemStack item : plan.inputs.mInputs) item.stackSize = Math.multiplyExact(item.stackSize, multiplier);
        for (FluidStack fluid : plan.inputs.mFluidInputs) fluid.amount = Math.multiplyExact(fluid.amount, multiplier);
        for (Map.Entry<Integer, FactoryRuntime.State> entry : base.jobs.entrySet()) {
            FactoryRuntime.State original = entry.getValue(), job = new FactoryRuntime.State();
            job.duration = job.remaining = original.duration;
            BigInteger energy = BigInteger.valueOf(original.eut).multiply(BigInteger.valueOf(original.duration))
                .add(BigInteger.valueOf(original.extraEnergyTicks)).multiply(BigInteger.valueOf(multiplier));
            BigInteger[] parts = energy.divideAndRemainder(BigInteger.valueOf(job.duration));
            job.eut = parts[0].longValueExact();
            job.extraEnergyTicks = parts[1].intValueExact();
            for (ItemStack output : original.pendingItems) {
                ItemStack item = output.copy();
                item.stackSize = Math.multiplyExact(item.stackSize, multiplier);
                job.pendingItems.add(item);
            }
            for (FluidStack output : original.pendingFluids) {
                FluidStack fluid = output.copy();
                fluid.amount = Math.multiplyExact(fluid.amount, multiplier);
                job.pendingFluids.add(fluid);
            }
            plan.jobs.put(entry.getKey(), job);
            plan.eut = Math.addExact(plan.eut, job.tickEUt());
        }
        return plan;
    }

    /** Bounds net output amounts, including duplicate outputs and previously completed stock. */
    public static int capacity(Plan plan, FactoryRuntime runtime, int requested) {
        int limit = FactoryBatching.inputLimit(plan.inputs, requested);
        for (Map.Entry<Integer, FactoryRuntime.State> entry : plan.jobs.entrySet()) {
            FactoryRuntime.State job = entry.getValue(), previous = runtime.state(entry.getKey());
            for (ItemStack output : job.pendingItems) {
                long amount = 0, stored = 0;
                for (ItemStack item : job.pendingItems)
                    if (item.isItemEqual(output) && ItemStack.areItemStackTagsEqual(item, output))
                        amount += item.stackSize;
                for (ItemStack item : previous.items)
                    if (item.isItemEqual(output) && ItemStack.areItemStackTagsEqual(item, output))
                        stored += item.stackSize;
                limit = FactoryBatching.amountLimit(limit, amount, stored);
            }
            for (FluidStack output : job.pendingFluids) {
                long amount = 0, stored = 0;
                for (FluidStack fluid : job.pendingFluids) if (fluid.isFluidEqual(output)) amount += fluid.amount;
                for (FluidStack fluid : previous.fluids) if (fluid.isFluidEqual(output)) stored += fluid.amount;
                limit = FactoryBatching.amountLimit(limit, amount, stored);
            }
        }
        return limit;
    }

    /** Exact common period rejects combinations that cannot fit the persisted job duration. */
    static int commonPeriod(int[] durations) {
        long period = 1;
        for (int duration : durations) {
            if (duration <= 0) throw new ArithmeticException("Invalid cycle duration");
            long a = period, b = duration;
            while (b != 0) {
                long next = a % b;
                a = b;
                b = next;
            }
            period = Math.multiplyExact(period / a, duration);
            if (period > Integer.MAX_VALUE) throw new ArithmeticException("Cycle period overflow");
        }
        return (int) period;
    }
}
