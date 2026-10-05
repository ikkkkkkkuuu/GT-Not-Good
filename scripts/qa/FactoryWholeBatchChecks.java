package com.xyp.gtnotgood.common.gui.modularui.widget;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.common.machines.multiblock.IntegratedProductionFactory;
import com.xyp.gtnotgood.utils.machine.factory.FactoryBatchDuration;
import com.xyp.gtnotgood.utils.machine.factory.FactoryCycles;
import com.xyp.gtnotgood.utils.machine.factory.FactoryGraph;
import com.xyp.gtnotgood.utils.machine.factory.FactoryPatternExport;
import com.xyp.gtnotgood.utils.machine.factory.FactoryRecipeCatalog;
import com.xyp.gtnotgood.utils.machine.factory.FactoryRuntime;
import com.xyp.gtnotgood.utils.machine.factory.FactoryWholeBatch;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.storage.data.IAEStack;
import gregtech.api.util.GTRecipe;

/** Checks the actual scheduler against independently summed recipe material and energy balances. */
final class FactoryWholeBatchChecks {

    private FactoryWholeBatchChecks() {}

    static void check(FactoryGraph graph, StringBuilder report) throws Exception {
        FactoryCycles.Plan base = FactoryWholeBatch.prepare(graph.nodes);
        Map<String, BigInteger> expected = new LinkedHashMap<>(), actual = boundary(base);
        BigInteger period = BigInteger.valueOf(FactoryRuntime.batchPeriod(graph.nodes));
        BigInteger rawEnergy = BigInteger.ZERO, batchEnergy = energy(base);
        for (FactoryGraph.Node node : graph.nodes) {
            GTRecipe recipe = FactoryRecipeCatalog.get(node.recipe).recipe;
            long[] timing = FactoryGraph
                .timing(node.customEUt < 0 ? recipe.mEUt : node.customEUt, recipe.mDuration, 1, node.overclocks);
            BigInteger repeats = period.divide(BigInteger.valueOf(timing[1]))
                .multiply(BigInteger.valueOf(node.parallel));
            rawEnergy = rawEnergy.add(
                period.multiply(BigInteger.valueOf(timing[0]))
                    .multiply(BigInteger.valueOf(node.parallel)));
            for (ItemStack item : recipe.mInputs) if (item != null && item.stackSize > 0)
                add(expected, key(item), repeats.multiply(BigInteger.valueOf(-item.stackSize)));
            for (FluidStack fluid : recipe.mFluidInputs)
                add(expected, key(fluid), repeats.multiply(BigInteger.valueOf(-fluid.amount)));
            for (int i = 0; i < recipe.mOutputs.length; i++) {
                ItemStack item = recipe.mOutputs[i];
                if (item != null) add(
                    expected,
                    key(item),
                    repeats.multiply(BigInteger.valueOf(item.stackSize))
                        .multiply(BigInteger.valueOf(recipe.getOutputChance(i)))
                        .divide(BigInteger.valueOf(10000)));
            }
            for (FluidStack fluid : recipe.mFluidOutputs)
                add(expected, key(fluid), repeats.multiply(BigInteger.valueOf(fluid.amount)));
        }
        expected.entrySet()
            .removeIf(
                e -> e.getValue()
                    .signum() == 0);
        actual.entrySet()
            .removeIf(
                e -> e.getValue()
                    .signum() == 0);
        require(
            expected.keySet()
                .equals(actual.keySet()),
            "net materials unchanged");
        for (String key : expected.keySet()) {
            BigInteger raw = expected.get(key);
            BigInteger rounded = raw.abs()
                .add(period)
                .subtract(BigInteger.ONE)
                .divide(period);
            if (raw.signum() < 0) rounded = rounded.negate();
            require(rounded.equals(actual.get(key)), "net batch rounded upward: " + key);
        }
        require(
            rawEnergy.divide(period)
                .equals(batchEnergy),
            "batch energy is unchanged by duration");
        report.append("PASS boundary rounding after cancellation and exact total batch energy\n");
        checkOverclocks(graph, base, report);
        FactoryGraph huge = graph.copy();
        for (FactoryGraph.Node node : huge.nodes) node.parallel = Math.multiplyExact(node.parallel, 2);
        FactoryCycles.Plan hugePlan = FactoryWholeBatch.prepare(huge.nodes);
        require(energy(hugePlan).equals(batchEnergy.multiply(BigInteger.valueOf(2))), "large parallel energy");
        require(
            hugePlan.jobs.values()
                .iterator()
                .next().duration
                == base.jobs.values()
                    .iterator()
                    .next().duration,
            "parallel counts never shorten processing time");
        report.append("PASS large intermediate recipe counts and unchanged parallel processing duration\n");
        FactoryCycles.Plan scaled = FactoryCycles.scale(base, 3);
        require(energy(scaled).equals(batchEnergy.multiply(BigInteger.valueOf(3))), "batch scaling energy");
        require(boundary(base).equals(actual), "cached base not mutated");
        int limit = FactoryCycles.capacity(base, new FactoryRuntime(), Integer.MAX_VALUE);
        require(limit > 0, "safe capacity exists");
        FactoryCycles.Plan maximum = FactoryCycles.scale(base, limit);
        FactoryRuntime empty = new FactoryRuntime();
        for (Map.Entry<Integer, FactoryRuntime.State> entry : maximum.jobs.entrySet())
            empty.checkCapacity(entry.getKey(), entry.getValue());
        report.append("Safe batches per execution: ")
            .append(limit)
            .append('\n');
        int duration = base.jobs.values()
            .iterator()
            .next().duration;
        report.append("Atomic batch: duration=")
            .append(duration)
            .append(" ticks; energy=")
            .append(batchEnergy)
            .append(" EU\n");
        for (FluidStack fluid : base.inputs.mFluidInputs) report.append("Batch input ")
            .append(
                fluid.getFluid()
                    .getName())
            .append('=')
            .append(fluid.amount)
            .append('\n');
        // Supply exactly three net batches through the controller's ordinary-input scheduling path.
        TestFactory machine = new TestFactory();
        Field installed = field("installed");
        installed.set(machine, graph);
        Method start = IntegratedProductionFactory.class
            .getDeclaredMethod("tryStartCycle", List.class, List.class, List.class);
        start.setAccessible(true);
        List<ItemStack> items = new ArrayList<>(Arrays.asList(scaled.inputs.mInputs));
        List<FluidStack> fluids = new ArrayList<>(Arrays.asList(scaled.inputs.mFluidInputs));
        machine.power = 0;
        start.invoke(machine, graph.nodes, items, fluids);
        FactoryRuntime unpaid = (FactoryRuntime) field("runtime").get(machine);
        require(
            unpaid.states.values()
                .stream()
                .noneMatch(j -> j.remaining > 0),
            "no job without power");
        require(
            items.stream()
                .allMatch(i -> i.stackSize > 0)
                && fluids.stream()
                    .allMatch(f -> f.amount > 0),
            "power failure never debits inputs");
        machine.power = Long.MAX_VALUE;
        start.invoke(machine, graph.nodes, new ArrayList<ItemStack>(), new ArrayList<FluidStack>());
        require(
            unpaid.states.values()
                .stream()
                .noneMatch(j -> j.remaining > 0),
            "no job without net inputs");
        start.invoke(machine, graph.nodes, items, fluids);
        FactoryRuntime runtime = (FactoryRuntime) field("runtime").get(machine);
        require(
            runtime.states.values()
                .stream()
                .anyMatch(j -> j.remaining > 0),
            "real scheduler starts");
        require(
            items.stream()
                .allMatch(i -> i.stackSize == 0)
                && fluids.stream()
                    .allMatch(f -> f.amount == 0),
            "net inputs consumed once");
        BigInteger paid = BigInteger.ZERO;
        for (int tick = 0; tick < duration; tick++) {
            paid = paid.add(BigInteger.valueOf(runtime.totalEUt()));
            runtime.advance();
            if (tick + 1 < duration) require(
                runtime.states.values()
                    .stream()
                    .allMatch(j -> j.items.isEmpty() && j.fluids.isEmpty()),
                "no output before final paid tick");
            FactoryRuntime reloaded = new FactoryRuntime();
            reloaded.read(runtime.write());
            runtime = reloaded;
        }
        require(paid.equals(energy(scaled)), "exact energy after repeated save/reload");
        Map<String, BigInteger> delivered = new LinkedHashMap<>(), promised = new LinkedHashMap<>();
        for (FactoryRuntime.State job : scaled.jobs.values()) {
            for (ItemStack item : job.pendingItems) add(promised, key(item), BigInteger.valueOf(item.stackSize));
            for (FluidStack fluid : job.pendingFluids) add(promised, key(fluid), BigInteger.valueOf(fluid.amount));
        }
        for (FactoryRuntime.State job : runtime.states.values()) {
            require(job.remaining == 0, "job finishes");
            for (ItemStack item : job.items) add(delivered, key(item), BigInteger.valueOf(item.stackSize));
            for (FluidStack fluid : job.fluids) add(delivered, key(fluid), BigInteger.valueOf(fluid.amount));
        }
        require(delivered.equals(promised), "all promised outputs delivered");
        report.append("PASS real scheduler start, input debit, output settlement, save/reload and energy payment\n");
        ItemStack pattern = FactoryPatternExport.create(graph);
        var details = ((ICraftingPatternItem) pattern.getItem())
            .getPatternForItem(pattern, Minecraft.getMinecraft().theWorld);
        require(details != null, "native pattern decodes");
        long patternInput = 0, planInput = 0;
        for (IAEStack<?> stack : details.getAEInputs()) patternInput += stack.getStackSize();
        for (ItemStack item : base.inputs.mInputs) planInput += item.stackSize;
        for (FluidStack fluid : base.inputs.mFluidInputs) planInput += fluid.amount;
        require(patternInput == planInput, "pattern uses reduced batch");
        report.append("PASS AE native pattern export\n");
        FactoryRuntime blocked = new FactoryRuntime();
        for (Map.Entry<Integer, FactoryRuntime.State> entry : base.jobs.entrySet()) {
            if (!entry.getValue().pendingFluids.isEmpty()) {
                FluidStack fluid = entry.getValue().pendingFluids.get(0)
                    .copy();
                fluid.amount = Integer.MAX_VALUE;
                blocked.state(entry.getKey()).fluids.add(fluid);
                break;
            }
        }
        require(FactoryCycles.capacity(base, blocked, 1) == 0, "full output blocks before consumption");
        FactoryRuntime remainder = new FactoryRuntime();
        FactoryRuntime.State fractionalEnergy = remainder.state(0);
        fractionalEnergy.duration = fractionalEnergy.remaining = 7;
        fractionalEnergy.eut = 3;
        fractionalEnergy.extraEnergyTicks = 5;
        long remainderPaid = 0;
        for (int i = 0; i < 7; i++) {
            FactoryRuntime restored = new FactoryRuntime();
            restored.read(remainder.write());
            remainder = restored;
            remainderPaid += remainder.totalEUt();
            remainder.advance();
        }
        require(remainderPaid == 26, "energy remainder survives every tick reload");
        report.append("PASS missing supplies, power shortage, full output and fractional energy persistence\n");
        checkAtomic(graph, report);
    }

