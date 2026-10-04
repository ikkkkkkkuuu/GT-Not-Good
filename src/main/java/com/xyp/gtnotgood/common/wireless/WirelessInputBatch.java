package com.xyp.gtnotgood.common.wireless;

import java.math.BigInteger;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.util.GTRecipe;

/** Bridges native int-sized inventory snapshots to an exact, potentially larger recipe batch. */
public final class WirelessInputBatch {

    private static final BigInteger NATIVE_LIMIT = BigInteger.valueOf(Integer.MAX_VALUE);

    private WirelessInputBatch() {}

    /**
     * Consumes only private snapshots. Native recipe matching/consumption handles ore alternatives, catalysts and
     * input chances. Work is bounded by the number of native input stacks, not by the number of individual crafts.
     * A null limit means no configured or energy limit. A free recipe with no consumable inputs needs a finite limit.
     */
    public static BigInteger consume(GTRecipe recipe, ItemStack[] items, FluidStack[] fluids, BigInteger limit,
        WirelessOutputs outputs) {
        boolean consumes = false;
        for (ItemStack stack : recipe.mInputs) if (stack != null && stack.stackSize > 0) consumes = true;
        for (FluidStack fluid : recipe.mFluidInputs) if (fluid != null && fluid.amount > 0) consumes = true;
        if (!consumes) {
            if (limit == null || limit.signum() <= 0 || recipe.maxParallelCalculatedByInputs(1, fluids, items) < 1)
                return BigInteger.ZERO;
            outputs.addRecipeBatch(recipe, limit);
            return limit;
        }
        BigInteger materialBound = BigInteger.ZERO;
        for (ItemStack stack : items) if (stack != null && stack.stackSize > 0)
            materialBound = materialBound.add(BigInteger.valueOf(stack.stackSize));
        for (FluidStack fluid : fluids)
            if (fluid != null && fluid.amount > 0) materialBound = materialBound.add(BigInteger.valueOf(fluid.amount));
        BigInteger remaining = limit == null ? materialBound : limit.min(materialBound);
        BigInteger total = BigInteger.ZERO;
        while (remaining.signum() > 0) {
            int cap = remaining.min(NATIVE_LIMIT)
                .intValueExact();
            int batch = (int) recipe.maxParallelCalculatedByInputs(cap, fluids, items);
            if (batch <= 0) break;
            recipe.consumeInput(batch, fluids, items);
            outputs.addRecipeBatch(recipe, BigInteger.valueOf(batch));
            total = total.add(BigInteger.valueOf(batch));
            remaining = remaining.subtract(BigInteger.valueOf(batch));
            if (batch < cap) break;
        }
        return total;
    }
}
