package com.xyp.gtnotgood.utils.machine.factory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.util.GTRecipe;

/** Exact net settlement for an atomic page. No intermediate amount is narrowed to a Minecraft stack. */
public final class FactoryWholeBatch {

    private FactoryWholeBatch() {}

    private static final class Port {

        int node;
        ItemStack item;
        FluidStack fluid;
        GTRecipe.RecipeItemInput match;
        BigInteger amount;
    }

    /** Cancels exact internal flows, then rounds each external material up once for a complete page batch. */
    public static FactoryCycles.Plan prepare(List<FactoryGraph.Node> nodes) {
        BigInteger period = BigInteger.ONE;
        for (FactoryGraph.Node node : nodes) {
            FactoryRecipeCatalog.Entry entry = FactoryRecipeCatalog.get(node.recipe);
            if (entry == null) throw new ArithmeticException("Missing page recipe");
            GTRecipe recipe = entry.recipe;
            BigInteger unit = BigInteger.valueOf(FactoryRuntime.outputBatch(recipe));
            BigInteger ticks = BigInteger.valueOf(FactoryGraph.timing(0, recipe.mDuration, 1, node.overclocks)[1]);
            BigInteger required = ticks.multiply(unit.divide(unit.gcd(BigInteger.valueOf(node.parallel))));
            period = period.divide(period.gcd(required))
                .multiply(required);
        }
        List<Port> inputs = new ArrayList<>(), outputs = new ArrayList<>();
        List<BigInteger> energies = new ArrayList<>();
        FactoryCycles.Plan plan = new FactoryCycles.Plan();
        for (FactoryGraph.Node node : nodes) {
            FactoryRecipeCatalog.Entry entry = FactoryRecipeCatalog.get(node.recipe);
            GTRecipe recipe = entry.recipe;
            long[] timing = FactoryGraph
                .timing(node.customEUt < 0 ? recipe.mEUt : node.customEUt, recipe.mDuration, 1, node.overclocks);
            BigInteger repetitions = period.divide(BigInteger.valueOf(timing[1]))
                .multiply(BigInteger.valueOf(node.parallel));
            energies.add(
                period.multiply(BigInteger.valueOf(timing[0]))
                    .multiply(BigInteger.valueOf(node.parallel)));
            plan.voltage = Math.max(plan.voltage, timing[0]);
            if (plan.inputs == null) plan.inputs = entry.consumableRecipe.copy();
            for (ItemStack item : entry.consumableRecipe.mInputs) {
                if (item == null || item.stackSize <= 0) continue;
                Port port = item(node.id, item, repetitions.multiply(BigInteger.valueOf(item.stackSize)));
                port.match = new GTRecipe.RecipeItemInput(item, recipe.isNBTSensitive);
                inputs.add(port);
            }
            for (FluidStack fluid : recipe.mFluidInputs) if (fluid != null && fluid.amount > 0)
                inputs.add(fluid(node.id, fluid, repetitions.multiply(BigInteger.valueOf(fluid.amount))));
            for (int i = 0; i < recipe.mOutputs.length; i++) {
                ItemStack item = recipe.mOutputs[i];
                if (item == null || item.stackSize <= 0) continue;
                BigInteger amount = repetitions.multiply(BigInteger.valueOf(item.stackSize))
                    .multiply(BigInteger.valueOf(Math.max(0, Math.min(10000, recipe.getOutputChance(i)))))
                    .divide(BigInteger.valueOf(10000));
                outputs.add(item(node.id, item, amount));
            }
            for (FluidStack fluid : recipe.mFluidOutputs) if (fluid != null && fluid.amount > 0)
                outputs.add(fluid(node.id, fluid, repetitions.multiply(BigInteger.valueOf(fluid.amount))));
        }
        cancel(nodes, inputs, outputs);
        combine(inputs);
        combine(outputs);
        // The former per-tick boundary now defines one batch. Duration no longer shrinks with that normalization.
        BigInteger divisor = period;
        int duration = FactoryBatchDuration.ticks(
            nodes,
            node -> (int) FactoryGraph
                .timing(0, FactoryRecipeCatalog.get(node.recipe).recipe.mDuration, 1, node.overclocks)[1]);
        for (int i = 0; i < nodes.size(); i++) {
            FactoryRuntime.State job = new FactoryRuntime.State();
            job.duration = job.remaining = duration;
            BigInteger[] energy = energies.get(i)
                .divide(divisor)
                .divideAndRemainder(BigInteger.valueOf(duration));
            job.eut = energy[0].longValueExact();
            job.extraEnergyTicks = energy[1].intValueExact();
            plan.eut = Math.addExact(plan.eut, job.tickEUt());
            plan.jobs.put(nodes.get(i).id, job);
        }
        List<ItemStack> items = new ArrayList<>();
        List<FluidStack> fluids = new ArrayList<>();
        for (Port port : inputs) {
            if (port.amount.signum() == 0) continue;
            int count = ceiling(port.amount, divisor);
            if (port.item != null) {
                ItemStack item = port.item.copy();
                item.stackSize = count;
                items.add(item);
            } else {
                FluidStack fluid = port.fluid.copy();
                fluid.amount = count;
                fluids.add(fluid);
            }
        }
        plan.inputs.mInputs = items.toArray(new ItemStack[0]);
        plan.inputs.mFluidInputs = fluids.toArray(new FluidStack[0]);
        plan.inputs.isNBTSensitive = nodes.stream()
            .anyMatch(n -> FactoryRecipeCatalog.get(n.recipe).recipe.isNBTSensitive);
        for (Port port : outputs) {
            if (port.amount.signum() == 0) continue;
            FactoryRuntime.State job = plan.jobs.get(port.node);
            int count = ceiling(port.amount, divisor);
            if (port.item != null) {
                ItemStack item = port.item.copy();
                item.stackSize = count;
                job.pendingItems.add(item);
            } else {
                FluidStack fluid = port.fluid.copy();
                fluid.amount = count;
                job.pendingFluids.add(fluid);
            }
        }
        return plan;
    }

