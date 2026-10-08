package com.xyp.gtnotgood.common.blocks.network;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.Test;

/** Regression coverage for multi-furnace supply and the three allocation policies. */
public class NetworkDistributionTest {

    @Test
    public void equalSupplyToEightFurnaces() {
        assertArrayEquals(new long[] { 8, 8, 8, 8, 8, 8, 8, 8 },
            NetworkDistribution.allocate(64, new long[] { 64, 64, 64, 64, 64, 64, 64, 64 }, 1));
        assertArrayEquals(new long[] { 1, 1, 1, 1, 1, 1, 1, 1 },
            NetworkDistribution.allocate(6400, new long[] { 1, 1, 1, 1, 1, 1, 1, 1 }, 1));
        assertArrayEquals(new long[] { 0, 1, 32, 31 },
            NetworkDistribution.allocate(64, new long[] { 0, 1, 64, 64 }, 1));
    }

    @Test
    public void sequentialModesContinuePastTheFirstRecipient() {
        long[] demand = { 0, 32, 64 };
        assertArrayEquals(new long[] { 0, 32, 32 }, NetworkDistribution.allocate(64, demand, 0));
        assertArrayEquals(new long[] { 0, 32, 32 }, NetworkDistribution.allocate(64, demand, 2));
        assertArrayEquals(new long[] { 4, 3, 3 }, NetworkDistribution.allocate(10, new long[] { 10, 10, 10 }, 1));
    }

    @Test
    public void rotationSuppliesEightStacksInOneOperation() {
        assertArrayEquals(new long[] { 64, 64, 64, 64, 64, 64, 64, 64 },
            NetworkDistribution.allocate(6400, new long[] { 64, 64, 64, 64, 64, 64, 64, 64 }, 0));
    }

    @Test
    public void insertBatchSpansSlotsAndRespectsEndpointLimit() {
        InventoryBasic chest = new InventoryBasic("test", true, 9);
        ItemStack offer = new ItemStack(new Item(), 512);
        assertEquals(512, NetworkTransfer.insert(chest, 0, offer, 6400, true));
        for (int i = 0; i < 9; i++) assertNull(chest.getStackInSlot(i));
        assertEquals(128, NetworkTransfer.insert(chest, 0, offer, 128, false));
        assertEquals(64, chest.getStackInSlot(0).stackSize);
        assertEquals(64, chest.getStackInSlot(1).stackSize);
        assertEquals(448, NetworkTransfer.insert(chest, 0, offer, 6400, false));
        for (int i = 0; i < 9; i++) assertEquals(64, chest.getStackInSlot(i).stackSize);
        assertEquals(512, offer.stackSize);
    }

    @Test
    public void largeEnergyBudgetDoesNotOverflow() {
        long[] allocation = NetworkDistribution.allocate(Long.MAX_VALUE, new long[] { Long.MAX_VALUE, Long.MAX_VALUE },
            1);
        assertEquals(Long.MAX_VALUE, allocation[0] + allocation[1]);
        assertEquals(1, allocation[0] - allocation[1]);
    }

    @Test
    public void collectAcrossChestSlotsWithoutLosingItems() {
        InventoryBasic chest = new InventoryBasic("test", true, 9);
        Item item = new Item();
        for (int i = 0; i < 8; i++) chest.setInventorySlotContents(i, new ItemStack(item, 64));
        ItemStack result = NetworkTransfer.collect(chest, 0, new ItemStack(item), 500);
        assertEquals(500, result.stackSize);
        for (int i = 0; i < 7; i++) assertNull(chest.getStackInSlot(i));
        assertEquals(12, chest.getStackInSlot(7).stackSize);
    }
}
