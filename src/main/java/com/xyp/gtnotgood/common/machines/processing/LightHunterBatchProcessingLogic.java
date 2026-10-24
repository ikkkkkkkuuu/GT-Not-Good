// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.machines.processing;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.common.recipe.machine.LightHunterRecipeRules;
import com.xyp.gtnotgood.common.wireless.WirelessOutputs;

import gregtech.api.objects.ItemData;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;

public abstract class LightHunterBatchProcessingLogic extends ModularProcessingLogic {

    private final BooleanSupplier unrestricted;
    private final IntSupplier minimumDuration;
    private final IntSupplier additionalThreads;
    private WirelessOutputs outputs;
    private long parallelCount;
    private int recipeCount;
    private int cursor;

    protected LightHunterBatchProcessingLogic(BooleanSupplier unrestricted, IntSupplier minimumDuration) {
        this(unrestricted, minimumDuration, () -> 0);
    }

    protected LightHunterBatchProcessingLogic(BooleanSupplier unrestricted, IntSupplier minimumDuration,
        IntSupplier additionalThreads) {
        this.unrestricted = unrestricted;
        this.minimumDuration = minimumDuration;
        this.additionalThreads = additionalThreads;
    }

    @Override
    @Nonnull
    protected CheckRecipeResult processRecipe() {
        outputs = null;
        parallelCount = 0;
        recipeCount = 0;
        if (availableVoltage <= 0) return CheckRecipeResultRegistry.NO_RECIPE;
        List<InputRecipe> matches = findInputRecipes().stream().filter(r -> r.recipe().mEnabled && r.recipe().mEUt >= 0)
            .filter(r -> r.recipe().mOutputs.length > 0 || r.recipe().mFluidOutputs.length > 0)
            .collect(Collectors.toList());
        List<InputRecipe> recipes = new ArrayList<>();
        CheckRecipeResult failure = CheckRecipeResultRegistry.NO_RECIPE;
        for (InputRecipe candidate : matches) {
            GTRecipe recipe = candidate.recipe();
            CheckRecipeResult result = recipe.mEUt > availableVoltage
                ? CheckRecipeResultRegistry.insufficientPower(recipe.mEUt)
                : validateRecipe(recipe);
            if (result.wasSuccessful()) recipes.add(candidate);
            else failure = result;
        }
        if (recipes.isEmpty()) return failure;
        long quota = LightHunterRecipeRules.parallelQuota(additionalThreads.getAsInt());
        long[] wants = new long[recipes.size()];
        for (int i = 0; i < wants.length; i++) {
            InputRecipe candidate = recipes.get(i);
            wants[i] = maximumParallel(candidate.recipe(), candidate.inputs().items(), candidate.inputs().fluids());
        }
        long[] allocations = unrestricted.getAsBoolean() ? wants : LightHunterRecipeRules.fairAllocation(wants, quota);
        BigInteger energy = BigInteger.ZERO;
        List<WirelessOutputs> batches = new ArrayList<>();
        int start = Math.floorMod(cursor, recipes.size());
        for (int step = 0; step < recipes.size(); step++) {
            int i = (start + step) % recipes.size();
            InputRecipe candidate = recipes.get(i);
            GTRecipe recipe = candidate.recipe();
            ItemStack[] inputItems = candidate.inputs().items();
            FluidStack[] inputFluids = candidate.inputs().fluids();
            long parallel = Math.min(allocations[i], maximumParallel(recipe, inputItems, inputFluids));
            if (parallel <= 0) continue;
            BigInteger recipeEnergy = BigInteger.valueOf(recipe.mEUt).multiply(BigInteger.valueOf(recipe.mDuration))
                .multiply(BigInteger.valueOf(parallel));
            energy = energy.add(recipeEnergy);
            try {
                LightHunterRecipeRules.normalRates(energy, availableVoltage, minimumDuration.getAsInt());
            } catch (ArithmeticException overflow) {
                return CheckRecipeResultRegistry.DURATION_OVERFLOW;
            }
            long remaining = parallel;
            while (remaining > 0) {
                int slice = (int) Math.min(Integer.MAX_VALUE, remaining);
                if (!recipe.isRecipeInputEqual(true, false, slice, inputFluids, inputItems))
                    return CheckRecipeResultRegistry.INTERNAL_ERROR;
                remaining -= slice;
            }
            WirelessOutputs batch = WirelessOutputs.forRecipe(recipe);
            batch.addRecipeBatch(recipe, BigInteger.valueOf(parallel));
            batches.add(batch);
            parallelCount = Math.addExact(parallelCount, parallel);
            recipeCount++;
            cursor = (i + 1) % recipes.size();
            if (energy.compareTo(BigInteger.valueOf(availableVoltage).multiply(BigInteger.valueOf(10000))) > 0) break;
        }
        if (batches.isEmpty()) return CheckRecipeResultRegistry.NO_RECIPE;
        outputs = WirelessOutputs.combine(batches);
        var rates = LightHunterRecipeRules.normalRates(energy, availableVoltage, minimumDuration.getAsInt());
        calculatedEut = rates.eut();
        duration = rates.duration();
        calculatedParallels = (int) Math.min(Integer.MAX_VALUE, parallelCount);
        outputItems = new ItemStack[0];
        outputFluids = new FluidStack[0];
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    public WirelessOutputs getBatchOutputs() {
        return outputs;
    }

    public long getBatchParallelCount() {
        return parallelCount;
    }

    public int getBatchRecipeCount() {
        return recipeCount;
    }

    public static long maximumParallel(GTRecipe recipe, ItemStack[] items, FluidStack[] fluids) {
        if (recipe.maxParallelCalculatedByInputs(1, fluids, items) < 1) return 0;
        long limit = Long.MAX_VALUE;
        ItemData[] associations = new ItemData[items.length];
        for (int i = 0; i < items.length; i++) associations[i] = GTOreDictUnificator.getAssociation(items[i]);
        for (GTRecipe.RecipeItemInput cost : recipe.getCachedCombinedItemInputs()) {
            if (cost.inputAmount <= 0) continue;
            long amount = 0;
            Map<ItemStack, Boolean> seen = new IdentityHashMap<>();
            for (int i = 0; i < items.length; i++) {
                ItemStack item = items[i];
                if (item != null && seen.put(item, true) == null && cost.matchesRecipe(associations[i], item))
                    amount = Math.addExact(amount, Math.max(0, item.stackSize));
            }
            limit = Math.min(limit, amount / cost.inputAmount);
        }
        for (int i = 0; i < recipe.mFluidInputs.length; i++) {
            FluidStack input = recipe.mFluidInputs[i];
            FluidStack[] alternatives = recipe.mAltFluidInputs != null && i < recipe.mAltFluidInputs.length
                && recipe.mAltFluidInputs[i] != null ? recipe.mAltFluidInputs[i] : new FluidStack[] { input };
            long maximum = 0;
            boolean consumed = false;
            for (FluidStack alternative : alternatives) {
                if (alternative == null || alternative.amount <= 0) continue;
                consumed = true;
                long amount = 0;
                Map<FluidStack, Boolean> seen = new IdentityHashMap<>();
                for (FluidStack fluid : fluids)
                    if (fluid != null && seen.put(fluid, true) == null && fluid.isFluidEqual(alternative))
                        amount = Math.addExact(amount, Math.max(0, fluid.amount));
                maximum = Math.max(maximum, amount / alternative.amount);
            }
            if (consumed) limit = Math.min(limit, maximum);
        }
        if (limit == Long.MAX_VALUE) return 0;
        if (recipe.mFluidInputs.length == 0) return limit;
        long low = 0, high = limit;
        while (low < high) {
            long middle = low + (high - low) / 2 + (high - low) % 2;
            if (canSupplyFluids(recipe, fluids, middle)) low = middle;
            else high = middle - 1;
        }
        return low;
    }

    private static boolean canSupplyFluids(GTRecipe recipe, FluidStack[] inputs, long parallel) {
        List<FluidStack> types = new ArrayList<>();
        List<Long> amounts = new ArrayList<>();
        Map<FluidStack, Boolean> seen = new IdentityHashMap<>();
        for (FluidStack input : inputs) {
            if (input == null || seen.put(input, true) != null) continue;
            int index = 0;
            while (index < types.size() && !types.get(index).isFluidEqual(input)) index++;
            if (index == types.size()) {
                types.add(input);
                amounts.add((long) Math.max(0, input.amount));
            } else amounts.set(index, Math.addExact(amounts.get(index), Math.max(0, input.amount)));
        }
        for (int slot = 0; slot < recipe.mFluidInputs.length; slot++) {
            FluidStack[] alternatives = recipe.mAltFluidInputs != null && slot < recipe.mAltFluidInputs.length
                && recipe.mAltFluidInputs[slot] != null ? recipe.mAltFluidInputs[slot]
                    : new FluidStack[] { recipe.mFluidInputs[slot] };
            FluidStack selected = null;
            int selectedIndex = -1;
            long maximum = 0;
            for (FluidStack alternative : alternatives) {
                if (alternative == null || alternative.amount <= 0) continue;
                for (int i = 0; i < types.size(); i++) if (types.get(i).isFluidEqual(alternative)) {
                    long possible = amounts.get(i) / alternative.amount;
                    if (possible > maximum) {
                        maximum = possible;
                        selected = alternative;
                        selectedIndex = i;
                    }
                }
            }
            boolean consumed = false;
            for (FluidStack alternative : alternatives)
                if (alternative != null && alternative.amount > 0) consumed = true;
            if (!consumed) continue;
            if (selected == null || maximum < parallel) return false;
            amounts.set(selectedIndex, amounts.get(selectedIndex) - parallel * selected.amount);
        }
        return true;
    }
}