    private static void checkAtomic(FactoryGraph graph, StringBuilder report) throws Exception {
        FactoryCycles.Plan base = FactoryWholeBatch.prepare(graph.nodes);
        int duration = FactoryBatchDuration.ticks(
            graph.nodes,
            n -> (int) FactoryGraph.timing(0, FactoryRecipeCatalog.get(n.recipe).recipe.mDuration, 1, n.overclocks)[1]);
        require(
            base.jobs.values()
                .stream()
                .allMatch(j -> j.duration == duration),
            "whole page barrier");
        long water = 0;
        for (FluidStack fluid : base.inputs.mFluidInputs) if (fluid.getFluid()
            .getName()
            .equals("aqua regia")) water += fluid.amount;
        if (FactoryRecipeCatalog.get(graph.nodes.get(0).recipe).recipe.mDuration > 1)
            require(water == 49500000, "normal batch uses 49,500,000 L aqua regia");
        TestFactory machine = new TestFactory();
        field("installed").set(machine, graph);
        FactoryRuntime runtime = (FactoryRuntime) field("runtime").get(machine);
        Method start = IntegratedProductionFactory.class
            .getDeclaredMethod("tryStartCycle", List.class, List.class, List.class);
        start.setAccessible(true);
        FactoryCycles.Plan supply = FactoryCycles.scale(base, 1);
        List<ItemStack> items = new ArrayList<>(Arrays.asList(supply.inputs.mInputs));
        List<FluidStack> fluids = new ArrayList<>(Arrays.asList(supply.inputs.mFluidInputs));
        start.invoke(machine, graph.nodes, items, fluids);
        require(
            items.stream()
                .allMatch(i -> i.stackSize == 0)
                && fluids.stream()
                    .allMatch(f -> f.amount == 0),
            "all input debited at start");
        require(
            runtime.states.values()
                .stream()
                .allMatch(j -> j.remaining == duration),
            "full duration at start");
        String beforePause = runtime.write()
            .toString();
        runtime.read(runtime.write());
        require(
            beforePause.equals(
                runtime.write()
                    .toString()),
            "unpaid pause/reload does not progress or emit");
        FactoryCycles.Plan second = FactoryCycles.scale(base, 1);
        start.invoke(
            machine,
            graph.nodes,
            new ArrayList<>(Arrays.asList(second.inputs.mInputs)),
            new ArrayList<>(Arrays.asList(second.inputs.mFluidInputs)));
        require(
            second.inputs.mFluidInputs[0].amount == base.inputs.mFluidInputs[0].amount,
            "running batch does not consume the next supply");
        for (int tick = 0; tick < duration - 1; tick++) runtime.advance();
        require(
            runtime.states.values()
                .stream()
                .allMatch(j -> j.items.isEmpty() && j.fluids.isEmpty() && j.remaining == 1),
            "all output held until final tick");
        runtime.read(runtime.write());
        runtime.advance();
        require(
            runtime.states.values()
                .stream()
                .allMatch(j -> j.remaining == 0),
            "all nodes finish together");
        report.append("PASS atomic batch: aqua regia=")
            .append(water)
            .append(" L, duration=")
            .append(duration)
            .append(" ticks; one debit, no early output, reload/pause, final simultaneous settlement\n");
    }

