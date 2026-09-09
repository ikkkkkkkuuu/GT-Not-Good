package com.xyp.gtnotgood.common.network;

import java.util.function.Supplier;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.utils.item.IItemHandlerModifiable;

/** Fixed phantom slots with server-owned samples and a separate client mirror. No real items enter this handler. */
final class NetworkFilterHandler implements IItemHandlerModifiable {

    private final TileNetworkController controller;
    private final Supplier<NetworkRule> selected;
    private final Supplier<NetworkRule> writable;
    private final ItemStack[] client = new ItemStack[18];

    NetworkFilterHandler(TileNetworkController controller, Supplier<NetworkRule> selected,
        Supplier<NetworkRule> writable) {
        this.controller = controller;
        this.selected = selected;
        this.writable = writable;
    }

    public int getSlots() {
        return 18;
    }

    public int getSlotLimit(int slot) {
        return 1;
    }

    public ItemStack getStackInSlot(int slot) {
        if (controller.getWorldObj().isRemote) return client[slot];
        NetworkRule rule = selected.get();
        return rule == null ? null : rule.filters[slot];
    }

    public void setStackInSlot(int slot, ItemStack stack) {
        ItemStack copy = stack == null ? null : stack.copy();
        if (copy != null) copy.stackSize = 1;
        if (controller.getWorldObj().isRemote) client[slot] = copy;
        else {
            NetworkRule rule = copy == null ? selected.get() : writable.get();
            if (rule != null) {
                rule.filters[slot] = copy;
                controller.markDirty();
            }
        }
    }

    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return stack;
    }

    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return null;
    }
}
