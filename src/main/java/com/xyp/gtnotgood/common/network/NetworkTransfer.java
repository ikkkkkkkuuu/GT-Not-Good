package com.xyp.gtnotgood.common.network;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;

import com.xyp.gtnotgood.common.network.NetworkTopology.Endpoint;
import com.xyp.gtnotgood.common.network.TileNetworkController.Channel;

/**
 * Loaded-device routing using the 1.7.10 sided inventory and fluid contracts.
 * Extraction precedes insertion; a persisted channel buffer retains anything the destination declines.
 * Each item batch can span source slots and serve multiple destinations, within the configured rate and work budget.
 */
public final class NetworkTransfer {

    private NetworkTransfer() {}

    public static boolean tick(TileNetworkController controller, Channel channel) {
        WorkBudget budget = new WorkBudget();
        List<Endpoint> sources = new ArrayList<>();
        List<Endpoint> sinks = new ArrayList<>();
        for (Endpoint endpoint : controller.topology().endpoints) {
            NetworkRule rule = channel.rules.get(endpoint.key);
            if (rule == null || rule.mode == 0) continue;
            if (rule.mode == 1 && rule.due(
                controller.getWorldObj()
                    .getTotalWorldTime()))
                sources.add(endpoint);
            if (rule.mode == 2) sinks.add(endpoint);
        }
        if (sinks.isEmpty()) return false;
        // Stable sort retains the rotated order between destinations of equal priority.
        if (channel.distribution != 2) java.util.Collections.rotate(sinks, -(channel.cursor % sinks.size()));
        if (channel.distribution == 2) sinks.sort(
            Comparator.comparingInt((Endpoint e) -> channel.rules.get(e.key).priority)
                .reversed());
        boolean changed = false;
        if (!channel.hasCargo()) {
            for (int i = 0; i < sources.size(); i++) {
                Endpoint source = sources.get((channel.cursor + i) % sources.size());
                if (!budget.spend()) break;
                if (extract(channel, source, sinks, budget)) {
                    changed = true;
                    break;
                }
            }
        }
        // No batch was extracted, so destination capacity cannot affect this tick.
        if (!channel.hasCargo()) {
            channel.cursor = (channel.cursor + 1) & Integer.MAX_VALUE;
            return changed;
        }
        long[] demand = new long[sinks.size()];
        for (int i = 0; i < sinks.size(); i++) {
            Endpoint sink = sinks.get(i);
            NetworkRule rule = channel.rules.get(sink.key);
            if (!rule.due(
                controller.getWorldObj()
                    .getTotalWorldTime())
                || deviceKey(sink).equals(channel.source)) continue;
            if (!budget.spend()) break;
            TileEntity target = sink.target();
            if (channel.item != null && target instanceof IInventory inventory && rule.accepts(channel.item)) {
                demand[i] = insert(
                    inventory,
                    rule.face(sink)
                        .ordinal(),
                    channel.item,
                    rule.rate,
                    true,
                    budget);
            } else
                if (channel.fluid != null && target instanceof IFluidHandler handler && rule.accepts(channel.fluid)) {
                    FluidStack offer = channel.fluid.copy();
                    offer.amount = Math.min(offer.amount, rule.rate);
                    demand[i] = Math.max(0, Math.min(offer.amount, handler.fill(rule.face(sink), offer, false)));
                }
        }
        long available = channel.item != null ? channel.item.stackSize
            : channel.fluid != null ? channel.fluid.amount : 0;
        long[] allocations = NetworkDistribution.allocate(available, demand, channel.distribution);
        channel.cursor = (channel.cursor + 1) & Integer.MAX_VALUE;
        for (int i = 0; i < sinks.size(); i++) {
            if (allocations[i] == 0) continue;
            if (!channel.hasCargo() || !budget.spend()) break;
            Endpoint sink = sinks.get(i);
            NetworkRule rule = channel.rules.get(sink.key);
            TileEntity target = sink.target();
            if (channel.item != null && target instanceof IInventory inventory) {
                int moved = insert(
                    inventory,
                    rule.face(sink)
                        .ordinal(),
                    channel.item,
                    (int) allocations[i],
                    false,
                    budget);
                channel.item.stackSize -= moved;
                if (channel.item.stackSize <= 0) channel.item = null;
                if (moved > 0) changed = true;
            }
            if (channel.fluid != null && target instanceof IFluidHandler handler) {
                FluidStack offer = channel.fluid.copy();
                offer.amount = Math.min(offer.amount, (int) allocations[i]);
                int moved = Math.max(0, Math.min(offer.amount, handler.fill(rule.face(sink), offer, true)));
                channel.fluid.amount -= moved;
                if (channel.fluid.amount <= 0) channel.fluid = null;
                if (moved > 0) {
                    target.markDirty();
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static boolean extract(Channel channel, Endpoint source, List<Endpoint> sinks, WorkBudget budget) {
        NetworkRule rule = channel.rules.get(source.key);
        TileEntity target = source.target();
        if (channel.type == 0 && target instanceof IInventory inventory) {
            int[] accessible = slots(
                inventory,
                rule.face(source)
                    .ordinal());
            int start = accessible.length == 0 ? 0 : Math.floorMod(rule.extractionCursor, accessible.length);
            for (int offset = 0; offset < accessible.length; offset++) {
                if (!budget.spend()) return false;
                int index = (start + offset) % accessible.length;
                int slot = accessible[index];
                rule.extractionCursor = (index + 1) % accessible.length;
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack == null || stack.stackSize <= 0 || !rule.accepts(stack)) continue;
                if (inventory instanceof ISidedInventory sided && !sided.canExtractItem(
                    slot,
                    stack,
                    rule.face(source)
                        .ordinal()))
                    continue;
                int amount = rule.rate;
                ItemStack offer = stack.copy();
                offer.stackSize = amount;
                int room = 0;
                Set<String> countedDevices = new HashSet<>();
                for (Endpoint sink : sinks) {
                    if (!budget.spend()) return false;
                    if (deviceKey(sink).equals(deviceKey(source)) || !countedDevices.add(deviceKey(sink))) continue;
                    NetworkRule output = channel.rules.get(sink.key);
                    if (sink.target() instanceof IInventory destination && output.accepts(stack)) {
                        room += insert(
                            destination,
                            output.face(sink)
                                .ordinal(),
                            offer,
                            Math.min(amount - room, output.rate),
                            true,
                            budget);
                        if (room >= amount) break;
                    }
                }
                if (room == 0) continue;
                channel.item = collect(
                    inventory,
                    rule.face(source)
                        .ordinal(),
                    stack,
                    Math.min(amount, room),
                    budget);
                if (channel.item == null || channel.item.stackSize <= 0) {
                    channel.item = null;
                    continue;
                }
                channel.source = deviceKey(source);
                inventory.markDirty();
                return true;
            }
        } else if (channel.type == 1 && target instanceof IFluidHandler handler) {
            FluidStack preview = null;
            net.minecraftforge.fluids.FluidTankInfo[] tanks = handler.getTankInfo(rule.face(source));
            if (tanks != null) for (net.minecraftforge.fluids.FluidTankInfo tank : tanks) {
                if (!budget.spend()) return false;
                if (tank == null || tank.fluid == null || !rule.accepts(tank.fluid)) continue;
                FluidStack sample = tank.fluid.copy();
                sample.amount = Math.min(rule.rate, sample.amount);
                preview = handler.drain(rule.face(source), sample, false);
                if (preview != null && preview.amount > 0) break;
            }
            if (preview == null) preview = handler.drain(rule.face(source), rule.rate, false);
            if (preview == null || preview.amount <= 0 || !rule.accepts(preview)) return false;
            preview.amount = Math.min(preview.amount, rule.rate);
            int room = 0;
            Set<String> countedDevices = new HashSet<>();
            for (Endpoint sink : sinks) {
                if (!budget.spend()) break;
                if (deviceKey(sink).equals(deviceKey(source))) continue;
                NetworkRule output = channel.rules.get(sink.key);
                if (!(sink.target() instanceof IFluidHandler destination) || !output.accepts(preview)) continue;
                if (!countedDevices.add(deviceKey(sink))) continue;
                FluidStack offer = preview.copy();
                offer.amount = Math.min(preview.amount - room, output.rate);
                room += Math.max(0, Math.min(offer.amount, destination.fill(output.face(sink), offer, false)));
                if (room >= preview.amount) break;
            }
            if (room <= 0) return false;
            preview.amount = room;
            channel.fluid = handler.drain(rule.face(source), preview, true);
            if (channel.fluid == null || channel.fluid.amount <= 0) {
                channel.fluid = null;
                return false;
            }
            channel.source = deviceKey(source);
            target.markDirty();
            return true;
        }
        return false;
    }

    /** Collects identical item/NBT stacks across slots without exceeding the source rate or sided permissions. */
    static ItemStack collect(IInventory inventory, int side, ItemStack template, int limit) {
        return collect(inventory, side, template, limit, new WorkBudget());
    }

    private static ItemStack collect(IInventory inventory, int side, ItemStack template, int limit, WorkBudget budget) {
        ItemStack result = null;
        int remaining = limit;
        for (int slot : slots(inventory, side)) {
            if (remaining <= 0 || !budget.spend()) break;
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack == null || stack.stackSize <= 0
                || !stack.isItemEqual(template)
                || !ItemStack.areItemStackTagsEqual(stack, template)) continue;
            if (inventory instanceof ISidedInventory sided && !sided.canExtractItem(slot, stack, side)) continue;
            ItemStack extracted = inventory.decrStackSize(slot, Math.min(remaining, stack.stackSize));
            if (extracted == null || extracted.stackSize <= 0) continue;
            if (result == null) result = extracted.copy();
            else result.stackSize += extracted.stackSize;
            remaining -= extracted.stackSize;
        }
        if (result != null) inventory.markDirty();
        return result;
    }

    /** Returns actual or simulated accepted count without mutating the offered stack. */
    static int insert(IInventory inventory, int side, ItemStack offer, int limit, boolean simulate) {
        return insert(inventory, side, offer, limit, simulate, new WorkBudget());
    }

    private static int insert(IInventory inventory, int side, ItemStack offer, int limit, boolean simulate,
        WorkBudget budget) {
        int remaining = Math.min(offer.stackSize, limit);
        int initial = remaining;
        for (int slot : slots(inventory, side)) {
            if (remaining <= 0 || !budget.spend()) break;
            if (!inventory.isItemValidForSlot(slot, offer)) continue;
            if (inventory instanceof ISidedInventory sided && !sided.canInsertItem(slot, offer, side)) continue;
            ItemStack existing = inventory.getStackInSlot(slot);
            if (existing != null && (!existing.isItemEqual(offer) || !ItemStack.areItemStackTagsEqual(existing, offer)))
                continue;
            int capacity = Math.min(inventory.getInventoryStackLimit(), offer.getMaxStackSize());
            int moved = Math.min(remaining, Math.max(0, capacity - (existing == null ? 0 : existing.stackSize)));
            if (moved <= 0) continue;
            if (!simulate) {
                ItemStack replacement = existing == null ? offer.copy() : existing.copy();
                replacement.stackSize = (existing == null ? 0 : existing.stackSize) + moved;
                inventory.setInventorySlotContents(slot, replacement);
            }
            remaining -= moved;
        }
        if (!simulate && remaining < initial) inventory.markDirty();
        return initial - remaining;
    }

    private static int[] slots(IInventory inventory, int side) {
        int size = inventory.getSizeInventory();
        if (inventory instanceof ISidedInventory sided) {
            int[] accessible = sided.getAccessibleSlotsFromSide(side);
            if (accessible == null) return new int[0];
            Set<Integer> seen = new HashSet<>();
            return java.util.Arrays.stream(accessible)
                .limit(4096)
                .filter(slot -> slot >= 0 && slot < size && seen.add(slot))
                .toArray();
        }
        return java.util.stream.IntStream.range(0, Math.min(4096, size))
            .toArray();
    }

    private static String deviceKey(Endpoint endpoint) {
        return (endpoint.connector.xCoord + endpoint.direction.offsetX) + ":"
            + (endpoint.connector.yCoord + endpoint.direction.offsetY)
            + ":"
            + (endpoint.connector.zCoord + endpoint.direction.offsetZ);
    }

    /** Bounds slot/probe work per channel operation; unfinished cargo waits for the next operation. */
    private static final class WorkBudget {

        private int remaining = 8192;

        boolean spend() {
            return remaining-- > 0;
        }
    }
}
