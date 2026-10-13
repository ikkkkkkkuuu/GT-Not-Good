// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure PR #627 at 651396c75d108e6ff5399a44c56c12214ad27d3d.

package com.xyp.gtnotgood.common.wirelessfrequency;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.item.ItemStackNBT;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessChannelManager.Address;

@Desugar
public record WirelessCardBinding(Address source, UUID owner, String ownerName) {

    private static final String KEY = "GTNLWirelessCard";
    private static final int VERSION = 1;
    private static final String AUTO_KEY = "GTNLWirelessAutoConnect";
    private static final String GROUP_KEY = "GTNLWirelessCollapseTypes";
    private static final String SOURCES_KEY = "GTNGWirelessSources";
    public static final int MAX_SOURCES = 64;

    public static boolean automatic(NBTTagCompound itemTag) {
        return itemTag != null && itemTag.getBoolean(AUTO_KEY);
    }

    public static boolean toggleAutomatic(ItemStack card) {
        boolean enabled = ItemStackNBT.invertBoolean(card, AUTO_KEY);
        rememberCurrent(ItemStackNBT.get(card));
        return enabled;
    }

    static boolean collapseTypes(NBTTagCompound itemTag) {
        return itemTag == null || !itemTag.hasKey(GROUP_KEY, 1) || itemTag.getBoolean(GROUP_KEY);
    }

    static boolean toggleGrouping(ItemStack card) {
        boolean collapse = !collapseTypes(card.getTagCompound());
        ItemStackNBT.setBoolean(card, GROUP_KEY, collapse);
        return collapse;
    }

    public static WirelessCardBinding read(NBTTagCompound itemTag) {
        if (itemTag == null || !itemTag.hasKey(KEY, 10)) return null;
        NBTTagCompound data = itemTag.getCompoundTag(KEY);
        if (
            !data.hasKey("version", 3) || data.getInteger("version") != VERSION
                || !data.hasKey("dimension", 3)
                || !data.hasKey("x", 3)
                || !data.hasKey("y", 3)
                || !data.hasKey("z", 3)
                || !data.hasKey("owner", 8)
        ) return null;
        try {
            return new WirelessCardBinding(
                new Address(data.getInteger("dimension"), data.getInteger("x"), data.getInteger("y"),
                    data.getInteger("z"), ForgeDirection.UNKNOWN),
                UUID.fromString(data.getString("owner")), data.getString("ownerName"));
        } catch (IllegalArgumentException invalidOwner) {
            return null;
        }
    }

    public void write(NBTTagCompound itemTag) {
        rememberCurrent(itemTag);
        NBTTagList entries = itemTag.getTagList(SOURCES_KEY, 10);
        int index = indexOf(entries, this);
        if (index < 0 && entries.tagCount() >= MAX_SOURCES) throw new IllegalStateException("Source limit reached");
        boolean automatic = index >= 0 && entries.getCompoundTagAt(index).getBoolean("automatic");
        NBTTagCompound data = encode();
        if (index >= 0) data.setString("name", entries.getCompoundTagAt(index).getString("name"));
        data.setBoolean("automatic", automatic);
        if (index < 0) entries.appendTag(data);
        else entries.func_150304_a(index, data);
        itemTag.setTag(SOURCES_KEY, entries);
        itemTag.setTag(KEY, encode());
        itemTag.setBoolean(AUTO_KEY, automatic);
    }

    private NBTTagCompound encode() {
        NBTTagCompound data = new NBTTagCompound();
        data.setInteger("version", VERSION);
        data.setInteger("dimension", source.dimension());
        data.setInteger("x", source.x());
        data.setInteger("y", source.y());
        data.setInteger("z", source.z());
        data.setString("owner", owner.toString());
        data.setString("ownerName", ownerName);
        return data;
    }

    public static List<WirelessCardBinding> sources(NBTTagCompound itemTag) {
        List<WirelessCardBinding> result = new ArrayList<>();
        WirelessCardBinding current = read(itemTag);
        if (current == null) return result;
        NBTTagList entries = itemTag.getTagList(SOURCES_KEY, 10);
        for (int i = 0; i < Math.min(entries.tagCount(), MAX_SOURCES); i++) {
            NBTTagCompound wrapper = new NBTTagCompound();
            wrapper.setTag(KEY, entries.getCompoundTagAt(i));
            WirelessCardBinding binding = read(wrapper);
            if (binding != null && binding.owner().equals(current.owner()) && !result.contains(binding))
                result.add(binding);
        }
        if (result.isEmpty()) result.add(current);
        return result;
    }

