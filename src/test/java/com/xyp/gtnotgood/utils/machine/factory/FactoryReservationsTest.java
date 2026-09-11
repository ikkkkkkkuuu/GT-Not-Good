package com.xyp.gtnotgood.utils.machine.factory;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

/** Reservation ownership survives repeated collection, failed output, and world saves without duplication. */
public class FactoryReservationsTest {

    @Test
    public void refundProgressContainsOnlyItemsStillHeldAndReturnsCopies() {
        FactoryReservations inventory = new FactoryReservations();
        ItemStack input = new ItemStack((Item) null, 2, 0);
        inventory.collectSlot(0, 0, Collections.singletonList(input), stack -> true);
        inventory.collectSlot(0, 1, Collections.singletonList(input), stack -> true);
        assertEquals(
            2,
            inventory.remaining()
                .size());
        inventory.remaining()
            .get(0).stackSize = 0;
        assertEquals(1, inventory.get(0, 0).stackSize);
        final int[] attempt = { 0 };
        assertFalse(inventory.refund(stack -> attempt[0]++ == 0));
        assertEquals(
            1,
            inventory.remaining()
                .size());
        assertTrue(inventory.refund(stack -> true));
        assertTrue(
            inventory.remaining()
                .isEmpty());
    }

    @Test
    public void actualNbtIsSavedAndBlockedRefundDoesNotTakeTwice() {
        ItemStack input = new ItemStack((Item) null, 3, 7);
        input.setTagCompound(new NBTTagCompound());
        input.getTagCompound()
            .setString("owner", "test-owner");
        FactoryReservations inventory = new FactoryReservations();
        List<ItemStack> bus = Collections.singletonList(input);
        assertTrue(inventory.collectSlot(0, 0, bus, stack -> true));
        assertTrue(inventory.collectSlot(0, 0, bus, stack -> true));
        assertEquals(2, input.stackSize);
        NBTTagCompound saved = inventory.write()
            .getTagList("nodes", 10)
            .getCompoundTagAt(0)
            .getCompoundTag("slot0");
        assertEquals(
            "test-owner",
            saved.getCompoundTag("tag")
                .getString("owner"));
        assertEquals(1, saved.getByte("Count"));
        FactoryReservations restored = inventory;
        assertFalse(restored.refund(stack -> false));
        assertEquals(
            "test-owner",
            restored.get(0, 0)
                .getTagCompound()
                .getString("owner"));
        List<ItemStack> returned = new ArrayList<>();
        assertTrue(restored.refund(returned::add));
        assertTrue(restored.refund(returned::add));
        assertEquals(1, returned.size());
        assertEquals(1, returned.get(0).stackSize);
        assertEquals(
            7,
            returned.get(0)
                .writeToNBT(new NBTTagCompound())
                .getShort("Damage"));
        assertNull(restored.get(0, 0));
    }

    @Test
    public void eachNodeNeedsItsOwnControllerAndCatalystReservation() {
        ItemStack input = new ItemStack((Item) null, 3, 0);
        FactoryReservations inventory = new FactoryReservations();
        List<ItemStack> bus = Collections.singletonList(input);
        assertTrue(inventory.collectSlot(0, 0, bus, stack -> true));
        assertTrue(inventory.collectSlot(0, 1, bus, stack -> true));
        assertTrue(inventory.collectSlot(1, 0, bus, stack -> true));
        assertFalse(inventory.collectSlot(1, 1, bus, stack -> true));
        assertEquals(0, input.stackSize);
        List<ItemStack> returned = new ArrayList<>();
        assertTrue(inventory.refund(returned::add));
        assertEquals(3, returned.size());
    }
}
