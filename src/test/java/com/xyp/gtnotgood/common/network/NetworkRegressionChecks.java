package com.xyp.gtnotgood.common.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Headless regression checks for sided insertion, NBT matching and persisted channel configuration. */
public final class NetworkRegressionChecks {

    private static int checks;

    public static void main(String[] args) throws Exception {
        Item item = new Item();
        ItemStack offered = new ItemStack(item, 32);
        TestInventory inventory = new TestInventory();
        check(NetworkTransfer.insert(inventory, 1, offered, 32, false) == 0, "wrong side rejected");
        check(NetworkTransfer.insert(inventory, 2, offered, 32, true) == 16, "simulate respects slot capacity");
        check(inventory.stack == null && offered.stackSize == 32, "simulation leaves both inventories unchanged");
        check(NetworkTransfer.insert(inventory, 2, offered, 32, false) == 16, "actual insert respects capacity");
        check(inventory.stack.stackSize == 16 && offered.stackSize == 32, "caller owns offered stack");
        check(NetworkTransfer.insert(inventory, 2, offered, 32, false) == 0, "full destination rejected");
        inventory.stack.stackSize = 10;
        check(NetworkTransfer.insert(inventory, 2, offered, 2, false) == 2, "operation rate limit");
        check(inventory.stack.stackSize == 12, "merge preserves existing stack");
        ItemStack tagged = offered.copy();
        tagged.setTagCompound(new NBTTagCompound());
        tagged.getTagCompound()
            .setString("owner", "sample");
        check(NetworkTransfer.insert(inventory, 2, tagged, 4, false) == 0, "NBT mismatch cannot merge");
        inventory.allow = false;
        check(NetworkTransfer.insert(inventory, 2, offered, 4, false) == 0, "slot insertion policy respected");

        NetworkRule rule = new NetworkRule();
        rule.filters[0] = tagged;
        check(rule.accepts(tagged.copy()), "identical sample accepted");
        check(!rule.accepts(offered), "filter checks NBT");
        rule.matchNbt = false;
        check(rule.accepts(offered), "NBT matching can be disabled");
        rule.blacklist = true;
        check(!rule.accepts(offered), "blacklist rejects matching samples");
        rule.filters[0] = null;
        check(rule.accepts(offered), "empty filters do not block transfer");
        rule.filters[17] = new ItemStack(item, 1, 3);
        rule.facing = 5;
        NBTTagCompound copiedRule = rule.write();
        NetworkRule pastedRule = new NetworkRule();
        pastedRule.read(copiedRule);
        check(
            pastedRule.facing == 5 && pastedRule.blacklist == rule.blacklist,
            "node copy preserves access face and filter mode");
        pastedRule.facing = 2;
        check(rule.facing == 5, "pasted settings do not modify the original configuration");
        rule.blacklist = false;
        check(!rule.accepts(offered), "metadata matching applies to all eighteen slots");
        rule.matchMeta = false;
        check(rule.accepts(offered), "metadata matching can be disabled");
        NBTTagCompound invalid = new NBTTagCompound();
        invalid.setInteger("mode", 99);
        invalid.setInteger("rate", -100);
        invalid.setInteger("priority", 1000);
        rule.read(invalid);
        check(rule.mode == 2 && rule.rate == 1 && rule.priority == 99, "invalid NBT bounded");

        TileNetworkController.Channel channel = new TileNetworkController.Channel();
        channel.type = 1;
        channel.enabled = true;
        channel.interval = 40;
        channel.name = "Fluid routing";
        channel.rules.put("1:2:3:4", rule);
        channel.source = "4:5:6";
        TileNetworkController.Channel restored = new TileNetworkController.Channel();
        restored.read(channel.write());
        check(restored.type == 1 && restored.enabled && restored.interval == 40, "channel settings survive save");
        check(restored.rules.get("1:2:3:4").rate == 1, "endpoint rule survives save");
        check(restored.source.equals("4:5:6"), "source exclusion survives save");
        System.out.println("Network regression checks passed: " + checks);
        NetworkGuiSyncChecks.run();
        NetworkEnergyChecks.run();
        NetworkFilterSlot sampleSlot = new NetworkFilterSlot(new NetworkFilterHandler(null, () -> null, () -> null), 0);
        check(sampleSlot.isItemValid(offered), "phantom accepts cursor and dragged samples without real insertion");
        check(
            sampleSlot.getItemStackLimit(offered) == 1 && offered.stackSize == 32,
            "phantom validation preserves the real stack and limits samples to one");
        System.out.println("Network phantom slot checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }

    /** A hostile sided inventory exposes duplicate and invalid slots to exercise slot validation. */
    private static final class TestInventory implements ISidedInventory {

        ItemStack stack;
        boolean allow = true;

        public int[] getAccessibleSlotsFromSide(int side) {
            return new int[] { -1, 0, 0, 99 };
        }

        public boolean canInsertItem(int slot, ItemStack item, int side) {
            return side == 2;
        }

        public boolean canExtractItem(int slot, ItemStack item, int side) {
            return false;
        }

        public int getSizeInventory() {
            return 1;
        }

        public ItemStack getStackInSlot(int slot) {
            return stack;
        }

        public ItemStack decrStackSize(int slot, int amount) {
            throw new AssertionError("unexpected extraction");
        }

        public ItemStack getStackInSlotOnClosing(int slot) {
            return null;
        }

        public void setInventorySlotContents(int slot, ItemStack item) {
            stack = item;
        }

        public String getInventoryName() {
            return "test";
        }

        public boolean hasCustomInventoryName() {
            return false;
        }

        public int getInventoryStackLimit() {
            return 16;
        }

        public void markDirty() {}

        public boolean isUseableByPlayer(EntityPlayer player) {
            return true;
        }

        public void openInventory() {}

        public void closeInventory() {}

        public boolean isItemValidForSlot(int slot, ItemStack item) {
            return allow;
        }
    }
}
