package com.xyp.gtnotgood.common.compat;

import static org.junit.Assert.*;

import java.util.Random;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.WeightedRandomChestContent;

import org.junit.Test;

/** Checks that maximum rewards preserve custom generation, NBT, stack limits, and distinct chest slots. */
public class LootGamesRewardsTest {

    @Test
    public void fillsEverySlotWithoutMutatingLootTemplate() {
        ItemStack template = new ItemStack(new Item(), 2, 7);
        template.setTagCompound(new NBTTagCompound());
        template.getTagCompound().setString("reward", "original");
        InventoryBasic chest = new InventoryBasic("", false, 27);
        LootGamesMaximumRewards.fillChest(
            new Random(1), new WeightedRandomChestContent[] { new WeightedRandomChestContent(template, 2, 2, 1) }, chest);
        for (int slot = 0; slot < 27; slot++) {
            ItemStack reward = chest.getStackInSlot(slot);
            assertNotNull(reward);
            assertEquals(64, reward.stackSize);
            assertEquals(7, reward.getItemDamage());
            assertEquals("original", reward.getTagCompound().getString("reward"));
            assertNotSame(template.getTagCompound(), reward.getTagCompound());
            if (slot > 0) assertNotSame(chest.getStackInSlot(slot - 1), reward);
        }
        assertEquals(2, template.stackSize);
    }

    @Test
    public void honorsCustomGenerationAndUnstackableItems() {
        ItemStack template = new ItemStack(new Item());
        ItemStack custom = new ItemStack(new Item().setMaxStackSize(1));
        int[] calls = { 0 };
        WeightedRandomChestContent entry = new WeightedRandomChestContent(template, 1, 1, 1) {
            @Override
            protected ItemStack[] generateChestContent(Random random, IInventory inventory) {
                calls[0]++;
                return new ItemStack[] { custom.copy() };
            }
        };
        InventoryBasic chest = new InventoryBasic("", false, 27);
        LootGamesMaximumRewards.fillChest(new Random(2), new WeightedRandomChestContent[] { entry }, chest);
        assertEquals(27, calls[0]);
        for (int slot = 0; slot < 27; slot++) {
            assertSame(custom.getItem(), chest.getStackInSlot(slot).getItem());
            assertEquals(1, chest.getStackInSlot(slot).stackSize);
        }
    }

    @Test
    public void respectsContainerLimitAndEmptyLoot() {
        InventoryBasic chest = new InventoryBasic("", false, 27) {
            @Override
            public int getInventoryStackLimit() {
                return 16;
            }
        };
        LootGamesMaximumRewards.fillChest(new Random(3), new WeightedRandomChestContent[0], chest);
        assertNull(chest.getStackInSlot(0));
        WeightedRandomChestContent entry = new WeightedRandomChestContent(new ItemStack(new Item()), 1, 1, 1);
        LootGamesMaximumRewards.fillChest(new Random(3), new WeightedRandomChestContent[] { entry }, chest);
        for (int slot = 0; slot < 27; slot++) assertEquals(16, chest.getStackInSlot(slot).stackSize);
    }
}
