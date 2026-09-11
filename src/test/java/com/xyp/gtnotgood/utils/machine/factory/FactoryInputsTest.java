package com.xyp.gtnotgood.utils.machine.factory;

import static org.junit.Assert.*;

import java.util.Collections;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.junit.Test;

import gregtech.common.tileentities.machines.IDualInputInventory;

/** Isolated dual-input views debit actual stack references without joining independent crafting tasks. */
public class FactoryInputsTest {

    @Test
    public void consumptionMutatesOnlyTheChosenCraftingBuffer() {
        ItemStack first = new ItemStack((Item) null, 8, 0), second = new ItemStack((Item) null, 9, 0);
        FactoryInputs a = new FactoryInputs(Collections.emptyList(), inventory(new ItemStack[] { first }, null));
        FactoryInputs b = new FactoryInputs(Collections.emptyList(), inventory(new ItemStack[] { second }, null));
        a.items.get(0).stackSize -= 3;
        assertEquals(5, first.stackSize);
        assertEquals(9, second.stackSize);
        assertSame(second, b.items.get(0));
        assertEquals(1, a.items.size());
        assertTrue(b.fluids.isEmpty());
    }

    @Test
    public void repeatedReferenceIsCountedOnceButEqualPhysicalStacksStaySeparate() {
        ItemStack shared = new ItemStack((Item) null, 1, 0), other = new ItemStack((Item) null, 1, 0);
        FactoryInputs input = new FactoryInputs(
            Collections.singletonList(shared),
            inventory(new ItemStack[] { shared, other }, null));
        assertEquals(2, input.items.size());
        assertSame(shared, input.items.get(0));
        assertSame(other, input.items.get(1));
    }

    @Test
    public void nullAndDepletedInputsAreIgnored() {
        FactoryInputs input = new FactoryInputs(
            Collections.emptyList(),
            inventory(new ItemStack[] { null, new ItemStack((Item) null, 0, 0) }, null));
        assertTrue(input.items.isEmpty());
        assertTrue(input.fluids.isEmpty());
    }

    private static IDualInputInventory inventory(ItemStack[] items, FluidStack[] fluids) {
        return new IDualInputInventory() {

            public boolean isEmpty() {
                return false;
            }

            public ItemStack[] getItemInputs() {
                return items;
            }

            public FluidStack[] getFluidInputs() {
                return fluids;
            }
        };
    }
}