    private static void checkOverclocks(FactoryGraph graph, FactoryCycles.Plan base, StringBuilder report) {
        ItemStack originalPattern = FactoryPatternExport.create(graph);
        for (int index = 0; index < graph.nodes.size(); index++) {
            for (int oc : new int[] { 1, 3, 14 }) {
                FactoryGraph changed = graph.copy();
                FactoryGraph.Node node = changed.nodes.get(index);
                node.overclocks = oc;
                FactoryCycles.Plan plan = FactoryWholeBatch.prepare(changed.nodes);
                require(
                    boundary(plan).equals(boundary(base)),
                    "OC must preserve every net material: " + index + "/" + oc);
                require(
                    ItemStack.areItemStackTagsEqual(originalPattern, FactoryPatternExport.create(changed)),
                    "OC must preserve AE input/output quantities: " + index + "/" + oc);
                GTRecipe recipe = FactoryRecipeCatalog.get(node.recipe).recipe;
                long cost = node.customEUt < 0 ? recipe.mEUt : node.customEUt;
                long oldPower = FactoryGraph.timing(cost, recipe.mDuration, 1, graph.nodes.get(index).overclocks)[0];
                long newPower = FactoryGraph.timing(cost, recipe.mDuration, 1, oc)[0];
                BigInteger expected = energy(base).add(
                    BigInteger.valueOf(newPower - oldPower)
                        .multiply(BigInteger.valueOf(node.parallel)));
                require(energy(plan).equals(expected), "only changed node contributes OC energy change");
                require(
                    plan.jobs.values()
                        .iterator()
                        .next().duration
                        <= base.jobs.values()
                            .iterator()
                            .next().duration,
                    "OC never lengthens the processing path");
                if (index == 0 && oc == 1) report.append("First node OC 0 -> 1: duration ")
                    .append(
                        base.jobs.values()
                            .iterator()
                            .next().duration)
                    .append(" -> ")
                    .append(
                        plan.jobs.values()
                            .iterator()
                            .next().duration)
                    .append(" ticks; batch materials unchanged\n");
            }
        }
        FactoryGraph all = graph.copy();
        for (FactoryGraph.Node node : all.nodes) node.overclocks = 1;
        require(
            ItemStack.areItemStackTagsEqual(originalPattern, FactoryPatternExport.create(all)),
            "whole-line OC preserves batch quantities");
        report.append(
            "PASS all 38 nodes individually at OC 1/3/14 and whole-line OC: materials and AE patterns unchanged\n");
    }

