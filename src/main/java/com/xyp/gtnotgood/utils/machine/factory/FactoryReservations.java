package com.xyp.gtnotgood.utils.machine.factory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import gregtech.api.util.GTRecipe;

/** Real per-node controller and catalyst inventory, isolated from consumable production buffers. */
public final class FactoryReservations {

    private final Map<Integer, ItemStack[]> stored = new LinkedHashMap<>();

    /** Collects one controller per node and one of each zero-sized input; partial collections are durable. */
    public FactoryText collect(int node, FactoryRecipeCatalog.Entry entry, List<ItemStack> inputs,
        Predicate<ItemStack> host) {
        ItemStack[] slots = stored.computeIfAbsent(node, key -> new ItemStack[17]);
        collectSlot(node, 0, inputs, host);
        boolean missing = false;
        for (int i = 0; i < entry.recipe.mInputs.length; i++) {
            ItemStack input = entry.recipe.mInputs[i];
            if (input == null || input.stackSize != 0 || slots[i + 1] != null) continue;
            GTRecipe.RecipeItemInput match = new GTRecipe.RecipeItemInput(input, entry.recipe.isNBTSensitive);
            collectSlot(node, i + 1, inputs, match::matchesType);
            missing |= slots[i + 1] == null;
        }
        return slots[0] == null ? FactoryText.HOST : missing ? FactoryText.CATALYST_MISSING : null;
    }

    /** Fills a vacant reservation once; repeated collection never takes another item. */
    boolean collectSlot(int node, int slot, List<ItemStack> inputs, Predicate<ItemStack> accepts) {
        ItemStack[] slots = stored.computeIfAbsent(node, key -> new ItemStack[17]);
        if (slots[slot] == null) slots[slot] = take(inputs, accepts);
        return slots[slot] != null;
    }

    /** Copies the actual taken stack, including NBT, rather than materializing a recipe template. */
    private static ItemStack take(List<ItemStack> inputs, Predicate<ItemStack> accepts) {
        for (ItemStack stack : inputs) {
            if (stack == null || stack.stackSize <= 0 || !accepts.test(stack)) continue;
            ItemStack taken = stack.copy();
            taken.stackSize = 1;
            stack.stackSize--;
            return taken;
        }
        return null;
    }

    /** Removes each reservation only after the destination atomically accepts the actual item. */
    public boolean refund(Predicate<ItemStack> output) {
        boolean empty = true;
        for (ItemStack[] slots : stored.values()) for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null) continue;
            if (output.test(slots[i].copy())) slots[i] = null;
            else empty = false;
        }
        if (empty) stored.clear();
        return empty;
    }

    /** Copies only actual held deposits for drain progress; never returns original mutable inventory stacks. */
    public java.util.List<ItemStack> remaining() {
        java.util.List<ItemStack> result = new java.util.ArrayList<>();
        for (ItemStack[] slots : stored.values())
            for (ItemStack item : slots) if (item != null && item.stackSize > 0) result.add(item.copy());
        return result;
    }

    /** Read-only display access; GUI receives a separate synchronized reservation instance. */
    public ItemStack get(int node, int slot) {
        ItemStack[] slots = stored.get(node);
        return slots == null || slot < 0 || slot >= slots.length ? null : slots[slot];
    }

    public NBTTagCompound write() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList nodes = new NBTTagList();
        stored.forEach((id, slots) -> {
            NBTTagCompound node = new NBTTagCompound();
            node.setInteger("id", id);
            for (int i = 0; i < slots.length; i++) {
                if (slots[i] != null) node.setTag("slot" + i, slots[i].writeToNBT(new NBTTagCompound()));
            }
            nodes.appendTag(node);
        });
        tag.setTag("nodes", nodes);
        return tag;
    }

    public void read(NBTTagCompound tag) {
        stored.clear();
        NBTTagList nodes = tag.getTagList("nodes", 10);
        for (int n = 0; n < Math.min(nodes.tagCount(), FactoryGraph.MAX_NODES); n++) {
            NBTTagCompound node = nodes.getCompoundTagAt(n);
            ItemStack[] slots = new ItemStack[17];
            for (int i = 0; i < slots.length; i++) {
                slots[i] = ItemStack.loadItemStackFromNBT(node.getCompoundTag("slot" + i));
            }
            stored.put(node.getInteger("id"), slots);
        }
    }
}
