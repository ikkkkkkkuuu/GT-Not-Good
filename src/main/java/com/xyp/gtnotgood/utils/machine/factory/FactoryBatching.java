package com.xyp.gtnotgood.utils.machine.factory;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.util.GTRecipe;

/**
 * Bounds automatic execution batches before consuming inputs or rolling chance outputs.
 * Preview quantities remain the configured node quantities. Physical stacks and buffers still use
 * signed integers, so the advertised maximum is a ceiling rather than an unconditional batch size.
 */
public final class FactoryBatching {

    private FactoryBatching() {}

    /** Bounds repeated amounts using long arithmetic, including space already occupied in a buffer. */
    static int amountLimit(int requested, long perRecipe, long stored) {
        if (perRecipe <= 0) return Math.max(0, requested);
        long room = Math.max(0L, Integer.MAX_VALUE - Math.max(0L, stored));
        return (int) Math.max(0L, Math.min(requested, room / perRecipe));
    }

    /** Uses remaining per-tick energy headroom without multiplying an unbounded candidate count. */
    public static int powerLimit(int requested, long unitEUt, long availableEUt) {
        if (unitEUt <= 0) return Math.max(0, requested);
        return (int) Math.max(0L, Math.min(requested, Math.max(0L, availableEUt) / unitEUt));
    }

    /** Prefer whole configured node batches; still permit a smaller final batch when supplies are scarce. */
    public static int align(int requested, int nodeParallel) {
        int unit = Math.max(1, nodeParallel);
        return requested < unit ? Math.max(0, requested) : requested / unit * unit;
    }

    /** Prevents individual input multiplications from overflowing before GT consumes a selected batch. */
    public static int inputLimit(GTRecipe recipe, int requested) {
        int limit = requested;
        for (ItemStack input : recipe.mInputs) if (input != null) limit = amountLimit(limit, input.stackSize, 0);
        for (FluidStack input : recipe.mFluidInputs) if (input != null) limit = amountLimit(limit, input.amount, 0);
        return limit;
    }

    /**
     * Reserves worst-case output space, grouping duplicate outputs exactly as FactoryRuntime does.
     * Chance rolls cannot subsequently turn a valid automatic batch into an overflow.
     *
     * @param recipe    original node recipe, including chance outputs
     * @param previous  completed products still waiting in this node's buffer
     * @param requested ceiling obtained from available materials and power
     * @return safe recipe execution count, possibly zero
     */
    public static int recipeLimit(GTRecipe recipe, FactoryRuntime.State previous, int requested) {
        int limit = inputLimit(recipe, requested);
        for (int i = 0; i < recipe.mOutputs.length; i++) {
            ItemStack output = recipe.mOutputs[i];
            if (output == null || output.stackSize <= 0 || recipe.getOutputChance(i) <= 0) continue;
            long perRecipe = 0, stored = 0;
            for (int j = 0; j < recipe.mOutputs.length; j++) {
                ItemStack other = recipe.mOutputs[j];
                if (other != null && other.stackSize > 0 && recipe.getOutputChance(j) > 0 && sameItem(output, other))
                    perRecipe += other.stackSize;
            }
            for (ItemStack other : previous.items) if (sameItem(output, other)) stored += other.stackSize;
            limit = amountLimit(limit, perRecipe, stored);
        }
        for (FluidStack output : recipe.mFluidOutputs) {
            if (output == null || output.amount <= 0) continue;
            long perRecipe = 0, stored = 0;
            for (FluidStack other : recipe.mFluidOutputs)
                if (other != null && output.isFluidEqual(other)) perRecipe += Math.max(0, other.amount);
            for (FluidStack other : previous.fluids) if (output.isFluidEqual(other)) stored += other.amount;
            limit = amountLimit(limit, perRecipe, stored);
        }
        return limit;
    }

    private static boolean sameItem(ItemStack first, ItemStack second) {
        return first == second || first.isItemEqual(second) && ItemStack.areItemStackTagsEqual(first, second);
    }
}