    private static Field field(String name) throws Exception {
        Field field = IntegratedProductionFactory.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static final class TestFactory extends IntegratedProductionFactory {

        long power = Long.MAX_VALUE;

        TestFactory() {
            super("factory.qa.whole");
        }

        @Override
        public long getMaxInputVoltage() {
            return Long.MAX_VALUE;
        }

        @Override
        public long getMaxInputEu() {
            return power;
        }
    }

    private static Map<String, BigInteger> boundary(FactoryCycles.Plan plan) {
        Map<String, BigInteger> values = new LinkedHashMap<>();
        for (ItemStack item : plan.inputs.mInputs) add(values, key(item), BigInteger.valueOf(-item.stackSize));
        for (FluidStack fluid : plan.inputs.mFluidInputs) add(values, key(fluid), BigInteger.valueOf(-fluid.amount));
        for (FactoryRuntime.State job : plan.jobs.values()) {
            for (ItemStack item : job.pendingItems) add(values, key(item), BigInteger.valueOf(item.stackSize));
            for (FluidStack fluid : job.pendingFluids) add(values, key(fluid), BigInteger.valueOf(fluid.amount));
        }
        values.entrySet()
            .removeIf(
                e -> e.getValue()
                    .signum() == 0);
        return values;
    }

    private static BigInteger energy(FactoryCycles.Plan plan) {
        BigInteger total = BigInteger.ZERO;
        for (FactoryRuntime.State job : plan.jobs.values()) total = total.add(
            BigInteger.valueOf(job.eut)
                .multiply(BigInteger.valueOf(job.duration))
                .add(BigInteger.valueOf(job.extraEnergyTicks)));
        return total;
    }

    private static String key(ItemStack item) {
        return item.getUnlocalizedName() + ":" + item.getItemDamage() + ":" + item.getTagCompound();
    }

    private static String key(FluidStack fluid) {
        return fluid.getFluid()
            .getName() + ":"
            + fluid.tag;
    }

    private static void add(Map<String, BigInteger> values, String key, BigInteger amount) {
        values.merge(key, amount, BigInteger::add);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
