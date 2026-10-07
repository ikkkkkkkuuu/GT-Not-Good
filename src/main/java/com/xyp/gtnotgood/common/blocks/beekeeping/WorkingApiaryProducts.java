package com.xyp.gtnotgood.common.blocks.beekeeping;

import java.util.List;
import java.util.Random;

import net.minecraft.item.ItemStack;

/** Scales successful native product rolls without changing their chances, NBT, or maximum stack sizes. */
public final class WorkingApiaryProducts {

    private WorkingApiaryProducts() {}

    /**
     * Adds a scaled product, probabilistically rounding the fractional item count. The neutral multiplier consumes
     * no random numbers, preserving native seeded behavior. Source stacks are never mutated.
     *
     * @param products   native output list, before flower-provider transformations
     * @param product    successful ordinary or specialty roll
     * @param multiplier configured quantity multiplier, from zero to 64
     * @param random     housing world's server RNG
     * @return whether any items were added
     */
    public static boolean add(List<ItemStack> products, ItemStack product, double multiplier, Random random) {
        if (multiplier == 1) return products.add(product);
        double scaled = product.stackSize * multiplier;
        int count = (int) scaled;
        double fraction = scaled - count;
        if (fraction > 0 && random.nextDouble() < fraction) count++;
        boolean added = count > 0;
        int limit = Math.max(1, product.getMaxStackSize());
        while (count > 0) {
            ItemStack copy = product.copy();
            copy.stackSize = Math.min(count, limit);
            products.add(copy);
            count -= copy.stackSize;
        }
        return added;
    }
}
