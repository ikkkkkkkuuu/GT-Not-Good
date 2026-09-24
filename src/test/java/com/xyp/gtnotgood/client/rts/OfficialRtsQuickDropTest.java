package com.xyp.gtnotgood.client.rts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collections;

import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.Test;

import com.rtsbuilding.rtsbuilding.platform.storage.IItemHandler;
import com.rtsbuilding.rtsbuilding.platform.storage.InventoryItemHandler;
import com.rtsbuilding.rtsbuilding.server.service.transfer.RtsTransferExtractor;

/** Regression coverage for nullable extraction results in the RTS Q quick-drop path. */
public class OfficialRtsQuickDropTest {

    @Test
    public void missingSourcesReturnEmpty() {
        assertNull(RtsTransferExtractor.extractMatchingFromQuickDropSources(null, null, new Item(), 8));
    }

    @Test
    public void emptyLinkedInventoryReturnsEmpty() {
        IItemHandler handler = new InventoryItemHandler(new InventoryBasic("test", false, 2));
        assertNull(
            RtsTransferExtractor
                .extractMatchingFromQuickDropSources(Collections.singletonList(handler), null, new Item(), 8));
    }

    @Test
    public void partialLinkedExtractionSurvivesEmptyFallback() {
        Item item = new Item();
        InventoryBasic inventory = new InventoryBasic("test", false, 2);
        inventory.setInventorySlotContents(0, new ItemStack(item, 3, 14));
        ItemStack result = RtsTransferExtractor.extractMatchingFromQuickDropSources(
            Collections.singletonList(new InventoryItemHandler(inventory)),
            null,
            item,
            8);
        assertEquals(3, result.stackSize);
        assertEquals(14, result.getItemDamage());
        assertNull(inventory.getStackInSlot(0));
    }

    @Test
    public void extractionRespectsRequestedLimit() {
        Item item = new Item();
        InventoryBasic inventory = new InventoryBasic("test", false, 2);
        inventory.setInventorySlotContents(0, new ItemStack(item, 9));
        ItemStack result = RtsTransferExtractor.extractMatchingFromQuickDropSources(
            Collections.singletonList(new InventoryItemHandler(inventory)),
            null,
            item,
            4);
        assertEquals(4, result.stackSize);
        assertEquals(5, inventory.getStackInSlot(0).stackSize);
    }

    @Test
    public void networkExtractionAlsoAcceptsMissingSources() {
        assertNull(RtsTransferExtractor.extractMatchingFromNetwork(null, null, new Item(), 8));
    }
}
