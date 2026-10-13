package com.xyp.gtnotgood.common.wirelessfrequency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Test;

import com.xyp.gtnotgood.common.wirelessfrequency.WirelessChannelManager.Address;

public class WirelessCardBindingTest {

    @Test
    public void bindingRoundTripKeepsSourceAndOwner() {
        UUID owner = UUID.randomUUID();
        WirelessCardBinding binding = new WirelessCardBinding(new Address(-3, 12, 64, -28, ForgeDirection.UNKNOWN),
            owner, "Owner");
        NBTTagCompound tag = new NBTTagCompound();
        binding.write(tag);

        assertEquals(binding, WirelessCardBinding.read(tag));
        assertTrue(WirelessCardBinding.read(tag).belongsTo(owner));
        assertFalse(WirelessCardBinding.read(tag).belongsTo(UUID.randomUUID()));
    }

    @Test
    public void invalidBindingIsIgnoredAndClearingDisablesAutomatic() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound malformed = new NBTTagCompound();
        malformed.setInteger("version", 1);
        malformed.setInteger("dimension", 0);
        malformed.setInteger("x", 0);
        malformed.setInteger("y", 64);
        malformed.setInteger("z", 0);
        malformed.setString("owner", "invalid UUID");
        tag.setTag("GTNLWirelessCard", malformed);
        tag.setBoolean("GTNLWirelessAutoConnect", true);

        assertNull(WirelessCardBinding.read(tag));
        WirelessCardBinding.clear(tag);
        assertFalse(WirelessCardBinding.automatic(tag));
        assertFalse(tag.hasKey("GTNLWirelessCard"));
    }

    @Test
    public void legacyBindingMigratesAndSourcesKeepSeparateAutomaticSettings() {
        UUID owner = UUID.randomUUID();
        var first = new WirelessCardBinding(new Address(0, 1, 64, 1, ForgeDirection.UNKNOWN), owner, "Owner");
        var second = new WirelessCardBinding(new Address(-1, 2, 64, 2, ForgeDirection.UNKNOWN), owner, "Owner");
        NBTTagCompound tag = new NBTTagCompound();
        first.write(tag);
        tag.removeTag("GTNGWirelessSources");
        tag.setBoolean("GTNLWirelessAutoConnect", true);
        second.write(tag);
        assertEquals(2, WirelessCardBinding.sources(tag).size());
        assertFalse(WirelessCardBinding.automatic(tag));
        assertTrue(WirelessCardBinding.select(tag, 0));
        assertEquals(first, WirelessCardBinding.read(tag));
        assertTrue(WirelessCardBinding.automatic(tag));
        first.write(tag);
        assertEquals(2, WirelessCardBinding.sources(tag).size());
        assertFalse(WirelessCardBinding.select(tag, 2));
        assertEquals(first, WirelessCardBinding.read(tag));
        WirelessCardBinding.clear(tag);
        assertEquals(second, WirelessCardBinding.read(tag));
        assertEquals(1, WirelessCardBinding.sources(tag).size());
        assertFalse(WirelessCardBinding.automatic(tag));
        WirelessCardBinding.clear(tag);
        assertNull(WirelessCardBinding.read(tag));
        assertTrue(WirelessCardBinding.sources(tag).isEmpty());
    }

    @Test
    public void sourceNamesSurviveSwitchingRebindingAndReloading() {
        UUID owner = UUID.randomUUID();
        var first = new WirelessCardBinding(new Address(0, 1, 64, 1, ForgeDirection.UNKNOWN), owner, "Owner");
        var second = new WirelessCardBinding(new Address(-1, 2, 64, 2, ForgeDirection.UNKNOWN), owner, "Owner");
        NBTTagCompound tag = new NBTTagCompound();
        first.write(tag);
        tag.removeTag("GTNGWirelessSources");
        assertTrue(WirelessCardBinding.renameSource(tag, 0, "  生产网络  "));
        second.write(tag);
        assertTrue(WirelessCardBinding.renameSource(tag, 1, "仓储网络"));
        first.write(tag);
        tag.setBoolean("GTNLWirelessAutoConnect", true);
        second.write(tag);
        tag = (NBTTagCompound) tag.copy();
        assertEquals("生产网络", WirelessCardBinding.sourceName(tag, first));
        assertEquals("仓储网络", WirelessCardBinding.sourceName(tag, second));
        assertEquals(second, WirelessCardBinding.read(tag));
        assertFalse(WirelessCardBinding.renameSource(tag, 0, "invalid\nname"));
        assertFalse(WirelessCardBinding.renameSource(tag, 2, "missing"));
        assertTrue(WirelessCardBinding.renameSource(tag, 0, " "));
        assertEquals("", WirelessCardBinding.sourceName(tag, first));
        assertEquals("仓储网络", WirelessCardBinding.sourceName(tag, second));
    }

    @Test
    public void deletingSourcesPreservesOtherNamesAndCurrentPreferences() {
        UUID owner = UUID.randomUUID();
        var first = new WirelessCardBinding(new Address(0, 1, 64, 1, ForgeDirection.UNKNOWN), owner, "Owner");
        var second = new WirelessCardBinding(new Address(0, 2, 64, 2, ForgeDirection.UNKNOWN), owner, "Owner");
        NBTTagCompound tag = new NBTTagCompound();
        first.write(tag);
        second.write(tag);
        WirelessCardBinding.renameSource(tag, 1, "保留的网络");
        tag.setBoolean("GTNLWirelessAutoConnect", true);
        assertFalse(WirelessCardBinding.removeSource(tag, -1));
        assertFalse(WirelessCardBinding.removeSource(tag, 2));
        assertTrue(WirelessCardBinding.removeSource(tag, 0));
        assertEquals(second, WirelessCardBinding.read(tag));
        assertTrue(WirelessCardBinding.automatic(tag));
        assertEquals("保留的网络", WirelessCardBinding.sourceName(tag, second));
        first.write(tag);
        assertTrue(WirelessCardBinding.removeSource(tag, 1));
        assertEquals(second, WirelessCardBinding.read(tag));
        assertTrue(WirelessCardBinding.automatic(tag));
        assertTrue(WirelessCardBinding.removeSource(tag, 0));
        assertNull(WirelessCardBinding.read(tag));
        assertFalse(WirelessCardBinding.automatic(tag));
        assertTrue(WirelessCardBinding.sources(tag).isEmpty());
    }
}
