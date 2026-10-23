// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.machines.processing;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.common.recipe.machine.LightHunterRecipeRules;
import com.xyp.gtnotgood.common.wireless.WirelessOutputs;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;

public final class ParallelBatchProcessingLogic extends ModularProcessingLogic {

    @Override
    protected Stream<GTRecipe> findRecipeMatches(RecipeMap<?> map) {
        if (map != RecipeMaps.furnaceRecipes) return super.findRecipeMatches(map);
        List<ItemStack> distinct = new ArrayList<>();
        for (ItemStack stack : inputItems) {
            if (stack == null || stack.stackSize <= 0) continue;
            if (distinct.stream().noneMatch(other -> GTUtility.areStacksEqual(other, stack))) distinct.add(stack);
        }
        return distinct.stream().flatMap(stack -> map.findRecipeQuery().items(stack).findAll());
    }

    private final IntSupplier parallel, threads;
    private int cursor, recipeCount;
    private long parallelCount;
    private WirelessOutputs outputs;

    public ParallelBatchProcessingLogic(IntSupplier parallel, IntSupplier threads) {
        this.parallel = parallel;
        this.threads = threads;
    }

    @Override
    protected CheckRecipeResult processRecipe() {
        outputs = null;
        recipeCount = 0;
        parallelCount = 0;
        if (availableVoltage <= 0) return CheckRecipeResultRegistry.NO_RECIPE;
        List<GTRecipe> matches = findRecipeMatches(getCurrentRecipeMap())
            .filter(r -> r.mEnabled && r.mEUt >= 0 && r.mEUt <= availableVoltage)
            .filter(r -> r.mOutputs.length > 0 || r.mFluidOutputs.length > 0).collect(Collectors.toList());
        if (matches.isEmpty()) return CheckRecipeResultRegistry.NO_RECIPE;
        int count = Math.min(threads.getAsInt(), matches.size());
        List<GTRecipe> recipes = new ArrayList<>();
        for (int i = 0; i < count; i++) recipes.add(matches.get((cursor + i) % matches.size()));
        cursor = (cursor + count) % matches.size();
        long quota = (long) Math.max(1, parallel.getAsInt()) * Math.max(1, threads.getAsInt());
        long[] wants = new long[count];
        for (int i = 0; i < count; i++)
            wants[i] = LightHunterBatchProcessingLogic.maximumParallel(recipes.get(i), inputItems, inputFluids);
        long[] allocations = LightHunterRecipeRules.fairAllocation(wants, quota);
        BigInteger energy = BigInteger.ZERO;
        List<WirelessOutputs> batches = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            GTRecipe recipe = recipes.get(i);
            long amount = Math.min(allocations[i],
                LightHunterBatchProcessingLogic.maximumParallel(recipe, inputItems, inputFluids));
            if (amount <= 0) continue;
            energy = energy.add(BigInteger.valueOf(recipe.mEUt).multiply(BigInteger.valueOf(recipe.mDuration))
                .multiply(BigInteger.valueOf(amount)));
            try {
                LightHunterRecipeRules.normalRates(energy, availableVoltage, 20);
            } catch (ArithmeticException overflow) {
                return CheckRecipeResultRegistry.DURATION_OVERFLOW;
            }
            long remaining = amount;
            while (remaining > 0) {
                int slice = (int) Math.min(Integer.MAX_VALUE, remaining);
                if (!recipe.isRecipeInputEqual(true, false, slice, inputFluids, inputItems))
                    return CheckRecipeResultRegistry.INTERNAL_ERROR;
                remaining -= slice;
            }
            WirelessOutputs batch = WirelessOutputs.forRecipe(recipe);
            batch.addRecipeBatch(recipe, BigInteger.valueOf(amount));
            batches.add(batch);
            parallelCount += amount;
            recipeCount++;
            if (energy.compareTo(BigInteger.valueOf(availableVoltage).multiply(BigInteger.valueOf(10000))) > 0) break;
        }
        if (batches.isEmpty()) return CheckRecipeResultRegistry.NO_RECIPE;
        outputs = WirelessOutputs.combine(batches);
        var rates = LightHunterRecipeRules.normalRates(energy, availableVoltage, 20);
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
}
