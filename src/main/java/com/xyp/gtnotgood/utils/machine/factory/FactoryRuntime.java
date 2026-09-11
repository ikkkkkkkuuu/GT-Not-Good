package com.xyp.gtnotgood.utils.machine.factory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.util.GTRecipe;

/**
 * Per-node material buffers and prepaid-input jobs. Jobs retain rolled outputs across saves and power interruptions.
 * Finished buffers remain visible while the next job runs; the controller bounds them using downstream watermarks.
 */
public final class FactoryRuntime {

    public final Map<Integer, State> states = new LinkedHashMap<>();

    /** The outputs remain unavailable to consumers until every paid processing tick has completed. */
    public static final class State {

        public final List<ItemStack> items = new ArrayList<>();
        public final List<FluidStack> fluids = new ArrayList<>();
        public final List<ItemStack> pendingItems = new ArrayList<>();
        public final List<FluidStack> pendingFluids = new ArrayList<>();
        public int remaining;
        public int duration;
        public long eut;

        public boolean empty() {
            compact();
            return remaining == 0 && items.isEmpty()
                && fluids.isEmpty()
                && pendingItems.isEmpty()
                && pendingFluids.isEmpty();
        }

        public void compact() {
            items.removeIf(stack -> stack == null || stack.stackSize <= 0);
            fluids.removeIf(stack -> stack == null || stack.amount <= 0);
        }
    }

    public State state(int id) {
        return states.computeIfAbsent(id, ignored -> new State());
    }

    public boolean empty() {
        for (State state : states.values()) if (!state.empty()) return false;
        return true;
    }

    public long totalEUt() {
        long total = 0;
        for (State state : states.values()) if (state.remaining > 0) total = Math.addExact(total, state.eut);
        return total;
    }

    /** Advance only after the machine successfully pays this tick's aggregate energy cost. */
    public void advance() {
        for (State state : states.values()) {
            if (state.remaining <= 0 || --state.remaining > 0) continue;
            for (ItemStack output : state.pendingItems) mergeItem(state.items, output);
            for (FluidStack output : state.pendingFluids) mergeFluid(state.fluids, output);
            state.pendingItems.clear();
            state.pendingFluids.clear();
        }
    }

    private static void mergeItem(List<ItemStack> items, ItemStack output) {
        for (ItemStack stored : items) {
            if (stored.isItemEqual(output) && ItemStack.areItemStackTagsEqual(stored, output)) {
                stored.stackSize = Math.addExact(stored.stackSize, output.stackSize);
                return;
            }
        }
        items.add(output.copy());
    }

    private static void mergeFluid(List<FluidStack> fluids, FluidStack output) {
        for (FluidStack stored : fluids) {
            if (stored.isFluidEqual(output)) {
                stored.amount = Math.addExact(stored.amount, output.amount);
                return;
            }
        }
        fluids.add(output.copy());
    }

    /** Reserve output capacity before consuming inputs, retaining already completed products for downstream jobs. */
    public void start(int id, State job) {
        State previous = state(id);
        job.items.addAll(previous.items);
        job.fluids.addAll(previous.fluids);
        states.put(id, job);
    }

    /** Validates the reserved batch against existing buffers before physical inputs are consumed. */
    public void checkCapacity(int id, State job) {
        State previous = state(id);
        for (ItemStack output : job.pendingItems) {
            long total = output.stackSize;
            for (ItemStack item : previous.items) {
                if (item.isItemEqual(output) && ItemStack.areItemStackTagsEqual(item, output)) total += item.stackSize;
            }
            if (total > Integer.MAX_VALUE) throw new ArithmeticException("Item buffer overflow");
        }
        for (FluidStack output : job.pendingFluids) {
            long total = output.amount;
            for (FluidStack fluid : previous.fluids) if (fluid.isFluidEqual(output)) total += fluid.amount;
            if (total > Integer.MAX_VALUE) throw new ArithmeticException("Fluid buffer overflow");
        }
    }

    /** Uses exact small-batch rolls and GT's large-batch chance calculation without a parallel-sized loop. */
    public static State prepare(GTRecipe recipe, int parallel, int overclocks, Random random) {
        return prepare(recipe, parallel, overclocks, random, recipe.mEUt);
    }

