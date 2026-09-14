package com.xyp.gtnotgood.common.flux;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;

/** Exact item identity, stock targets and sided insertion shared by logistics export and regression tests. */
public final class LogisticsStock {

    private LogisticsStock() {}

    /** Restores the int cargo count after vanilla has decoded its byte-sized Count field. */
    static ItemStack restoreCargoCount(ItemStack cargo, net.minecraft.nbt.NBTTagCompound tag) {
        if (cargo != null && tag.hasKey("pendingItemCount")) {
            cargo.stackSize = Math.max(0, tag.getInteger("pendingItemCount"));
            if (cargo.stackSize == 0) return null;
        }
        return cargo;
    }

    public static int[] accessibleSlots(IInventory inventory, int side) {
        if (inventory instanceof ISidedInventory sided) return sided.getAccessibleSlotsFromSide(side);
        int[] slots = new int[inventory.getSizeInventory()];
        for (int i = 0; i < slots.length; i++) slots[i] = i;
        return slots;
    }

    /** Returns a copy of an output stack, never granting extraction from a machine input slot. */
    public static ItemStack extractable(IInventory inventory, int slot, int side) {
        if (slot < 0 || slot >= inventory.getSizeInventory()) return null;
        ItemStack stack = inventory.getStackInSlot(slot);
        if (stack == null || stack.stackSize <= 0) return null;
        if (inventory instanceof ISidedInventory sided && !sided.canExtractItem(slot, stack, side)) return null;
        return stack.copy();
    }

    public static boolean same(ItemStack a, ItemStack b) {
        return a != null && b != null && a.isItemEqual(b) && ItemStack.areItemStackTagsEqual(a, b);
    }

    /** Zero means no target; positive targets never remove excess already present in the destination. */
    public static int missing(long target, long present, int batch) {
        return target == 0 ? batch : (int) Math.max(0, Math.min(batch, target - present));
    }

    public static long count(IInventory inventory, ItemStack filter) {
        long count = 0;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (same(stack, filter)) count += Math.max(0, stack.stackSize);
        }
        return count;
    }

    /** Returns accepted count; simulation leaves both inventory and offered stack unchanged. */
    public static int insert(IInventory inventory, int side, ItemStack offered, boolean simulate) {
        int[] slots;
        if (inventory instanceof ISidedInventory sided) slots = sided.getAccessibleSlotsFromSide(side);
        else {
            slots = new int[inventory.getSizeInventory()];
            for (int i = 0; i < slots.length; i++) slots[i] = i;
        }
        if (slots == null) return 0;
        int left = offered.stackSize;
        Set<Integer> seen = new HashSet<>();
        for (int slot : slots) {
            if (slot < 0 || slot >= inventory.getSizeInventory() || !seen.add(slot)) continue;
            ItemStack current = inventory.getStackInSlot(slot);
            if (current != null && !same(current, offered)) continue;
            int capacity = Math.min(inventory.getInventoryStackLimit(), offered.getMaxStackSize());
            int amount = Math.min(left, Math.max(0, capacity - (current == null ? 0 : current.stackSize)));
            if (amount <= 0) continue;
            // Validate the portion for this slot, not the potentially billion-item aggregate offer.
            ItemStack portion = offered.copy();
            portion.stackSize = amount;
            if (!inventory.isItemValidForSlot(slot, portion)) continue;
            if (inventory instanceof ISidedInventory sided && !sided.canInsertItem(slot, portion, side)) continue;
            if (!simulate) {
                ItemStack replacement = offered.copy();
                replacement.stackSize = amount + (current == null ? 0 : current.stackSize);
                inventory.setInventorySlotContents(slot, replacement);
                inventory.markDirty();
            }
            left -= amount;
            if (left == 0) break;
        }
        return offered.stackSize - left;
    }
}
