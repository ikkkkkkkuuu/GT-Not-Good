package com.xyp.gtnotgood.client.rts;

import static org.junit.Assert.*;

import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.Test;

import com.rtsbuilding.rtsbuilding.platform.storage.InventoryItemHandler;
import com.rtsbuilding.rtsbuilding.server.storage.cache.RtsAggregateStorage;
import com.rtsbuilding.rtsbuilding.server.storage.cache.RtsHandlerCache;

/** Exercises real insertion and change tracking when legacy handlers return null on success. */
public class OfficialRtsAggregateInsertTest {

    @Test
    public void networkStyleInsertionCanReturnNullWithoutSlots() {
        RtsAggregateStorage storage = new RtsAggregateStorage();
        NetworkHandler handler = new NetworkHandler();
        storage.mount(0, handler, new RtsHandlerCache());
        assertNull(storage.insert(new ItemStack(new Item(), 8), false));
        assertEquals(8, handler.stored);
        assertFalse(
            storage.drainPendingChanges()
                .isEmpty());
    }

    /** AE-style insertion is independent of visible slots and returns null when fully accepted. */
    private static final class NetworkHandler implements com.rtsbuilding.rtsbuilding.platform.storage.IItemHandler,
        com.rtsbuilding.rtsbuilding.compat.AnySlotInsertItemHandler {

        private int stored;

        public int getSlots() {
            return 0;
        }

        public ItemStack getStackInSlot(int slot) {
            return null;
        }

        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            throw new AssertionError("slot insertion used");
        }

        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return null;
        }

        public int getSlotLimit(int slot) {
            return 64;
        }

        public ItemStack insertItemAnywhere(ItemStack stack, boolean simulate) {
            if (!simulate) stored += stack.stackSize;
            return null;
        }
    }

    @Test
    public void completeInsertionTracksChangeAndPreservesInput() {
        InventoryBasic inventory = new InventoryBasic("test", false, 1);
        RtsAggregateStorage storage = storage(inventory);
        ItemStack input = new ItemStack(new Item(), 8);
        assertNull(storage.insert(input, false));
        assertEquals(8, inventory.getStackInSlot(0).stackSize);
        assertEquals(8, input.stackSize);
        assertFalse(
            storage.drainPendingChanges()
                .isEmpty());
    }

    @Test
    public void partialInsertionConservesQuantity() {
        Item item = new Item();
        InventoryBasic inventory = new InventoryBasic("test", false, 1);
        inventory.setInventorySlotContents(0, new ItemStack(item, 60));
        RtsAggregateStorage storage = storage(inventory);
        ItemStack remainder = storage.insert(new ItemStack(item, 8), false);
        assertEquals(4, remainder.stackSize);
        assertEquals(64, inventory.getStackInSlot(0).stackSize);
        assertFalse(
            storage.drainPendingChanges()
                .isEmpty());
    }

    @Test
    public void fullInventoryLeavesInputAndChangeSetUntouched() {
        Item item = new Item();
        InventoryBasic inventory = new InventoryBasic("test", false, 1);
        inventory.setInventorySlotContents(0, new ItemStack(item, 64));
        RtsAggregateStorage storage = storage(inventory);
        assertEquals(8, storage.insert(new ItemStack(item, 8), false).stackSize);
        assertTrue(
            storage.drainPendingChanges()
                .isEmpty());
    }

    @Test
    public void simulationWithNoRemainderDoesNotMutate() {
        InventoryBasic inventory = new InventoryBasic("test", false, 1);
        RtsAggregateStorage storage = storage(inventory);
        assertNull(storage.insert(new ItemStack(new Item(), 8), true));
        assertNull(inventory.getStackInSlot(0));
        assertTrue(
            storage.drainPendingChanges()
                .isEmpty());
    }

    private static RtsAggregateStorage storage(InventoryBasic inventory) {
        RtsAggregateStorage storage = new RtsAggregateStorage();
        storage.mount(0, new InventoryItemHandler(inventory), new RtsHandlerCache());
        return storage;
    }
}
