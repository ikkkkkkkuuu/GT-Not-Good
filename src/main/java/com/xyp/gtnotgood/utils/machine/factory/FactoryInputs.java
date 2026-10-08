package com.xyp.gtnotgood.utils.machine.factory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.objects.GTDualInputPattern;
import gregtech.common.tileentities.machines.IDualInputInventory;
import gregtech.common.tileentities.machines.IDualInputInventoryWithPattern;

/** Live, isolated views of one crafting buffer; array/list copies never copy the stacks that must be debited. */
public final class FactoryInputs {

    public final List<ItemStack> items = new ArrayList<>();
    public final List<FluidStack> fluids = new ArrayList<>();
    public final boolean patterned;
    public final GTDualInputPattern pattern;

    /** Pattern signatures exclude shared deposits; consumed stacks always remain references to this inventory. */
    public FactoryInputs(List<ItemStack> shared, IDualInputInventory inventory) {
        patterned = inventory instanceof IDualInputInventoryWithPattern;
        GTDualInputPattern raw = patterned ? ((IDualInputInventoryWithPattern) inventory).getPatternInputs() : null;
        Set<ItemStack> sharedRefs = Collections.newSetFromMap(new IdentityHashMap<>());
        sharedRefs.addAll(shared);
        pattern = raw == null ? null
            : new GTDualInputPattern(
                raw.inputItems == null ? new ItemStack[0]
                    : Arrays.stream(raw.inputItems).filter(item -> item != null && !sharedRefs.contains(item))
                        .map(ItemStack::copy).toArray(ItemStack[]::new),
                raw.inputFluid == null ? new FluidStack[0]
                    : Arrays.stream(raw.inputFluid).filter(fluid -> fluid != null).map(FluidStack::copy)
                        .toArray(FluidStack[]::new));
        Set<ItemStack> itemRefs = Collections.newSetFromMap(new IdentityHashMap<>());
        // Pattern jobs debit only their own inventory. Shared slots remain available for installed requirements.
        if (!patterned) for (ItemStack item : shared) addItem(item, itemRefs);
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
