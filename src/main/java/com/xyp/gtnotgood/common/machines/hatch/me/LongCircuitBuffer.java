package com.xyp.gtnotgood.common.machines.hatch.me;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.util.Platform;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import gregtech.api.util.GTUtility;

/** Long totals with stable int-sized recipe views; GT's in-place debits are settled before the next buffer access. */
final class LongCircuitBuffer {

    private final Buffer<IAEItemStack, ItemStack> items;
    private final Buffer<IAEFluidStack, FluidStack> fluids;

    LongCircuitBuffer(NBTTagCompound saved, Runnable changed) {
        items = new Buffer<>(
            IAEItemStack::getItemStack,
            stack -> stack.stackSize,
            (stack, count) -> stack.stackSize = count,
            changed);
        fluids = new Buffer<>(
            IAEFluidStack::getFluidStack,
            stack -> stack.amount,
            (stack, count) -> stack.amount = count,
            changed);
        if (saved == null) return;
        if (saved.hasKey("longItems") || saved.hasKey("longFluids")) {
            load(saved.getTagList("longItems", Constants.NBT.TAG_COMPOUND));
            load(saved.getTagList("longFluids", Constants.NBT.TAG_COMPOUND));
        } else {
            NBTTagList legacy = saved.getTagList("inventory", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < legacy.tagCount(); i++) {
                IAEItemStack item = AEItemStack.create(GTUtility.loadItem(legacy.getCompoundTagAt(i)));
                if (item != null && item.getStackSize() > 0) items.load(item);
            }
            legacy = saved.getTagList("fluidInventory", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < legacy.tagCount(); i++) {
                IAEFluidStack fluid = AEFluidStack.create(FluidStack.loadFluidStackFromNBT(legacy.getCompoundTagAt(i)));
                if (fluid != null && fluid.getStackSize() > 0) fluids.load(fluid);
            }
        }
    }

    private void load(NBTTagList list) {
        for (int i = 0; i < list.tagCount(); i++) {
            IAEStack<?> stack = Platform.readStackNBT(list.getCompoundTagAt(i), true);
            if (stack == null || stack.getStackSize() <= 0) continue;
            if (stack instanceof IAEItemStack item) items.load(item);
            else if (stack instanceof IAEFluidStack fluid) fluids.load(fluid);
        }
    }

    /** Stages both channels together so a late type-limit or long overflow rejects the entire CPU delivery. */
    boolean insert(MEInventoryCrafting table) {
        List<IAEItemStack> stagedItems = items.stacks();
        List<IAEFluidStack> stagedFluids = fluids.stacks();
        for (int i = 0; i < table.getSizeInventory(); i++) {
            IAEStack<?> stack = table.getAEStackInSlot(i);
            if (stack == null || stack.getStackSize() == 0) continue;
            if (stack.getStackSize() < 0 || CircuitPatternCodec.isCircuit(stack)) return false;
            if (stack instanceof IAEItemStack item) {
                if (!add(stagedItems, item)) return false;
            } else if (stack instanceof IAEFluidStack fluid) {
                if (!add(stagedFluids, fluid)) return false;
            } else return false;
        }
        items.replace(stagedItems);
        fluids.replace(stagedFluids);
        return true;
    }

    private static <T extends IAEStack<T>> boolean add(List<T> stacks, T inserted) {
        for (T stored : stacks) {
            if (!stored.isSameType(inserted)) continue;
            if (inserted.getStackSize() > Long.MAX_VALUE - stored.getStackSize()) return false;
            stored.setStackSize(stored.getStackSize() + inserted.getStackSize());
            return true;
        }
        if (stacks.size() >= CircuitMEPatternBuffer.bufferTypeLimit) return false;
        stacks.add(inserted.copy());
        return true;
    }

    List<ItemStack> itemViews() {
        return items.views();
    }

    List<FluidStack> fluidViews() {
        return fluids.views();
    }

    List<IAEItemStack> storedItems() {
        return items.stacks();
    }

    List<IAEFluidStack> storedFluids() {
        return fluids.stacks();
    }

    void write(NBTTagCompound tag) {
        tag.setTag("longItems", write(items.stacks()));
        tag.setTag("longFluids", write(fluids.stacks()));
        tag.removeTag("inventory");
        tag.removeTag("fluidInventory");
    }

    private static NBTTagList write(List<? extends IAEStack<?>> stacks) {
        NBTTagList list = new NBTTagList();
        for (IAEStack<?> stack : stacks) {
            NBTTagCompound tag = new NBTTagCompound();
            stack.writeToNBTGeneric(tag);
            list.appendTag(tag);
        }
        return list;
    }

