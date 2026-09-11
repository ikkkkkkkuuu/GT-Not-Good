package com.xyp.gtnotgood.utils.machine.factory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import gregtech.common.tileentities.machines.IDualInputInventory;

/** Live, isolated views of one crafting buffer; array/list copies never copy the stacks that must be debited. */
public final class FactoryInputs {

    public final List<ItemStack> items = new ArrayList<>();
    public final List<FluidStack> fluids = new ArrayList<>();

    /** Shared manual slots are combined only with this buffer, never another AE task's inventory. */
    public FactoryInputs(List<ItemStack> shared, IDualInputInventory inventory) {
        Set<ItemStack> itemRefs = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ItemStack item : shared) addItem(item, itemRefs);
        ItemStack[] inputs = inventory.getItemInputs();
        if (inputs != null) for (ItemStack item : inputs) addItem(item, itemRefs);
        Set<FluidStack> fluidRefs = Collections.newSetFromMap(new IdentityHashMap<>());
        FluidStack[] fluidInputs = inventory.getFluidInputs();
        if (fluidInputs != null) for (FluidStack fluid : fluidInputs)
            if (fluid != null && fluid.amount > 0 && fluidRefs.add(fluid)) fluids.add(fluid);
    }

    private void addItem(ItemStack item, Set<ItemStack> seen) {
        if (item != null && item.stackSize > 0 && seen.add(item)) items.add(item);
    }
}
