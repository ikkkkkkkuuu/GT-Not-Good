package com.xyp.gtnotgood.common.items.patternsorter;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

/** Regression checks for lossless inventory permutations, stale data and duplicate-slot rejection. */
public class PatternSorterTest {

    @Test
    public void sortingKeepsOriginalReferencesCountsNbtAndOtherSlots() {
        ItemStack[] inventory = new ItemStack[36];
        ItemStack high = stack("circuit 24", 3), low = stack("circuit 1", 1), other = stack("tool", 1);
        inventory[9] = high;
        inventory[14] = low;
        inventory[10] = other;
        List<PatternSorter.Entry> entries = Arrays.asList(entry(9, 24, high), entry(14, 1, low));
        assertTrue(PatternSorter.reorder(inventory, entries, ""));
        assertSame(low, inventory[9]);
        assertSame(high, inventory[14]);
        assertSame(other, inventory[10]);
        assertNull(inventory[11]);
        assertEquals(3, high.stackSize);
        assertEquals(
            "circuit 24",
            high.getTagCompound()
                .getString("identity"));
    }

    @Test
    public void selectedGroupMovesFirstWhileEqualGroupsRemainStable() {
        ItemStack[] inventory = new ItemStack[36];
        inventory[9] = stack("first", 1);
        inventory[10] = stack("second", 1);
        inventory[0] = stack("third", 1);
        ItemStack first = inventory[9], second = inventory[10], third = inventory[0];
        List<PatternSorter.Entry> entries = Arrays.asList(entry(9, 1, first), entry(10, 2, second), entry(0, 2, third));
        assertTrue(PatternSorter.reorder(inventory, entries, "0:2:-1"));
        assertSame(second, inventory[9]);
        assertSame(third, inventory[10]);
        assertSame(first, inventory[0]);
    }

    @Test
    public void staleOrDuplicateSnapshotsCannotOverwriteItems() {
        ItemStack[] inventory = new ItemStack[36];
        inventory[9] = stack("first", 1);
        inventory[10] = stack("second", 1);
        PatternSorter.Entry a = entry(9, 2, inventory[9]), b = entry(10, 1, inventory[10]);
        ItemStack replacement = stack("new item", 1);
        inventory[10] = replacement;
        assertFalse(PatternSorter.reorder(inventory, Arrays.asList(a, b), ""));
        assertSame(replacement, inventory[10]);
        assertEquals(
            "first",
            inventory[9].getTagCompound()
                .getString("identity"));
        assertFalse(PatternSorter.reorder(inventory, Arrays.asList(a, a), ""));
    }

    private static PatternSorter.Entry entry(int slot, int circuit, ItemStack stack) {
        return new PatternSorter.Entry(slot, PatternSorter.MATCHED, circuit, -1, stack.copy());
    }

    private static ItemStack stack(String identity, int count) {
        ItemStack stack = new ItemStack(new Item(), count);
        stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound()
            .setString("identity", identity);
        return stack;
    }
}
