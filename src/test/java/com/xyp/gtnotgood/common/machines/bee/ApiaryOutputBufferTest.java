package com.xyp.gtnotgood.common.machines.bee;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.BeforeClass;
import org.junit.Test;

public class ApiaryOutputBufferTest {

    private static final Item product = new Item().setHasSubtypes(true);
    private static final Item limitedProduct = new Item().setMaxStackSize(16);

    @BeforeClass
    public static void registerItems() throws ReflectiveOperationException {
        Method register = Item.itemRegistry.getClass().getDeclaredMethod("addObjectRaw", int.class, String.class,
            Object.class);
        register.setAccessible(true);
        int id = 32500;
        while (Item.itemRegistry.getObjectById(id) != null) id--;
        register.invoke(Item.itemRegistry, id, "test:apiary_output", product);
        while (Item.itemRegistry.getObjectById(id) != null) id--;
        register.invoke(Item.itemRegistry, id, "test:limited_apiary_output", limitedProduct);
    }

    @Test
    public void savesLongCountSeparatelyFromOneItemTemplate() {
        ApiaryOutputBuffer buffer = new ApiaryOutputBuffer();
        ItemStack stack = bee(7, "forestry.speciesForest");
        stack.stackSize = 37;
        long amount = (long) Integer.MAX_VALUE + 257;
        buffer.add(stack, amount);
        stack.getTagCompound().setString("Genome", "changed");

        NBTTagList saved = buffer.writeToNBT();
        assertEquals(1, saved.tagCount());
        assertEquals(amount, saved.getCompoundTagAt(0).getLong("Count"));
        assertEquals(1, saved.getCompoundTagAt(0).getCompoundTag("Item").getByte("Count"));
        ApiaryOutputBuffer restored = new ApiaryOutputBuffer();
        restored.readFromNBT(saved);
        assertEquals(amount, restored.getItemCount());
        InventoryBasic inventory = new InventoryBasic("apiary", false, 1);
        assertTrue(restored.flushToInventory(inventory, 0, 1));
        assertEquals(64, inventory.getStackInSlot(0).stackSize);
        assertEquals(7, inventory.getStackInSlot(0).getItemDamage());
        assertEquals("forestry.speciesForest", inventory.getStackInSlot(0).getTagCompound().getString("Genome"));
        assertEquals(amount - 64, restored.getItemCount());
    }

    @Test
    public void genesAndMetadataRemainSeparateWhileIdenticalProductsMerge() {
        ApiaryOutputBuffer buffer = new ApiaryOutputBuffer();
        buffer.add(bee(1, "forest"), 11);
        buffer.add(bee(1, "forest"), 13);
        buffer.add(bee(1, "meadows"), 17);
        buffer.add(bee(2, "forest"), 19);
        assertEquals(3, buffer.getTypeCount());
        assertEquals(60, buffer.getItemCount());
        InventoryBasic inventory = new InventoryBasic("apiary", false, 3);
        assertTrue(buffer.flushToInventory(inventory, 0, 3));
        assertEquals(24, inventory.getStackInSlot(0).stackSize);
        assertEquals("forest", inventory.getStackInSlot(0).getTagCompound().getString("Genome"));
        assertEquals(17, inventory.getStackInSlot(1).stackSize);
        assertEquals("meadows", inventory.getStackInSlot(1).getTagCompound().getString("Genome"));
        assertEquals(2, inventory.getStackInSlot(2).getItemDamage());
        assertEquals(19, inventory.getStackInSlot(2).stackSize);
        assertTrue(buffer.isEmpty());
    }

    @Test
    public void wildcardMetadataDoesNotMergeWithAnOrdinaryProduct() {
        ApiaryOutputBuffer buffer = new ApiaryOutputBuffer();
        buffer.add(bee(32767, "forest"), 11);
        buffer.add(bee(1, "forest"), 13);
        assertEquals(2, buffer.getTypeCount());
        assertEquals(24, buffer.getItemCount());
        InventoryBasic inventory = new InventoryBasic("apiary", false, 2);
        assertTrue(buffer.flushToInventory(inventory, 0, 2));
        assertEquals(32767, inventory.getStackInSlot(0).getItemDamage());
        assertEquals(11, inventory.getStackInSlot(0).stackSize);
        assertEquals(1, inventory.getStackInSlot(1).getItemDamage());
        assertEquals(13, inventory.getStackInSlot(1).stackSize);
        assertTrue(buffer.isEmpty());
    }

    @Test
    public void blockedOutputsResumeWithExactRemainderWhenSpaceBecomesAvailable() {
        ApiaryOutputBuffer buffer = new ApiaryOutputBuffer();
        buffer.add(new ItemStack(product), 100);
        InventoryBasic inventory = new InventoryBasic("apiary", false, 3);
        for (int slot = 0; slot < 3; slot++) inventory.setInventorySlotContents(slot, new ItemStack(product, 64));
        assertFalse(buffer.flushToInventory(inventory, 1, 1));
        assertEquals(100, buffer.getItemCount());
        inventory.getStackInSlot(1).stackSize = 61;
        assertTrue(buffer.flushToInventory(inventory, 1, 1));
        assertEquals(64, inventory.getStackInSlot(1).stackSize);
        assertEquals(97, buffer.getItemCount());
        inventory.setInventorySlotContents(1, null);
        assertTrue(buffer.flushToInventory(inventory, 1, 1));
        assertEquals(64, inventory.getStackInSlot(1).stackSize);
        assertEquals(33, buffer.getItemCount());
        inventory.setInventorySlotContents(1, null);
        assertTrue(buffer.flushToInventory(inventory, 1, 1));
        assertEquals(33, inventory.getStackInSlot(1).stackSize);
        assertEquals(64, inventory.getStackInSlot(0).stackSize);
        assertEquals(64, inventory.getStackInSlot(2).stackSize);
        assertTrue(buffer.isEmpty());
        assertFalse(buffer.flushToInventory(inventory, 1, 1));
    }