    private static int ceiling(BigInteger amount, BigInteger divisor) {
        return amount.add(divisor)
            .subtract(BigInteger.ONE)
            .divide(divisor)
            .intValueExact();
    }

    private static Port item(int node, ItemStack item, BigInteger amount) {
        Port port = new Port();
        port.node = node;
        port.item = item;
        port.amount = amount;
        return port;
    }

    private static void combine(List<Port> ports) {
        for (int i = 0; i < ports.size(); i++) {
            Port first = ports.get(i);
            for (int j = ports.size() - 1; j > i; j--) {
                Port other = ports.get(j);
                boolean same = first.item != null
                    ? other.item != null && first.item.isItemEqual(other.item)
                        && ItemStack.areItemStackTagsEqual(first.item, other.item)
                    : other.fluid != null && first.fluid.isFluidEqual(other.fluid);
                if (same) {
                    first.amount = first.amount.add(other.amount);
                    ports.remove(j);
                }
            }
        }
    }

    private static Port fluid(int node, FluidStack fluid, BigInteger amount) {
        Port port = new Port();
        port.node = node;
        port.fluid = fluid;
        port.amount = amount;
        return port;
    }

    private static final class Arc {

        final int to, reverse;
        BigInteger remaining;

        Arc(int to, int reverse, BigInteger remaining) {
            this.to = to;
            this.reverse = reverse;
            this.remaining = remaining;
        }
    }

    /** Integral residual flow also handles overlapping ore-dictionary matches without greedy allocation loss. */
    private static void cancel(List<FactoryGraph.Node> nodes, List<Port> inputs, List<Port> outputs) {
        int sink = outputs.size() + inputs.size() + 1;
        List<List<Arc>> graph = new ArrayList<>();
        for (int i = 0; i <= sink; i++) graph.add(new ArrayList<>());
        Arc[] supply = new Arc[outputs.size()], demand = new Arc[inputs.size()];
        for (int o = 0; o < outputs.size(); o++) supply[o] = arc(graph, 0, 1 + o, outputs.get(o).amount);
        for (int i = 0; i < inputs.size(); i++)
            demand[i] = arc(graph, 1 + outputs.size() + i, sink, inputs.get(i).amount);
        for (int o = 0; o < outputs.size(); o++) for (int i = 0; i < inputs.size(); i++) {
            Port out = outputs.get(o), in = inputs.get(i);
            boolean connected = nodes.stream()
                .anyMatch(n -> n.id == in.node && n.sources.contains(out.node));
            boolean matches = out.item != null ? in.match != null && in.match.matchesType(out.item)
                : in.fluid != null && in.fluid.isFluidEqual(out.fluid);
            if (connected && matches) arc(graph, 1 + o, 1 + outputs.size() + i, out.amount.min(in.amount));
        }
        while (true) {
            int[] parent = new int[sink + 1], edge = new int[sink + 1], queue = new int[sink + 1];
            Arrays.fill(parent, -1);
            parent[0] = 0;
            int head = 0, tail = 1;
            while (head < tail && parent[sink] < 0) {
                int from = queue[head++];
                for (int e = 0; e < graph.get(from)
                    .size(); e++) {
                    Arc arc = graph.get(from)
                        .get(e);
                    if (arc.remaining.signum() <= 0 || parent[arc.to] >= 0) continue;
                    parent[arc.to] = from;
                    edge[arc.to] = e;
                    queue[tail++] = arc.to;
                }
            }
            if (parent[sink] < 0) break;
            BigInteger amount = null;
            for (int at = sink; at != 0; at = parent[at]) {
                BigInteger remaining = graph.get(parent[at])
                    .get(edge[at]).remaining;
                amount = amount == null ? remaining : amount.min(remaining);
            }
            for (int at = sink; at != 0; at = parent[at]) {
                Arc arc = graph.get(parent[at])
                    .get(edge[at]);
                arc.remaining = arc.remaining.subtract(amount);
                Arc reverse = graph.get(at)
                    .get(arc.reverse);
                reverse.remaining = reverse.remaining.add(amount);
            }
        }
        for (int o = 0; o < outputs.size(); o++) outputs.get(o).amount = supply[o].remaining;
        for (int i = 0; i < inputs.size(); i++) inputs.get(i).amount = demand[i].remaining;
    }

    private static Arc arc(List<List<Arc>> graph, int from, int to, BigInteger amount) {
        Arc forward = new Arc(
            to,
            graph.get(to)
                .size(),
            amount);
        Arc reverse = new Arc(
            from,
            graph.get(from)
                .size(),
            BigInteger.ZERO);
        graph.get(from)
            .add(forward);
        graph.get(to)
            .add(reverse);
        return forward;
    }
}
