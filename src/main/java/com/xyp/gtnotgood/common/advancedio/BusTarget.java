package com.xyp.gtnotgood.common.advancedio;

import java.util.ArrayList;
import java.util.List;

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

/**
 * One adjacent, already-loaded machine face. Item and fluid adapters stay separate so a GT tank's fluid API
 * cannot hide its item inventory. Every mutation uses the machine's ordinary sided insertion/extraction API.
 */
final class BusTarget {

    private final InventoryAdaptor items;
    private final IFluidHandler fluids;
    private final ForgeDirection face;

    BusTarget(TileEntity tile, ForgeDirection face) {
        this.face = face;
        items = InventoryAdaptor.getAdaptor(
            tile,
            face,
            InventoryAdaptor.ALLOW_ITEMS | InventoryAdaptor.FOR_INSERTS | InventoryAdaptor.FOR_EXTRACTS);
        fluids = tile instanceof IFluidHandler handler ? handler : null;
    }

    boolean available() {
        return items != null || fluids != null;
    }

    /** Returns distinct resource identities and total visible stock, including non-extractable input slots. */
    List<IAEStack<?>> stock() {
        List<IAEStack<?>> result = new ArrayList<>();
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

    static long count(List<IAEStack<?>> stock, IAEStack<?> key) {
        for (var stack : stock) if (stack.isSameType(key)) return stack.getStackSize();
        return 0;
    }

    /** @return accepted quantity; the supplied identity/quantity is never mutated */
    long insert(IAEStack<?> stack, boolean simulate) {
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
    IAEStack<?> extract(IAEStack<?> stack, boolean simulate) {
        if (stack instanceof IAEItemStack item && items != null) {
            return AEItemStack.create(
                simulate ? items.simulateRemove((int) stack.getStackSize(), item.getItemStack(), null)
                    : items.removeItems((int) stack.getStackSize(), item.getItemStack(), null));
        }
        if (stack instanceof IAEFluidStack fluid && fluids != null) {
            FluidStack drained = fluids.drain(face, fluid.getFluidStack(), !simulate);
            return AEFluidStack.create(drained);
        }
        return null;
    }
}
