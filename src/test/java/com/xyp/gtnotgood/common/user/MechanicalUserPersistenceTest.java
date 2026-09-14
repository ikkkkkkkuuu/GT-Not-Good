package com.xyp.gtnotgood.common.user;

import static org.junit.Assert.*;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.BeforeClass;
import org.junit.Test;

/** Regression coverage for tool NBT, slot extraction and corrupt mode persistence. */
public class MechanicalUserPersistenceTest {

    @BeforeClass
    public static void registerTile() {

        cpw.mods.fml.common.registry.GameRegistry.registerTileEntity(TileMechanicalUser.class, "test_mechanical_user");
    }

    @Test
    public void toolDataIsSerializedAndModesSurviveReload() {
        TileMechanicalUser tile = new TileMechanicalUser();
        ItemStack tool = new ItemStack(new net.minecraft.item.Item().setMaxDamage(100), 1, 17);
        tool.setTagCompound(new NBTTagCompound());
        tool.getTagCompound()
            .setString("Custom", "retained");
        tile.setInventorySlotContents(7, tool);
        tile.mode = 5;
        tile.leftClick = true;
        tile.firstSlotOnly = true;
        NBTTagCompound tag = new NBTTagCompound();
        tile.writeToNBT(tag);
        TileMechanicalUser loaded = new TileMechanicalUser();
        loaded.readFromNBT(tag);
        assertEquals(
            17,
            tag.getTagList("Items", 10)
                .getCompoundTagAt(0)
                .getShort("Damage"));
        assertEquals(5, loaded.mode);
        assertTrue(loaded.leftClick);
        assertTrue(loaded.firstSlotOnly);
    }

    @Test
    public void extractionDoesNotDuplicateAndInvalidModeIsClamped() {
        TileMechanicalUser tile = new TileMechanicalUser();
        tile.setInventorySlotContents(0, new ItemStack(new net.minecraft.item.Item(), 3));
        assertEquals(2, tile.decrStackSize(0, 2).stackSize);
        assertEquals(1, tile.getStackInSlotOnClosing(0).stackSize);
        assertNull(tile.getStackInSlot(0));
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Mode", Integer.MAX_VALUE);
        tile.readFromNBT(tag);
        assertEquals(5, tile.mode);
        assertEquals(9, tile.getAccessibleSlotsFromSide(0).length);
        assertFalse(tile.isItemValidForSlot(9, new ItemStack(new net.minecraft.item.Item())));
    }
}
