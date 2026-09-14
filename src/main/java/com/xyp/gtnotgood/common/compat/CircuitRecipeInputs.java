package com.xyp.gtnotgood.common.compat;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.util.GTRecipe;

/** Validates complete input batches using GT's own ore-dictionary, alternative-input and NBT rules. */
public final class CircuitRecipeInputs {

    private CircuitRecipeInputs() {}

    /**
     * Compares total item counts and fluid amounts, without assuming representative input items are the only choices.
     * This is a cheap candidate filter only; {@link #batches} must validate the actual ingredient identities.
     *
     * @param recipe registered GT recipe
     * @param items  supplied consumed items, excluding the virtual circuit
     * @param fluids supplied fluids
     * @return possible integer operation count, or zero if quantities cannot match
     */
    public static long quantityBatches(GTRecipe recipe, ItemStack[] items, FluidStack[] fluids) {
        return CircuitPatternQuantities.batches(totals(recipe.mInputs, recipe.mFluidInputs), totals(items, fluids));
    }

    /**
     * Simulates GT consumption on copies and requires that every consumed input is used up. This accepts legal
     * alternatives without admitting extra ingredients, wrong quantities or unrelated items. Chance-based consumption
     * is excluded so validation never samples randomness or mutates the real CPU or machine inventory.
     *
     * @param recipe  registered GT recipe
     * @param circuit selected non-consumed virtual circuit, or null
     * @param items   supplied consumed items
     * @param fluids  supplied fluids
     * @return exact integer operation count, or zero if GT rejects the inputs or leaves resources over
     */
    public static long batches(GTRecipe recipe, ItemStack circuit, ItemStack[] items, FluidStack[] fluids) {
        long count = quantityBatches(recipe, items, fluids);
        if (count <= 0 || count > Integer.MAX_VALUE || !deterministic(recipe)) return 0;
        return simulate(
            count,
            circuit,
            items,
            fluids,
            (operations, copiedItems, copiedFluids) -> recipe
                .isRecipeInputEqual(true, false, operations, copiedFluids, copiedItems));
    }

    /** GT's matching-and-consumption operation, separated so copy isolation can be tested without a running Forge. */
    @FunctionalInterface
    interface InputConsumer {

        boolean consume(int operations, ItemStack[] items, FluidStack[] fluids);
    }

    /** Validates consumption on disposable copies; never passes an original resource stack to the matcher. */
    static long simulate(long count, ItemStack circuit, ItemStack[] items, FluidStack[] fluids, InputConsumer matcher) {
        if (count <= 0 || count > Integer.MAX_VALUE) return 0;
        ItemStack[] itemCopies = new ItemStack[items.length + (circuit == null ? 0 : 1)];
        for (int i = 0; i < items.length; i++) itemCopies[i] = items[i] == null ? null : items[i].copy();
        if (circuit != null) itemCopies[items.length] = circuit.copy();
        FluidStack[] fluidCopies = new FluidStack[fluids.length];
        for (int i = 0; i < fluids.length; i++) fluidCopies[i] = fluids[i] == null ? null : fluids[i].copy();
        if (!matcher.consume((int) count, itemCopies, fluidCopies)) return 0;
        for (int i = 0; i < items.length; i++) {
            if (itemCopies[i] != null && itemCopies[i].stackSize > 0) return 0;
        }
        for (FluidStack fluid : fluidCopies) if (fluid != null && fluid.amount > 0) return 0;
        return count;
    }

    /** @return whether checking consumed inputs is deterministic */
    public static boolean deterministic(GTRecipe recipe) {
        return guaranteed(recipe.mInputChances) && guaranteed(recipe.mFluidInputChances);
    }

    private static boolean guaranteed(int[] chances) {
        if (chances != null) for (int chance : chances) if (chance != 10000) return false;
        return true;
    }

    private static Map<String, Long> totals(ItemStack[] items, FluidStack[] fluids) {
        long itemCount = 0;
        long fluidAmount = 0;
        for (ItemStack item : items) if (item != null && item.stackSize > 0) itemCount += item.stackSize;
        for (FluidStack fluid : fluids) if (fluid != null && fluid.amount > 0) fluidAmount += fluid.amount;
        Map<String, Long> totals = new HashMap<>();
        if (itemCount > 0) totals.put("items", itemCount);
        if (fluidAmount > 0) totals.put("fluids", fluidAmount);
        return totals;
    }
}
