package com.xyp.gtnotgood.common.items.patternsorter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.xyp.gtnotgood.common.compat.AutomaticMachineCircuit;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import gregtech.api.recipe.RecipeMap;

/** Read-only classification and lossless permutation of the player's existing pattern stacks. */
public final class PatternSorter {

    public static final int MATCHED = 0, AMBIGUOUS = 1, UNMATCHED = 2, CRAFTING = 3, INVALID = 4;

    private PatternSorter() {}

    /** A server-classified inventory slot; preview stacks never replace the player's authoritative items. */
    public static final class Entry {

        public final int slot, status, circuit, mold;
        public final ItemStack stack;

        public Entry(int slot, int status, int circuit, int mold, ItemStack stack) {
            this.slot = slot;
            this.status = status;
            this.circuit = circuit;
            this.mold = mold;
            this.stack = stack;
        }

        public String group() {
            return status + ":" + circuit + ":" + mold;
        }
    }

    /**
     * Examines only ordinary player inventory slots in screen order (main inventory then hotbar).
     * Unsupported or malformed patterns remain visible as unresolved entries instead of being guessed.
     *
     * @param inventory live player main inventory, never modified by this method
     * @param world     server world used by AE's native pattern decoder
     * @param map       selected recipe map, or null until the user chooses one
     * @return one copied preview per occupied pattern slot
     */
    public static List<Entry> classify(ItemStack[] inventory, World world, RecipeMap<?> map) {
        List<Entry> result = new ArrayList<>();
        for (int order = 0; order < 36; order++) {
            int slot = (order + 9) % 36;
            ItemStack stack = inventory[slot];
            if (stack == null || !(stack.getItem() instanceof ICraftingPatternItem item)) continue;
            int status = INVALID, circuit = -1, mold = -1;
            try {
                ICraftingPatternDetails details = item.getPatternForItem(stack.copy(), world);
                if (details != null) {
                    if (details.isCraftable()) status = CRAFTING;
                    else {
                        List<int[]> configurations = AutomaticMachineCircuit.patternConfigurations(map, details);
                        status = configurations.isEmpty() ? UNMATCHED : configurations.size() > 1 ? AMBIGUOUS : MATCHED;
                        if (status == MATCHED) {
                            circuit = configurations.get(0)[0];
                            mold = configurations.get(0)[1];
                        }
                    }
                }
            } catch (RuntimeException ignored) {
                // Invalid third-party pattern data is shown to the player; nothing is rewritten or discarded.
            }
            result.add(new Entry(slot, status, circuit, mold, stack.copy()));
        }
        return result;
    }

    /**
     * Reorders only the occupied slots in a fresh classification snapshot. All source references are captured
     * before writing; counts, NBT, non-pattern items and empty slots are preserved. Stale snapshots are rejected.
     *
     * @param inventory  authoritative main inventory
     * @param entries    classification snapshot in visual slot order
     * @param firstGroup optional group to bring to the front; empty sorts all groups
     * @return whether the snapshot was current and the permutation was applied
     */
    public static boolean reorder(ItemStack[] inventory, List<Entry> entries, String firstGroup) {
        boolean[] seen = new boolean[inventory.length];
        for (Entry entry : entries) {
            if (entry.slot < 0 || entry.slot >= inventory.length
                || seen[entry.slot]
                || !ItemStack.areItemStacksEqual(inventory[entry.slot], entry.stack)) return false;
            seen[entry.slot] = true;
        }
        List<Entry> sorted = new ArrayList<>(entries);
        sorted.sort(
            Comparator.comparingInt(
                (Entry entry) -> entry.group()
                    .equals(firstGroup) ? 0 : 1)
                .thenComparingInt(entry -> entry.status)
                .thenComparingInt(entry -> entry.circuit)
                .thenComparingInt(entry -> entry.mold));
        List<ItemStack> original = new ArrayList<>();
        for (Entry entry : sorted) original.add(inventory[entry.slot]);
        for (int i = 0; i < entries.size(); i++) inventory[entries.get(i).slot] = original.get(i);
        return true;
    }
}
