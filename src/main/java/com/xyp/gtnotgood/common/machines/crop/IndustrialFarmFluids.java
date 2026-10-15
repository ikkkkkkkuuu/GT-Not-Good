package com.xyp.gtnotgood.common.machines.crop;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.cropsnh.farming.registries.FertilizerRegistry;
import com.gtnewhorizon.cropsnh.farming.registries.HydrationRegistry;
import com.gtnewhorizon.cropsnh.init.CropsNHFluids;

public final class IndustrialFarmFluids {

    private IndustrialFarmFluids() {}

    public static Plan plan(List<FluidStack> fluids, long waterNeeded, long fertilizerNeeded, boolean enrichedOnly) {
        List<FluidStack> sources = new ArrayList<>();
        Map<FluidStack, Integer> available = new IdentityHashMap<>();
        if (fluids != null) {
            for (FluidStack stack : fluids) {
                if (stack != null && stack.amount > 0 && !available.containsKey(stack)) {
                    sources.add(stack);
                    available.put(stack, stack.amount);
                }
            }
        }
        Map<FluidStack, Integer> water = new IdentityHashMap<>();
        Map<FluidStack, Integer> fertilizer = new IdentityHashMap<>();
        boolean hasWater = allocate(sources, available, water, waterNeeded, HydrationRegistry.instance::getPotency);
        boolean hasFertilizer = allocate(sources, available, fertilizer, fertilizerNeeded, stack -> {
            if (!enrichedOnly) return FertilizerRegistry.instance.getPotency(stack);
            return CropsNHFluids.enrichedFertilizer != null && stack.getFluid() == CropsNHFluids.enrichedFertilizer ? 1
                : 0;
        });
        return new Plan(water, fertilizer, hasWater, hasFertilizer);
    }

    private static boolean allocate(List<FluidStack> sources, Map<FluidStack, Integer> available,
        Map<FluidStack, Integer> selected, long needed, ToIntFunction<FluidStack> potencyLookup) {
        long remaining = Math.max(0L, needed);
        for (FluidStack stack : sources) {
            if (remaining == 0) break;
            int potency = potencyLookup.applyAsInt(stack);
            int amount = available.get(stack);
            if (potency <= 0 || amount <= 0) continue;
            long required = remaining / potency + (remaining % potency == 0 ? 0 : 1);
            int consumed = (int) Math.min(amount, required);
            selected.put(stack, consumed);
            available.put(stack, amount - consumed);
            remaining -= Math.min(remaining, (long) consumed * potency);
        }
        return remaining == 0;
    }

    public static final class Plan {

        private final Map<FluidStack, Integer> water;
        private final Map<FluidStack, Integer> fertilizer;
        private final boolean hasWater;
        private final boolean hasFertilizer;
        private boolean committed;

        private Plan(Map<FluidStack, Integer> water, Map<FluidStack, Integer> fertilizer, boolean hasWater,
            boolean hasFertilizer) {
            this.water = water;
            this.fertilizer = fertilizer;
            this.hasWater = hasWater;
            this.hasFertilizer = hasFertilizer;
        }

        public boolean hasWater() {
            return hasWater;
        }

        public boolean hasFertilizer() {
            return hasFertilizer;
        }

        public synchronized boolean commit(boolean consumeFertilizer) {
            if (committed || !hasWater || consumeFertilizer && !hasFertilizer) return false;
            for (Map.Entry<FluidStack, Integer> entry : water.entrySet()) {
                long required = entry.getValue();
                if (consumeFertilizer) required += fertilizer.getOrDefault(entry.getKey(), 0);
                if (entry.getKey().amount < required) return false;
            }
            if (consumeFertilizer) {
                for (Map.Entry<FluidStack, Integer> entry : fertilizer.entrySet()) {
                    if (!water.containsKey(entry.getKey()) && entry.getKey().amount < entry.getValue()) return false;
                }
            }
            water.forEach((stack, amount) -> stack.amount -= amount);
            if (consumeFertilizer) fertilizer.forEach((stack, amount) -> stack.amount -= amount);
            committed = true;
            return true;
        }
    }
}
