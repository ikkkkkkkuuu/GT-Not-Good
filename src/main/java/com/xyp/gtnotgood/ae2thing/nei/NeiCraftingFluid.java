package com.xyp.gtnotgood.ae2thing.nei;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.common.compat.FluidDropCompat;

import codechicken.nei.recipe.StackInfo;

public final class NeiCraftingFluid {

    private NeiCraftingFluid() {}

    public static FluidStack getFluid(ItemStack stack) {
        if (stack == null || !(StackInfo.isFluidDisplayItem(stack) || FluidDropCompat.isFluidDrop(stack))) return null;
        FluidStack fluid = StackInfo.getFluid(stack);
        if (fluid == null) return null;
        // NEI caches this stack; zero-volume display entries still identify a valid craft target.
        fluid = fluid.copy();
        fluid.amount = 1;
        return fluid;
    }
}