    /** A network rejection leaves the exact long remainder here; no quantity is narrowed to an item entity. */
    void refund(AENetworkProxy proxy, BaseActionSource source) throws GridAccessException {
        for (Entry<IAEItemStack, ItemStack> entry : items.entries) {
            refund(
                entry,
                proxy,
                proxy.getStorage()
                    .getItemInventory(),
                source);
        }
        for (Entry<IAEFluidStack, FluidStack> entry : fluids.entries) {
            refund(
                entry,
                proxy,
                proxy.getStorage()
                    .getFluidInventory(),
                source);
        }
    }

    private static <T extends IAEStack<T>, V> void refund(Entry<T, V> entry, AENetworkProxy proxy,
        IMEMonitor<T> inventory, BaseActionSource source) throws GridAccessException {
        entry.settle();
        // AE2 ceilDiv adds the divisor before subtracting one. Keep that addition within long and send any tail next.
        long safeRequest = Long.MAX_VALUE - Math.max(1, entry.stored.getAmountPerUnit());
        for (int attempt = 0; attempt < 2 && entry.stored.getStackSize() > 0; attempt++) {
            long sent = Math.min(entry.stored.getStackSize(), safeRequest);
            T rest = Platform.poweredInsert(
                proxy.getEnergy(),
                inventory,
                entry.stored.copy()
                    .setStackSize(sent),
                source);
            entry.refunded(sent, rest);
            if (rest != null && rest.getStackSize() > 0) break;
        }
    }

    void clear() {
        items.entries.clear();
        fluids.entries.clear();
    }

    private static final class Buffer<T extends IAEStack<T>, V> {

        private final List<Entry<T, V>> entries = new ArrayList<>();
        private final Function<T, V> view;
        private final ToIntFunction<V> count;
        private final ObjIntConsumer<V> setCount;
        private final Runnable changed;

        Buffer(Function<T, V> view, ToIntFunction<V> count, ObjIntConsumer<V> setCount, Runnable changed) {
            this.view = view;
            this.count = count;
            this.setCount = setCount;
            this.changed = changed;
        }

        private void settle() {
            entries.forEach(Entry::settle);
            entries.removeIf(entry -> entry.stored.getStackSize() == 0);
        }

        List<T> stacks() {
            settle();
            List<T> stacks = new ArrayList<>();
            for (Entry<T, V> entry : entries) stacks.add(entry.stored.copy());
            return stacks;
        }

        List<V> views() {
            settle();
            List<V> views = new ArrayList<>();
            for (Entry<T, V> entry : entries) views.add(entry.view);
            return views;
        }

        void load(T stack) {
            List<T> staged = stacks();
            if (!add(staged, stack)) throw new IllegalArgumentException("Circuit buffer save exceeds capacity");
            replace(staged);
        }

        void replace(List<T> stacks) {
            for (T stack : stacks) {
                Entry<T, V> match = null;
                for (Entry<T, V> entry : entries) if (entry.stored.isSameType(stack)) {
                    match = entry;
                    break;
                }
                if (match == null) entries.add(new Entry<>(stack.copy(), this));
                else {
                    match.stored.setStackSize(stack.getStackSize());
                    match.refresh();
                }
            }
            changed.run();
        }
    }

    private static final class Entry<T extends IAEStack<T>, V> {

        private final T stored;
        private final V view;
        private final Buffer<T, V> owner;
        private int exposed;

        Entry(T stored, Buffer<T, V> owner) {
            this.stored = stored;
            this.owner = owner;
            view = owner.view.apply(
                stored.copy()
                    .setStackSize(1));
            refresh();
        }

        void refresh() {
            exposed = (int) Math.min(Integer.MAX_VALUE, stored.getStackSize());
            owner.setCount.accept(view, exposed);
        }

        void settle() {
            int consumed = exposed - Math.max(0, owner.count.applyAsInt(view));
            if (consumed > 0) {
                stored.setStackSize(stored.getStackSize() - consumed);
                owner.changed.run();
            }
            refresh();
        }

        void refunded(long sent, T rest) {
            long before = stored.getStackSize();
            long rejected = rest == null ? 0 : Math.max(0, Math.min(sent, rest.getStackSize()));
            stored.setStackSize(before - (sent - rejected));
            refresh();
            if (stored.getStackSize() != before) owner.changed.run();
        }
    }
}
