package com.xyp.gtnotgood.common.machines.hatch.me;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidStack;

/**
 * Nine exact-identity ghost filters; an empty whitelist accepts everything without changing existing cached outputs.
 */
public final class MaxCapacityMEOutputFilters {

    public static final int SLOT_COUNT = 9;
    private static final String NBT_KEY = "gtngOutputFilters";
    private final ItemStack[] items = new ItemStack[SLOT_COUNT];
    private final FluidStack[] fluids = new FluidStack[SLOT_COUNT];
    private final Runnable changed;

    public MaxCapacityMEOutputFilters(Runnable changed) {
        this.changed = changed;
    }

    public ItemStack getItem(int slot) {
        return items[slot] == null ? null : items[slot].copy();
    }

    public FluidStack getFluid(int slot) {
        return fluids[slot] == null ? null : fluids[slot].copy();
    }

    public void setItem(int slot, ItemStack stack) {
        items[slot] = stack == null ? null : stack.copy();
        if (items[slot] != null) items[slot].stackSize = 1;
        changed.run();
    }

    public void setFluid(int slot, FluidStack stack) {
        fluids[slot] = stack == null ? null : stack.copy();
        if (fluids[slot] != null) fluids[slot].amount = 1;
        changed.run();
    }

    public boolean hasItems() {
        for (ItemStack item : items) if (item != null) return true;
        return false;
    }

    public boolean hasFluids() {
        for (FluidStack fluid : fluids) if (fluid != null) return true;
        return false;
    }

    /** Matches item, damage and NBT, ignoring count; no configured samples means unrestricted input. */
    public boolean accepts(ItemStack stack) {
        if (stack == null) return false;
        if (!hasItems()) return true;
        for (ItemStack item : items) {
            if (item != null && item.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(item, stack)) return true;
        }
        return false;
    }

    /** Matches fluid and NBT, ignoring amount; no configured samples means unrestricted input. */
    public boolean accepts(FluidStack stack) {
        if (stack == null) return false;
        if (!hasFluids()) return true;
        for (FluidStack fluid : fluids) if (fluid != null && fluid.isFluidEqual(stack)) return true;
        return false;
    }

    /**
     * Saves only configured samples in world data. Removing an empty tag normalizes the older nine-empty-entry format.
     *
     * @param tag world data to update; unrelated tags are preserved
     */
    public void save(NBTTagCompound tag) {
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (items[i] == null && fluids[i] == null) continue;
            NBTTagCompound entry = new NBTTagCompound();
            entry.setInteger("slot", i);
            if (items[i] != null) entry.setTag("item", items[i].writeToNBT(new NBTTagCompound()));
            if (fluids[i] != null) entry.setTag("fluid", fluids[i].writeToNBT(new NBTTagCompound()));
            list.appendTag(entry);
        }
        if (list.tagCount() == 0) tag.removeTag(NBT_KEY);
        else tag.setTag(NBT_KEY, list);
    }

    /**
     * Clears filter data on pickup without changing the placed machine or unrelated item tags.
     *
     * @param tag dropped-item data to normalize
     */
    static void clearItemData(NBTTagCompound tag) {
        tag.removeTag(NBT_KEY);
    }

    /** Loads old machines as unfiltered, ignores invalid indices, and never invokes world callbacks while loading. */
    public void load(NBTTagCompound tag) {
        java.util.Arrays.fill(items, null);
        java.util.Arrays.fill(fluids, null);
        NBTTagList list = tag.getTagList(NBT_KEY, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            int slot = entry.getInteger("slot");
            if (slot < 0 || slot >= SLOT_COUNT) continue;
            if (entry.hasKey("item")) items[slot] = ItemStack.loadItemStackFromNBT(entry.getCompoundTag("item"));
            if (entry.hasKey("fluid")) fluids[slot] = FluidStack.loadFluidStackFromNBT(entry.getCompoundTag("fluid"));
            if (items[slot] != null) items[slot].stackSize = 1;
            if (fluids[slot] != null) fluids[slot].amount = 1;
        }
    }
}
