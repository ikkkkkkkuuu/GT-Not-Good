package com.xyp.gtnotgood.common.blocks.stockio;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/**
 * Mutable recipe inputs backed by one ME transaction. The machine changes only the remaining amounts;
 * {@link StockIOLogic#endRecipe(StockIOSnapshot)} commits the consumed difference after recipe acceptance.
 * These arrays contain simulated stock and must never be exposed as a physical inventory.
 */
public final class StockIOSnapshot {

    public final ItemStack[] items = new ItemStack[StockIOLogic.SLOT_COUNT];
    public final FluidStack[] fluids = new FluidStack[StockIOLogic.SLOT_COUNT];
    final ItemStack[] originalItems = new ItemStack[StockIOLogic.SLOT_COUNT];
    final FluidStack[] originalFluids = new FluidStack[StockIOLogic.SLOT_COUNT];
    final StockIOLogic owner;
    boolean closed;

    StockIOSnapshot(StockIOLogic owner) {
        this.owner = owner;
    }
}
