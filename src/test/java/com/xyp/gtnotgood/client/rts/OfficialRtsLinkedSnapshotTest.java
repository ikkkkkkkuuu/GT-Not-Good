package com.xyp.gtnotgood.client.rts;

import static org.junit.Assert.*;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.Test;

import com.rtsbuilding.rtsbuilding.compat.RefreshableSnapshotHandler;
import com.rtsbuilding.rtsbuilding.compat.ReportedCountItemHandler;
import com.rtsbuilding.rtsbuilding.platform.storage.IItemHandler;
import com.rtsbuilding.rtsbuilding.server.storage.cache.RtsHandlerCache;
import com.rtsbuilding.rtsbuilding.server.storage.view.LinkedItemHandlerView;

/** Verifies that linked network inventories refresh before the browser cache reads their slots. */
public class OfficialRtsLinkedSnapshotTest {

    @Test
    public void browserTracksNetworkChangesThroughPermissionWrapper() {
        verifyRefresh(true);
    }

    @Test
    public void extractOnlyNetworkStillRefreshesButRejectsInsertion() {
        verifyRefresh(false);
    }

    private void verifyRefresh(boolean allowStore) {
        SnapshotHandler network = new SnapshotHandler();
        LinkedItemHandlerView linked = new LinkedItemHandlerView(network, allowStore);
        RtsHandlerCache cache = new RtsHandlerCache();
        cache.update(linked);
        assertEquals(0, cache.getCachedSlotCount());
        network.amount = 4096;
        cache.update(linked);
        assertEquals(1, cache.getCachedSlotCount());
        assertEquals(4096L, linked.getReportedCount(0));
        network.amount = 8192;
        cache.update(linked);
        assertEquals(8192L, linked.getReportedCount(0));
        if (!allowStore) {
            ItemStack input = new ItemStack(network.item, 8);
            assertEquals(8, linked.insertItemAnywhere(input, false).stackSize);
        }
        network.amount = 0;
        cache.update(linked);
        assertEquals(0L, linked.getReportedCount(0));
        assertEquals(0, cache.getCachedSlotCount());
    }

    /** Exposes network changes only when the snapshot refresh contract is invoked. */
    private static final class SnapshotHandler
        implements IItemHandler, RefreshableSnapshotHandler, ReportedCountItemHandler {

        private final Item item = new Item();
        private long amount;
        private long snapshot;

        public void ensureFreshSnapshot() {
            snapshot = amount;
        }

        public int getSlots() {
            return snapshot > 0 ? 1 : 0;
        }

        public ItemStack getStackInSlot(int slot) {
            return new ItemStack(item, 1);
        }

        public long getReportedCount(int slot) {
            return snapshot;
        }

        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            throw new AssertionError("read-only test inventory received an insertion");
        }

        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return null;
        }

        public int getSlotLimit(int slot) {
            return 64;
        }
    }
}
