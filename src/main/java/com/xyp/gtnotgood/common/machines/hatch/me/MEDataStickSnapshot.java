package com.xyp.gtnotgood.common.machines.hatch.me;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.item.ItemStack;

import gregtech.api.util.GTUtility.ItemId;

/** A read-only snapshot of distinct data sticks; quantities and network iteration order do not change recipes. */
final class MEDataStickSnapshot {

    private Map<ItemId, ItemStack> sticks = Collections.emptyMap();
    private ItemStack[] slots = new ItemStack[0];

    /** Replaces network contents, preserving NBT identity and reporting additions, removals or research changes. */
    boolean replace(Iterable<ItemStack> contents, ItemStack dataStick) {
        Map<ItemId, ItemStack> next = new LinkedHashMap<>();
        for (ItemStack stack : contents) {
            if (stack == null || stack.stackSize <= 0 || !matches(stack, dataStick)) continue;
            ItemStack copy = stack.copy();
            copy.stackSize = 1;
            next.put(ItemId.create(copy.getItem(), copy.getItemDamage(), copy.getTagCompound()), copy);
        }
        if (sticks.keySet()
            .equals(next.keySet())) return false;
        sticks = next;
        slots = next.values()
            .toArray(new ItemStack[0]);
        return true;
    }

    static boolean matches(ItemStack stack, ItemStack dataStick) {
        return stack != null && dataStick != null
            && stack.getItem() == dataStick.getItem()
            && stack.getItemDamage() == dataStick.getItemDamage();
    }

    boolean clear() {
        if (slots.length == 0) return false;
        sticks = Collections.emptyMap();
        slots = new ItemStack[0];
        return true;
    }

    int size() {
        return slots.length;
    }

    ItemStack get(int index) {
        return index < 0 || index >= slots.length ? null : slots[index].copy();
    }
}
