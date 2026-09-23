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
    private final Map<String, ItemStack> hosts = new LinkedHashMap<>();
    private final Map<String, ItemStack> catalysts = new LinkedHashMap<>();

    /** Collects one controller per recipe map and one of each circuit or catalyst across all locked pages. */
    public FactoryText collect(int node, FactoryRecipeCatalog.Entry entry, List<ItemStack> inputs,
        Predicate<ItemStack> host) {
        String map = entry.map.unlocalizedName;
        if (!hosts.containsKey(map)) {
            ItemStack taken = take(inputs, host);
            if (taken != null) hosts.put(map, taken);
        }
        boolean missing = false;
        for (int i = 0; i < entry.recipe.mInputs.length; i++) {
            ItemStack input = entry.recipe.mInputs[i];
            if (input == null || input.stackSize != 0) continue;
            String key = catalystKey(input);
            if (catalysts.containsKey(key)) continue;
            GTRecipe.RecipeItemInput match = new GTRecipe.RecipeItemInput(input, entry.recipe.isNBTSensitive);
            ItemStack taken = take(inputs, match::matchesType);
            if (taken != null) catalysts.put(key, taken);
            else missing = true;
        }
        return !hosts.containsKey(map) ? FactoryText.HOST : missing ? FactoryText.CATALYST_MISSING : null;
    }

    /** Stable identity for one non-consumable item template, regardless of its recipe stack size. */
    public static String catalystKey(ItemStack input) {
        ItemStack display = input.copy();
        display.stackSize = 1;
        return display.writeToNBT(new NBTTagCompound())
            .toString();
    }

    public boolean hasHost(String map) {
        return hosts.containsKey(map);
    }

    public boolean hasCatalyst(ItemStack input) {
        return catalysts.containsKey(catalystKey(input));
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
        empty &= refundMap(hosts, output);
        empty &= refundMap(catalysts, output);
        for (ItemStack[] slots : stored.values()) for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null) continue;
            if (output.test(slots[i].copy())) slots[i] = null;
            else empty = false;
        }
        if (empty) stored.clear();
        return empty;
    }

    private static boolean refundMap(Map<String, ItemStack> held, Predicate<ItemStack> output) {
        held.entrySet()
            .removeIf(
                entry -> output.test(
                    entry.getValue()
                        .copy()));
        return held.isEmpty();
    }

    /** Copies only actual held deposits for drain progress; never returns original mutable inventory stacks. */
    public java.util.List<ItemStack> remaining() {
        java.util.List<ItemStack> result = new java.util.ArrayList<>();
        for (ItemStack item : hosts.values()) result.add(item.copy());
        for (ItemStack item : catalysts.values()) result.add(item.copy());
        for (ItemStack[] slots : stored.values())
            for (ItemStack item : slots) if (item != null && item.stackSize > 0) result.add(item.copy());
        return result;
    }

    public NBTTagCompound write() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("hosts", writeShared(hosts));
        tag.setTag("catalysts", writeShared(catalysts));
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

    private static NBTTagList writeShared(Map<String, ItemStack> values) {
        NBTTagList list = new NBTTagList();
        values.forEach((key, stack) -> {
            NBTTagCompound row = new NBTTagCompound();
            row.setString("key", key);
            row.setTag("item", stack.writeToNBT(new NBTTagCompound()));
            list.appendTag(row);
        });
        return list;
    }

    public void read(NBTTagCompound tag) {
        readShared(hosts, tag.getTagList("hosts", 10));
        readShared(catalysts, tag.getTagList("catalysts", 10));
        stored.clear();
        NBTTagList nodes = tag.getTagList("nodes", 10);
        for (int n = 0; n < Math.min(nodes.tagCount(), FactoryGraph.MAX_ACTIVE_NODES); n++) {
            NBTTagCompound node = nodes.getCompoundTagAt(n);
            ItemStack[] slots = new ItemStack[17];
            for (int i = 0; i < slots.length; i++) {
                slots[i] = ItemStack.loadItemStackFromNBT(node.getCompoundTag("slot" + i));
            }
            stored.put(node.getInteger("id"), slots);
        }
    }

    private static void readShared(Map<String, ItemStack> target, NBTTagList list) {
        target.clear();
        for (int i = 0; i < Math.min(FactoryGraph.MAX_ACTIVE_NODES, list.tagCount()); i++) {
            NBTTagCompound row = list.getCompoundTagAt(i);
            ItemStack item = ItemStack.loadItemStackFromNBT(row.getCompoundTag("item"));
            if (item != null && !row.getString("key")
                .isEmpty()) target.put(row.getString("key"), item);
        }
    }
}