    @Test
    public void outputUsesBothInventoryAndItemStackLimits() {
        ApiaryOutputBuffer buffer = new ApiaryOutputBuffer();
        buffer.add(new ItemStack(limitedProduct), 37);
        InventoryBasic inventory = new InventoryBasic("apiary", false, 4) {

            @Override
            public int getInventoryStackLimit() {
                return 10;
            }
        };
        assertTrue(buffer.flushToInventory(inventory, 0, 4));
        assertEquals(10, inventory.getStackInSlot(0).stackSize);
        assertEquals(10, inventory.getStackInSlot(1).stackSize);
        assertEquals(10, inventory.getStackInSlot(2).stackSize);
        assertEquals(7, inventory.getStackInSlot(3).stackSize);
        assertTrue(buffer.isEmpty());

        buffer.add(new ItemStack(limitedProduct), 37);
        inventory = new InventoryBasic("apiary", false, 3);
        assertTrue(buffer.flushToInventory(inventory, 0, 3));
        assertEquals(16, inventory.getStackInSlot(0).stackSize);
        assertEquals(16, inventory.getStackInSlot(1).stackSize);
        assertEquals(5, inventory.getStackInSlot(2).stackSize);
        assertTrue(buffer.isEmpty());
    }

    @Test
    public void enormousProductionStaysBufferedBeyondTheVisibleInventory() {
        ApiaryOutputBuffer buffer = new ApiaryOutputBuffer();
        long amount = (long) Integer.MAX_VALUE + 10_000;
        buffer.add(new ItemStack(product), amount);
        InventoryBasic inventory = new InventoryBasic("apiary", false, 54);
        assertTrue(buffer.flushToInventory(inventory, 0, 54));
        for (int slot = 0; slot < 54; slot++) assertEquals(64, inventory.getStackInSlot(slot).stackSize);
        assertEquals(amount - 54 * 64, buffer.getItemCount());
        assertFalse(buffer.flushToInventory(inventory, 0, 54));
        ApiaryOutputBuffer restored = new ApiaryOutputBuffer();
        restored.readFromNBT(buffer.writeToNBT());
        assertEquals(buffer.getItemCount(), restored.getItemCount());
    }

    @Test
    public void mergeTransfersOwnershipAndClearsTheSource() {
        ApiaryOutputBuffer target = new ApiaryOutputBuffer();
        target.add(bee(0, "forest"), 6);
        ApiaryOutputBuffer source = new ApiaryOutputBuffer();
        source.add(bee(0, "forest"), 9);
        source.add(bee(0, "meadows"), 23);
        target.mergeFrom(source);
        assertTrue(source.isEmpty());
        assertEquals(0, source.getItemCount());
        assertEquals(0, source.getTypeCount());
        assertEquals(38, target.getItemCount());
        assertEquals(2, target.getTypeCount());
        target.mergeFrom(target);
        assertEquals(38, target.getItemCount());
        InventoryBasic inventory = new InventoryBasic("apiary", false, 2);
        target.flushToInventory(inventory, 0, 2);
        assertEquals(15, inventory.getStackInSlot(0).stackSize);
        assertEquals(23, inventory.getStackInSlot(1).stackSize);
        source.add(inventory.getStackInSlot(0), 1);
        inventory.getStackInSlot(0).getTagCompound().setString("Genome", "changed");
        InventoryBasic copy = new InventoryBasic("apiary", false, 1);
        source.flushToInventory(copy, 0, 1);
        assertNotSame(inventory.getStackInSlot(0).getTagCompound(), copy.getStackInSlot(0).getTagCompound());
        assertEquals("forest", copy.getStackInSlot(0).getTagCompound().getString("Genome"));
    }

    @Test
    public void overflowRejectsTheWholeTransferWithoutLosingEitherBuffer() {
        ApiaryOutputBuffer target = new ApiaryOutputBuffer();
        target.add(bee(0, "forest"), Long.MAX_VALUE - 1);
        assertThrows(ArithmeticException.class, () -> target.add(bee(0, "forest"), 2));
        assertEquals(Long.MAX_VALUE - 1, target.getItemCount());
        ApiaryOutputBuffer source = new ApiaryOutputBuffer();
        source.add(bee(0, "meadows"), 2);
        assertThrows(ArithmeticException.class, () -> target.mergeFrom(source));
        assertEquals(Long.MAX_VALUE - 1, target.getItemCount());
        assertEquals(1, target.getTypeCount());
        assertEquals(2, source.getItemCount());
        assertFalse(source.isEmpty());
    }

    private static ItemStack bee(int metadata, String genome) {
        ItemStack stack = new ItemStack(product, 1, metadata);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Genome", genome);
        stack.setTagCompound(tag);
        return stack;
    }
}