    public static boolean select(NBTTagCompound itemTag, int index) {
        List<WirelessCardBinding> saved = sources(itemTag);
        if (index < 0 || index >= saved.size()) return false;
        saved.get(index).write(itemTag);
        return true;
    }

    public static String sourceName(NBTTagCompound itemTag, WirelessCardBinding binding) {
        if (itemTag == null || binding == null) return "";
        NBTTagList entries = itemTag.getTagList(SOURCES_KEY, 10);
        int index = indexOf(entries, binding);
        return index < 0 ? "" : entries.getCompoundTagAt(index).getString("name");
    }

    public static boolean renameSource(NBTTagCompound itemTag, int index, String name) {
        if (name == null || name.length() > 32) return false;
        for (int i = 0; i < name.length(); i++)
            if (Character.isISOControl(name.charAt(i)) || name.charAt(i) == '\u00a7') return false;
        List<WirelessCardBinding> saved = sources(itemTag);
        if (index < 0 || index >= saved.size()) return false;
        rememberCurrent(itemTag);
        NBTTagList entries = itemTag.getTagList(SOURCES_KEY, 10);
        int storedIndex = indexOf(entries, saved.get(index));
        if (storedIndex < 0) return false;
        entries.getCompoundTagAt(storedIndex).setString("name", name.trim());
        return true;
    }

    private static int indexOf(NBTTagList entries, WirelessCardBinding binding) {
        for (int i = 0; i < entries.tagCount(); i++) {
            NBTTagCompound data = entries.getCompoundTagAt(i);
            if (
                data.getInteger("dimension") == binding.source().dimension()
                    && data.getInteger("x") == binding.source().x()
                    && data.getInteger("y") == binding.source().y()
                    && data.getInteger("z") == binding.source().z()
                    && data.getString("owner").equals(binding.owner().toString())
            ) return i;
        }
        return -1;
    }

    private static void rememberCurrent(NBTTagCompound itemTag) {
        WirelessCardBinding current = read(itemTag);
        if (current == null) return;
        NBTTagList entries = itemTag.getTagList(SOURCES_KEY, 10);
        int index = indexOf(entries, current);
        NBTTagCompound data = current.encode();
        if (index >= 0) data.setString("name", entries.getCompoundTagAt(index).getString("name"));
        data.setBoolean("automatic", automatic(itemTag));
        if (index >= 0) entries.func_150304_a(index, data);
        else if (entries.tagCount() < MAX_SOURCES) entries.appendTag(data);
        itemTag.setTag(SOURCES_KEY, entries);
    }

    public static void clear(NBTTagCompound itemTag) {
        if (itemTag == null) return;
        rememberCurrent(itemTag);
        WirelessCardBinding current = read(itemTag);
        NBTTagList entries = itemTag.getTagList(SOURCES_KEY, 10);
        int index = current == null ? -1 : indexOf(entries, current);
        if (index >= 0) entries.removeTag(index);
        itemTag.removeTag(KEY);
        itemTag.setBoolean(AUTO_KEY, false);
        if (entries.tagCount() > 0) {
            NBTTagCompound next = entries.getCompoundTagAt(0);
            itemTag.setTag(KEY, next.copy());
            itemTag.setBoolean(AUTO_KEY, next.getBoolean("automatic"));
        }
    }

    public static boolean removeSource(NBTTagCompound itemTag, int index) {
        List<WirelessCardBinding> saved = sources(itemTag);
        if (index < 0 || index >= saved.size()) return false;
        WirelessCardBinding removed = saved.get(index);
        if (removed.equals(read(itemTag))) clear(itemTag);
        else {
            rememberCurrent(itemTag);
            NBTTagList entries = itemTag.getTagList(SOURCES_KEY, 10);
            int storedIndex = indexOf(entries, removed);
            if (storedIndex < 0) return false;
            entries.removeTag(storedIndex);
        }
        return true;
    }

    public boolean belongsTo(UUID player) {
        return owner.equals(player);
    }
}