    /** Uses the submitted node cost when reserving a batch; existing batches retain their original EU/t. */
    public static State prepare(GTRecipe recipe, int parallel, int overclocks, Random random, long baseEUt) {
        long[] timing = FactoryGraph.timing(baseEUt, recipe.mDuration, parallel, overclocks);
        State state = new State();
        state.eut = timing[0];
        state.duration = state.remaining = (int) timing[1];
        for (int i = 0; i < recipe.mOutputs.length; i++) {
            ItemStack template = recipe.mOutputs[i];
            if (template == null || template.stackSize <= 0) continue;
            int chance = recipe.getOutputChance(i);
            long successes;
            if (chance >= 10000) successes = parallel;
            else if (parallel > 65536)
                successes = gregtech.api.util.ParallelHelper.calculateIntegralChancedOutputMultiplier(chance, parallel);
            else {
                successes = 0;
                for (int p = 0; p < parallel; p++) if (random.nextInt(10000) < chance) successes++;
            }
            int amount = Math.toIntExact(Math.multiplyExact(successes, template.stackSize));
            if (amount > 0) {
                ItemStack stack = template.copy();
                stack.stackSize = amount;
                mergeItem(state.pendingItems, stack);
            }
        }
        for (FluidStack template : recipe.mFluidOutputs) {
            if (template == null || template.amount <= 0) continue;
            FluidStack stack = template.copy();
            stack.amount = Math.multiplyExact(template.amount, parallel);
            mergeFluid(state.pendingFluids, stack);
        }
        return state;
    }

    public NBTTagCompound write() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (Map.Entry<Integer, State> entry : states.entrySet()) {
            State state = entry.getValue();
            NBTTagCompound data = new NBTTagCompound();
            data.setInteger("id", entry.getKey());
            data.setInteger("remaining", state.remaining);
            data.setInteger("duration", state.duration);
            data.setLong("eut", state.eut);
            data.setTag("items", FactoryRecipeCatalog.items(state.items.toArray(new ItemStack[0])));
            data.setTag("fluids", FactoryRecipeCatalog.fluids(state.fluids.toArray(new FluidStack[0])));
            data.setTag("pendingItems", FactoryRecipeCatalog.items(state.pendingItems.toArray(new ItemStack[0])));
            data.setTag("pendingFluids", FactoryRecipeCatalog.fluids(state.pendingFluids.toArray(new FluidStack[0])));
            list.appendTag(data);
        }
        tag.setTag("states", list);
        return tag;
    }

    public void read(NBTTagCompound tag) {
        states.clear();
        NBTTagList list = tag.getTagList("states", 10);
        for (int i = 0; i < Math.min(FactoryGraph.MAX_NODES, list.tagCount()); i++) {
            NBTTagCompound data = list.getCompoundTagAt(i);
            State state = state(data.getInteger("id"));
            state.duration = Math.max(1, data.getInteger("duration"));
            state.remaining = Math.max(0, Math.min(state.duration, data.getInteger("remaining")));
            state.eut = Math.max(1, data.getLong("eut"));
            NBTTagList items = data.getTagList("items", 10);
            for (int j = 0; j < Math.min(16, items.tagCount()); j++) {
                NBTTagCompound value = items.getCompoundTagAt(j);
                ItemStack stack = ItemStack.loadItemStackFromNBT(value);
                if (stack != null) {
                    stack.stackSize = Math.max(0, value.getInteger("amount"));
                    state.items.add(stack);
                }
            }
            NBTTagList fluids = data.getTagList("fluids", 10);
            for (int j = 0; j < Math.min(16, fluids.tagCount()); j++) {
                FluidStack stack = FluidStack.loadFluidStackFromNBT(fluids.getCompoundTagAt(j));
                if (stack != null) state.fluids.add(stack);
            }
            state.compact();
            NBTTagList pendingItems = data.getTagList("pendingItems", 10);
            for (int j = 0; j < Math.min(16, pendingItems.tagCount()); j++) {
                NBTTagCompound value = pendingItems.getCompoundTagAt(j);
                ItemStack stack = ItemStack.loadItemStackFromNBT(value);
                if (stack != null) {
                    stack.stackSize = Math.max(0, value.getInteger("amount"));
                    state.pendingItems.add(stack);
                }
            }
            NBTTagList pendingFluids = data.getTagList("pendingFluids", 10);
            for (int j = 0; j < Math.min(16, pendingFluids.tagCount()); j++) {
                FluidStack stack = FluidStack.loadFluidStackFromNBT(pendingFluids.getCompoundTagAt(j));
                if (stack != null) state.pendingFluids.add(stack);
            }
        }
    }
}
