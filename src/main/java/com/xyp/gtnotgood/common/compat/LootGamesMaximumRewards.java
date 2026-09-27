package com.xyp.gtnotgood.common.compat;

import java.util.Random;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandomChestContent;

/** Slot-by-slot loot generation confined to LootGames reward chests. */
public final class LootGamesMaximumRewards {

    private LootGamesMaximumRewards() {}

    /**
     * Uses native weighted generation, including Forge custom loot hooks, then maximizes each resulting stack.
     * A one-slot buffer prevents native random slot selection from overwriting previous chest rewards.
     * Empty results from custom loot hooks remain empty; no replacement items are invented.
     *
     * @param random native loot RNG
     * @param loot   non-empty configured loot table
     * @param chest  new reward inventory to fill
     */
    public static void fillChest(Random random, WeightedRandomChestContent[] loot, IInventory chest) {
        if (loot.length == 0) return;
        InventoryBasic buffer = new InventoryBasic("", false, 1);
        for (int slot = 0; slot < chest.getSizeInventory(); slot++) {
            buffer.setInventorySlotContents(0, null);
            WeightedRandomChestContent.generateChestContents(random, loot, buffer, 1);
            ItemStack generated = buffer.getStackInSlot(0);
            if (generated == null) continue;
            ItemStack reward = generated.copy();
            reward.stackSize = Math.min(reward.getMaxStackSize(), chest.getInventoryStackLimit());
            chest.setInventorySlotContents(slot, reward);
        }
        chest.markDirty();
    }
}
