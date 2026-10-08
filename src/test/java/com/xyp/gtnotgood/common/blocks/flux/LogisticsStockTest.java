package com.xyp.gtnotgood.common.blocks.flux;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

/** Regression coverage for stock targets, exact NBT matching and simulated sided container insertion. */
public class LogisticsStockTest {

    @Test
    public void blankTargetFillsButPositiveTargetOnlyRestocksMissingAmount() {
        assertEquals(64, LogisticsStock.missing(0, 100000, 64));
        assertEquals(4, LogisticsStock.missing(100, 96, 64));
        assertEquals(0, LogisticsStock.missing(100, 101, 64));
        assertEquals(16000, LogisticsStock.missing(Long.MAX_VALUE, 0, 16000));
    }

    @Test
    public void simulationDoesNotConsumeAndInsertionStopsAtStackCapacity() {
        InventoryBasic inventory = new InventoryBasic("test", false, 2);
        Item item = new Item();
        ItemStack offered = new ItemStack(item, 64);
        inventory.setInventorySlotContents(0, new ItemStack(item, 60));
        assertEquals(64, LogisticsStock.insert(inventory, 0, offered, true));
        assertEquals(60, inventory.getStackInSlot(0).stackSize);
        assertNull(inventory.getStackInSlot(1));
        assertEquals(64, LogisticsStock.insert(inventory, 0, offered, false));
        assertEquals(124, LogisticsStock.count(inventory, offered));
        assertEquals(4, LogisticsStock.insert(inventory, 0, offered, false));
        assertEquals(0, LogisticsStock.insert(inventory, 0, offered, true));
        assertEquals(64, offered.stackSize);
    }

    @Test
    public void matchingIncludesNbtAndMetadata() {
        InventoryBasic inventory = new InventoryBasic("test", false, 2);
        ItemStack sample = new ItemStack(new Item(), 1);
        ItemStack tagged = sample.copy();
        tagged.setTagCompound(new NBTTagCompound());
        tagged.getTagCompound().setString("variant", "other");
        inventory.setInventorySlotContents(0, tagged);
        assertEquals(0, LogisticsStock.count(inventory, sample));
        assertEquals(64, LogisticsStock.insert(inventory, 0, new ItemStack(sample.getItem(), 64), true));
        assertEquals(1, inventory.getStackInSlot(0).stackSize);
    }

    @Test
    public void sidedRulesAndDuplicateSlotIndicesCannotInflateSimulation() {
        SidedInventory inventory = new SidedInventory();
        ItemStack sample = new ItemStack(new Item(), 64);
        inventory.setInventorySlotContents(0, new ItemStack(sample.getItem(), 60));
        assertEquals(0, LogisticsStock.insert(inventory, 1, sample, true));
        assertEquals(4, LogisticsStock.insert(inventory, 0, sample, true));
        assertEquals(4, LogisticsStock.insert(inventory, 0, sample, false));
        assertNull(inventory.getStackInSlot(1));
    }

    @Test
    public void automaticCollectionNeedsNoFilterButRespectsMachineOutputRules() {
        ItemStack stack = new ItemStack(new Item(), 12);
        SidedInventory machine = new SidedInventory();
        machine.setInventorySlotContents(0, stack);
        assertNull(LogisticsStock.extractable(machine, 0, 0));
        InventoryBasic chest = new InventoryBasic("chest", false, 1);
        chest.setInventorySlotContents(0, stack);
        ItemStack candidate = LogisticsStock.extractable(chest, 0, 0);
        assertEquals(12, candidate.stackSize);
        candidate.stackSize = 1;
        assertEquals(12, chest.getStackInSlot(0).stackSize);
    }

    @Test
    public void modeAndTargetsSurvivePortableSaveWithoutFilters() {
        NBTTagCompound saved = new NBTTagCompound();
        saved.setBoolean("logisticsImport", true);
        saved.setLong("items0", 128);
        saved.setLong("fluids0", 16000);
        TileFluxLogistics tile = new TileFluxLogistics();
        tile.readContents(saved);
        NBTTagCompound rewritten = new NBTTagCompound();
        tile.writeContents(rewritten);
        assertEquals(true, tile.importing());
        assertEquals(128, rewritten.getLong("items0"));
        assertEquals(16000, rewritten.getLong("fluids0"));
        assertNull(tile.itemFilters[0]);
    }

    @Test
    public void bulkOfferFillsEveryAvailableSlotWithoutTheOldSixteenStackCap() {
        InventoryBasic chest = new InventoryBasic("bulk", false, 54) {

            @Override
            public boolean isItemValidForSlot(int slot, ItemStack stack) {
                return stack.stackSize <= 64;
            }
        };
        ItemStack offer = new ItemStack(new Item(), Integer.MAX_VALUE);
        assertEquals(3456, LogisticsStock.insert(chest, 0, offer, true));
        assertEquals(3456, LogisticsStock.insert(chest, 0, offer, false));
        assertEquals(3456, LogisticsStock.count(chest, offer));
        assertEquals(0, LogisticsStock.insert(chest, 0, offer, false));
        assertEquals(Integer.MAX_VALUE, offer.stackSize);
    }

    @Test
    public void bulkCargoCountRestoresAfterVanillaByteTruncation() throws Exception {
        ItemStack cargo = new ItemStack(new Item(), 1_000_000);
        TileFluxLogistics tile = new TileFluxLogistics();
        var field = TileFluxLogistics.class.getDeclaredField("pendingItem");
        field.setAccessible(true);
        field.set(tile, cargo);
        NBTTagCompound saved = new NBTTagCompound();
        tile.writeContents(saved);
        assertEquals(1_000_000, saved.getInteger("pendingItemCount"));
        ItemStack decoded = cargo.copy();
        decoded.stackSize = saved.getCompoundTag("pendingItem").getByte("Count");
        assertEquals(1_000_000, LogisticsStock.restoreCargoCount(decoded, saved).stackSize);
        saved.setInteger("pendingItemCount", -1);
        assertNull(LogisticsStock.restoreCargoCount(decoded, saved));
    }

    /** Duplicate and invalid advertised slots model imperfect third-party inventories. */
    private static final class SidedInventory extends InventoryBasic implements ISidedInventory {

        SidedInventory() {
            super("sided", false, 2);
        }

        public int[] getAccessibleSlotsFromSide(int side) {
            return new int[] { 0, 0, -1, 100 };
        }

        public boolean canInsertItem(int slot, ItemStack stack, int side) {
            return side == 0;
        }

        public boolean canExtractItem(int slot, ItemStack stack, int side) {
            return false;
        }
    }
}
