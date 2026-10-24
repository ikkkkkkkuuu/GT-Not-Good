// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.machines.processing;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.ParallelHelper;

/** Keeps recipe consumption private until the controller has accepted and funded the execution core. */
public class ModularProcessingLogic extends ProcessingLogic {

    private boolean preparing;
    private boolean noOverclock;
    private long availablePower = Long.MAX_VALUE;
    private final Map<ItemStack, ItemStack> itemCopies = new IdentityHashMap<>();
    private final Map<FluidStack, FluidStack> fluidCopies = new IdentityHashMap<>();
    private final Map<ItemStack, Integer> itemAmounts = new IdentityHashMap<>();
    private final Map<FluidStack, Integer> fluidAmounts = new IdentityHashMap<>();
    private List<RecipeInputs> patternInputs;

    public record RecipeInputs(ItemStack[] items, FluidStack[] fluids) {}

    protected record InputRecipe(GTRecipe recipe, RecipeInputs inputs) {}

    public final void resetPatternInventory() {
        activeDualInv = null;
    }

    public final CheckRecipeResult processPatternInputs(List<RecipeInputs> inputs) {
        patternInputs = inputs;
        try {
            return process();
        } finally {
            patternInputs = null;
        }
    }

    protected final List<InputRecipe> findInputRecipes() {
        List<InputRecipe> matches = new ArrayList<>();
        ItemStack[] originalItems = inputItems;
        FluidStack[] originalFluids = inputFluids;
        try {
            List<RecipeInputs> groups = patternInputs == null ? List.of(new RecipeInputs(inputItems, inputFluids))
                : patternInputs;
            for (RecipeInputs group : groups) {
                inputItems = group.items();
                inputFluids = group.fluids();
                findRecipeMatches(getCurrentRecipeMap()).distinct()
                    .forEach(recipe -> matches.add(new InputRecipe(recipe, group)));
            }
        } finally {
            inputItems = originalItems;
            inputFluids = originalFluids;
        }
        return matches;
    }

    public final void prepareModuleRecipe(long power, boolean wireless) {
        preparing = true;
        noOverclock = wireless;
        availablePower = Math.max(0, power);
        discardInputs();
    }

    public final boolean isPreparingModuleRecipe() {
        return preparing;
    }

    public final boolean isModuleOverclockDisabled() {
        return noOverclock;
    }

    @Override
    @Nonnull
    public final CheckRecipeResult process() {
        if (euModSupplier != null) euModifier = euModSupplier.get();
        if (speedBoostSupplier != null) speedBoost = speedBoostSupplier.get();
        if (!preparing) return processRecipe();
        discardInputs();
        ItemStack[] originalItems = inputItems;
        FluidStack[] originalFluids = inputFluids;
        List<RecipeInputs> originalPatterns = patternInputs;
        inputItems = copyItems(originalItems);
        inputFluids = copyFluids(originalFluids);
        if (patternInputs != null) {
            patternInputs = new ArrayList<>();
            for (RecipeInputs group : originalPatterns)
                patternInputs.add(new RecipeInputs(copyItems(group.items()), copyFluids(group.fluids())));
        }
        try {
            return processRecipe();
        } finally {
            inputItems = originalItems;
            inputFluids = originalFluids;
            patternInputs = originalPatterns;
        }
    }

    @Nonnull
    protected CheckRecipeResult processRecipe() {
        return super.process();
    }

    @Override
    @Nonnull
    protected ParallelHelper createParallelHelper(@Nonnull GTRecipe recipe) {
        ParallelHelper helper = super.createParallelHelper(recipe);
        return preparing ? helper.setAvailableEUt(availablePower) : helper;
    }

    @Override
    @Nonnull
    protected OverclockCalculator createOverclockCalculator(@Nonnull GTRecipe recipe) {
        if (preparing && noOverclock)
            return OverclockCalculator.ofNoOverclock(recipe).setEUtDiscount(euModifier).setDurationModifier(speedBoost);
        return super.createOverclockCalculator(recipe);
    }

    private ItemStack[] copyItems(ItemStack[] items) {
        if (items == null) return new ItemStack[0];
        ItemStack[] copies = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            ItemStack item = items[i];
            if (item == null) continue;
            ItemStack copy = itemCopies.get(item);
            if (copy == null) {
                copy = item.copy();
                itemCopies.put(item, copy);
                itemAmounts.put(item, item.stackSize);
            }
            copies[i] = copy;
        }
        return copies;
    }

    private FluidStack[] copyFluids(FluidStack[] fluids) {
        if (fluids == null) return new FluidStack[0];
        FluidStack[] copies = new FluidStack[fluids.length];
        for (int i = 0; i < fluids.length; i++) {
            FluidStack fluid = fluids[i];
            if (fluid == null) continue;
            FluidStack copy = fluidCopies.get(fluid);
            if (copy == null) {
                copy = fluid.copy();
                fluidCopies.put(fluid, copy);
                fluidAmounts.put(fluid, fluid.amount);
            }
            copies[i] = copy;
        }
        return copies;
    }

    public final boolean canCommitInputs() {
        for (Map.Entry<ItemStack, ItemStack> entry : itemCopies.entrySet()) {
            ItemStack original = entry.getKey(), copy = entry.getValue();
            if (
                original.stackSize != itemAmounts.get(original) || copy.stackSize < 0
                    || !original.isItemEqual(copy)
                    || !ItemStack.areItemStackTagsEqual(original, copy)
            ) return false;
        }
        for (Map.Entry<FluidStack, FluidStack> entry : fluidCopies.entrySet()) {
            if (
                entry.getKey().amount != fluidAmounts.get(entry.getKey()) || entry.getValue().amount < 0
                    || !entry.getKey().isFluidEqual(entry.getValue())
            ) return false;
        }
        return true;
    }

    public final int getPlannedConsumption(ItemStack original) {
        ItemStack copy = itemCopies.get(original);
        return copy == null ? 0 : Math.max(0, itemAmounts.get(original) - copy.stackSize);
    }

    public final int getPlannedConsumption(FluidStack original) {
        FluidStack copy = fluidCopies.get(original);
        return copy == null ? 0 : Math.max(0, fluidAmounts.get(original) - copy.amount);
    }

    public final boolean commitInputs() {
        if (!preparing || !canCommitInputs()) return false;
        itemCopies.forEach((original, copy) -> original.stackSize = copy.stackSize);
        fluidCopies.forEach((original, copy) -> original.amount = copy.amount);
        finishModuleRecipe();
        return true;
    }

    public final void finishModuleRecipe() {
        preparing = false;
        discardInputs();
    }

    private void discardInputs() {
        itemCopies.clear();
        fluidCopies.clear();
        itemAmounts.clear();
        fluidAmounts.clear();
    }
}
