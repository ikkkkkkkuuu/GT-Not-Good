package com.xyp.gtnotgood.common.machines.bee;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public final class ApiaryOutputBuffer {

    private final List<Entry> entries = new ArrayList<>();
    private long itemCount;

    public void add(ItemStack stack, long count) {
        if (count < 0) throw new IllegalArgumentException("Negative apiary output count");
        if (count == 0) return;
        if (stack == null || stack.getItem() == null) throw new IllegalArgumentException("Missing apiary output item");
        long total = Math.addExact(itemCount, count);
        for (Entry entry : entries) {
            if (matches(entry.template, stack)) {
                entry.count = Math.addExact(entry.count, count);
                itemCount = total;
                return;
            }
        }
        ItemStack template = stack.copy();
        template.stackSize = 1;
        entries.add(new Entry(template, count));
        itemCount = total;
    }

    public long getItemCount() {
        return itemCount;
    }

    public int getTypeCount() {
        return entries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public void clear() {
        entries.clear();
        itemCount = 0;
    }

    public NBTTagList writeToNBT() {
        NBTTagList saved = new NBTTagList();
        for (Entry entry : entries) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag("Item", entry.template.copy().writeToNBT(new NBTTagCompound()));
            tag.setLong("Count", entry.count);
            saved.appendTag(tag);
        }
        return saved;
    }

    public void readFromNBT(NBTTagList saved) {
        ApiaryOutputBuffer restored = new ApiaryOutputBuffer();
        for (int i = 0; i < saved.tagCount(); i++) {
            NBTTagCompound tag = saved.getCompoundTagAt(i);
            long count = tag.getLong("Count");
            if (count <= 0) continue;
            restored.add(ItemStack.loadItemStackFromNBT(tag.getCompoundTag("Item")), count);
        }
        clear();
        mergeFrom(restored);
    }

    public void mergeFrom(ApiaryOutputBuffer other) {
        if (other == this) return;
        Math.addExact(itemCount, other.itemCount);
        for (Entry entry : other.entries) add(entry.template, entry.count);
        other.clear();
    }

    /** Internal production writes output slots without applying external insertion restrictions. */
    public boolean flushToInventory(IInventory inventory, int firstSlot, int slotCount) {
        if (firstSlot < 0 || slotCount < 0 || firstSlot > inventory.getSizeInventory() - slotCount)
            throw new IndexOutOfBoundsException("Invalid apiary output slots");
        boolean changed = false;
        for (Iterator<Entry> iterator = entries.iterator(); iterator.hasNext();) {
            Entry entry = iterator.next();
            int limit = Math.min(inventory.getInventoryStackLimit(), entry.template.getMaxStackSize());
            if (limit <= 0) continue;
            for (int pass = 0; pass < 2 && entry.count > 0; pass++) {
                for (int slot = firstSlot; slot < firstSlot + slotCount && entry.count > 0; slot++) {
                    ItemStack existing = inventory.getStackInSlot(slot);
                    boolean empty = existing == null || existing.stackSize <= 0;
                    if (empty != (pass == 1)) continue;
                    if (!empty && !matches(existing, entry.template)) continue;
                    int stored = empty ? 0 : existing.stackSize;
                    if (stored >= limit) continue;
                    int moved = (int) Math.min(entry.count, limit - stored);
                    ItemStack output = empty ? entry.template.copy() : existing.copy();
                    output.stackSize = stored + moved;
                    inventory.setInventorySlotContents(slot, output);
                    entry.count -= moved;
                    itemCount -= moved;
                    changed = true;
                }
            }
            if (entry.count == 0) iterator.remove();
        }
        if (changed) inventory.markDirty();
        return changed;
    }

    private static boolean matches(ItemStack first, ItemStack second) {
        return first.getItem() == second.getItem() && first.getItemDamage() == second.getItemDamage()
            && ItemStack.areItemStackTagsEqual(first, second);
    }

    private static final class Entry {

        private final ItemStack template;
        private long count;

        private Entry(ItemStack template, long count) {
            this.template = template;
            this.count = count;
        }
    }
}
