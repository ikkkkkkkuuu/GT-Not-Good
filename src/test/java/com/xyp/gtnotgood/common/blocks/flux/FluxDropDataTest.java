package com.xyp.gtnotgood.common.blocks.flux;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

/** Regression coverage for connector setting resets without discarding bulk cargo or unknown tags. */
public class FluxDropDataTest {

    @Test
    public void defaultEnergyDropsMatchNewlyCraftedItems() {
        ItemStack crafted = new ItemStack(new Item());
        ItemStack dropped = crafted.copy();
        NBTTagCompound tag = new NBTTagCompound();
        new TileFluxPoint().writeSettings(tag);
        dropped.setTagCompound(tag);
        FluxDropData.normalize(dropped, false);
        assertFalse(dropped.hasTagCompound());
        assertTrue(ItemStack.areItemStackTagsEqual(crafted, dropped));
    }

    @Test
    public void defaultLogisticsDropsMatchNewlyCraftedItems() {
        ItemStack dropped = new ItemStack(new Item());
        NBTTagCompound tag = new NBTTagCompound();
        new TileFluxLogistics().writeContents(tag);
        dropped.setTagCompound(tag);
        FluxDropData.normalize(dropped, true);
        assertFalse(dropped.hasTagCompound());
    }

    @Test
    public void customEnergySettingsResetWithoutDiscardingUnknownTags() {
        ItemStack dropped = new ItemStack(new Item());
        NBTTagCompound tag = new NBTTagCompound();
        new TileFluxPlug().writeSettings(tag);
        tag.setLong("fluxVoltage", 2048);
        tag.setLong("fluxAmperage", 16);
        tag.setBoolean("fluxEnabled", false);
        tag.setLong("fluxLimit", 800000);
        tag.setBoolean("fluxDisableLimit", true);
        tag.setString("customExtension", "preserve");
        dropped.setTagCompound(tag);
        FluxDropData.normalize(dropped, false);
        TileFluxPlug reloaded = new TileFluxPlug();
        reloaded.readSettings(dropped.getTagCompound());
        assertEquals(32, reloaded.voltage());
        assertEquals(1, reloaded.amperage());
        assertTrue(reloaded.enabled());
        assertTrue(reloaded.connected());
        assertEquals("preserve", dropped.getTagCompound().getString("customExtension"));
        assertFalse(dropped.getTagCompound().hasKey("fluxVoltage"));
        assertFalse(dropped.getTagCompound().hasKey("fluxAmperage"));
        assertFalse(dropped.getTagCompound().hasKey("fluxLimit"));
    }

    @Test
    public void logisticsSettingsResetWhileCargoAndUnknownTagsRemainIntact() {
        ItemStack dropped = new ItemStack(new Item());
        NBTTagCompound tag = new NBTTagCompound();
        new TileFluxLogistics().writeContents(tag);
        tag.setString("logisticsChannel", "Production");
        tag.setLong("items0", 1000000);
        tag.setBoolean("logisticsImport", true);
        tag.setInteger("pendingItemCount", 1000000);
        NBTTagCompound cargo = new NBTTagCompound();
        cargo.setInteger("Amount", 2000000000);
        tag.setTag("pendingFluid", cargo);
        tag.setString("customExtension", "preserve");
        dropped.setTagCompound(tag);
        FluxDropData.normalize(dropped, true);
        NBTTagCompound result = dropped.getTagCompound();
        assertFalse(result.hasKey("logisticsChannel"));
        assertFalse(result.hasKey("items0"));
        assertFalse(result.hasKey("logisticsImport"));
        assertEquals(1000000, result.getInteger("pendingItemCount"));
        assertEquals(cargo, result.getCompoundTag("pendingFluid"));
        assertEquals("preserve", result.getString("customExtension"));
        NBTTagCompound before = (NBTTagCompound) result.copy();
        FluxDropData.normalize(dropped, true);
        assertEquals(before, dropped.getTagCompound());
    }
}
