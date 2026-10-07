package com.xyp.gtnotgood.common.parts.advancedio;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;

import appeng.api.config.InsertionMode;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.util.InventoryAdaptor;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;

/**
 * One adjacent, already-loaded machine face. Item and fluid adapters stay separate so a GT tank's fluid API
 * cannot hide its item inventory. Every mutation uses the machine's ordinary sided insertion/extraction API.
 */
public final class BusTarget {

    private final InventoryAdaptor items;
    private final IFluidHandler fluids;
    private final ForgeDirection face;
    private final BaseMetaTileEntity nativeTile;
    private final MTEBasicMachine nativeMachine;

    public BusTarget(TileEntity tile, ForgeDirection face) {
        this(tile, face, false);
    }

    /**
     * @param nativeOutputsOnly restrict native GT singleblocks to their declared output slots and drainable tank
     */
    public BusTarget(TileEntity tile, ForgeDirection face, boolean nativeOutputsOnly) {
        this.face = face;
        items = InventoryAdaptor.getAdaptor(
            tile,
            face,
            InventoryAdaptor.ALLOW_ITEMS | InventoryAdaptor.FOR_INSERTS | InventoryAdaptor.FOR_EXTRACTS);
        fluids = tile instanceof IFluidHandler handler ? handler : null;
        nativeTile = nativeOutputsOnly && tile instanceof BaseMetaTileEntity base
            && base.getMetaTileEntity() instanceof MTEBasicMachine ? base : null;
        nativeMachine = nativeTile == null ? null : (MTEBasicMachine) nativeTile.getMetaTileEntity();
    }

    public boolean available() {
        return items != null || fluids != null;
    }

    public boolean hasNativeOutputs() {
        return nativeMachine != null;
    }

    /** Returns distinct resource identities and total visible stock, including non-extractable input slots. */
    public List<IAEStack<?>> stock() {
        List<IAEStack<?>> result = new ArrayList<>();
        if (nativeMachine != null) {
            for (int slot = nativeMachine.getOutputSlot(); slot < nativeOutputEnd(); slot++) {
                ItemStack stack = nativeTile.getStackInSlot(slot);
                if (canExtractNativeOutput(slot, stack)) add(result, AEItemStack.create(stack));
            }
            FluidStack output = nativeMachine.getDrainableStack();
            if (output != null && allowsUntypedDrain(output)) {
                FluidStack offered = fluids.drain(face, output.amount, false);
                if (offered != null && offered.isFluidEqual(output)) add(result, AEFluidStack.create(offered));
            }
            return result;
        }
        if (items != null) {
            for (var slot : items) add(result, slot.getAEItemStack());
        }
        if (fluids != null) {
            var tanks = fluids.getTankInfo(face);
            if (tanks != null) {
                for (var tank : tanks) if (tank != null) add(result, AEFluidStack.create(tank.fluid));
            }
        }
        return result;
    }

    static void add(List<IAEStack<?>> result, IAEStack<?> stack) {
        if (stack == null || stack.getStackSize() <= 0) return;
        for (var existing : result) {
            if (existing.isSameType(stack)) {
                existing.setStackSize(existing.getStackSize() + stack.getStackSize());
                return;
            }
        }
        result.add(stack.copy());
    }

    public static long count(List<IAEStack<?>> stock, IAEStack<?> key) {
        for (var stack : stock) if (stack.isSameType(key)) return stack.getStackSize();
        return 0;
    }

    /** @return accepted quantity; the supplied identity/quantity is never mutated */
    public long insert(IAEStack<?> stack, boolean simulate) {
        if (stack instanceof IAEItemStack && items != null) {
            var rest = simulate ? items.simulateAddStack(stack.copy(), InsertionMode.DEFAULT)
                : items.addStack(stack.copy(), InsertionMode.DEFAULT);
            return stack.getStackSize() - (rest == null ? 0 : rest.getStackSize());
        }
        if (stack instanceof IAEFluidStack fluid && fluids != null) {
            return fluids.fill(face, fluid.getFluidStack(), !simulate);
        }
        return 0;
    }

    /** @return what the connected face actually allowed to be extracted, or null */
    public IAEStack<?> extract(IAEStack<?> stack, boolean simulate) {
        if (stack instanceof IAEItemStack item && nativeMachine != null) return extractNativeOutput(item, simulate);
        if (stack instanceof IAEItemStack item && items != null) {
            return AEItemStack.create(
                simulate ? items.simulateRemove((int) stack.getStackSize(), item.getItemStack(), null)
                    : items.removeItems((int) stack.getStackSize(), item.getItemStack(), null));
        }
        if (stack instanceof IAEFluidStack fluid && fluids != null) {
            FluidStack requested = fluid.getFluidStack();
            if (nativeMachine != null) {
                FluidStack output = nativeMachine.getDrainableStack();
                if (output == null || !output.isFluidEqual(requested) || !allowsUntypedDrain(requested)) return null;
                FluidStack offered = fluids.drain(face, requested.amount, false);
                if (offered == null || !offered.isFluidEqual(requested)) return null;
                FluidStack drained = simulate ? offered
                    : fluids.drain(face, Math.min(requested.amount, offered.amount), true);
                return AEFluidStack.create(drained);
            }
            FluidStack drained = fluids.drain(face, requested, !simulate);
            if (drained == null && allowsUntypedDrain(requested)) {
                // GT's typed drain checks the input tank even when its ordinary drain targets the output tank.
                FluidStack offered = fluids.drain(face, requested.amount, false);
                if (offered != null && offered.isFluidEqual(requested)) {
                    drained = simulate ? offered : fluids.drain(face, Math.min(requested.amount, offered.amount), true);
                }
            }
            return AEFluidStack.create(drained);
        }
        return null;
    }

    private int nativeOutputEnd() {
        return nativeMachine.getOutputSlot()
            + (nativeMachine.mOutputItems == null ? 0 : nativeMachine.mOutputItems.length);
    }

    private boolean canExtractNativeOutput(int slot, ItemStack stack) {
        if (stack == null || stack.stackSize <= 0 || !nativeTile.canExtractItem(slot, stack, face.ordinal()))
            return false;
        for (int accessible : nativeTile.getAccessibleSlotsFromSide(face.ordinal()))
            if (accessible == slot) return true;
        return false;
    }

    private IAEItemStack extractNativeOutput(IAEItemStack requested, boolean simulate) {
        long remaining = Math.min(Integer.MAX_VALUE, requested.getStackSize());
        long taken = 0;
        for (int slot = nativeMachine.getOutputSlot(); slot < nativeOutputEnd() && remaining > 0; slot++) {
            ItemStack stack = nativeTile.getStackInSlot(slot);
            if (!canExtractNativeOutput(slot, stack) || !requested.isSameType(AEItemStack.create(stack))) continue;
            int quantity = (int) Math.min(remaining, stack.stackSize);
            ItemStack actual = simulate ? stack : nativeTile.decrStackSize(slot, quantity);
            int extracted = actual == null ? 0 : Math.min(quantity, actual.stackSize);
            taken += extracted;
            remaining -= extracted;
        }
        return taken <= 0 ? null
            : requested.copy()
                .setStackSize(taken);
    }

    private boolean allowsUntypedDrain(FluidStack requested) {
        if (fluids instanceof BaseMetaTileEntity tile) {
            return tile.getMetaTileEntity() != null && tile.getMetaTileEntity()
                .isLiquidOutput(face)
                && tile.getCoverAtSide(face)
                    .letsFluidOut(requested.getFluid());
        }
        return fluids.canDrain(face, requested.getFluid());
    }
}
