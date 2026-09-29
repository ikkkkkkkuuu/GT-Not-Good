package com.xyp.gtnotgood.common.compat;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

/** Stores quantities separately from ItemStack's byte-sized NBT Count field. */
public final class CircuitInputBufferState {

    private static final String KEY_ITEMS = "Items";
    private static final String KEY_FLUID = "Fluid";
    private static final String KEY_SIDE = "Side";
    private static final String KEY_QUANTITY = "Quantity";

    private final List<ItemStack> items = new ArrayList<>();
    private FluidStack fluid;
    private ForgeDirection side = ForgeDirection.UNKNOWN;

    public boolean isEmpty() {
        return items.isEmpty() && fluid == null;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public FluidStack getFluid() {
        return fluid;
    }

    public void setFluid(FluidStack fluid) {
        this.fluid = fluid;
    }

    public ForgeDirection getSide() {
        return side;
    }

    public void replace(List<ItemStack> pendingItems, FluidStack pendingFluid, ForgeDirection inputSide) {
        items.clear();
        for (ItemStack item : pendingItems) if (item != null && item.stackSize > 0) items.add(item.copy());
        fluid = pendingFluid == null || pendingFluid.amount <= 0 ? null : pendingFluid.copy();
        side = isEmpty() ? ForgeDirection.UNKNOWN : inputSide;
    }

    public void clearIfEmpty() {
        if (isEmpty()) side = ForgeDirection.UNKNOWN;
    }

    public void writeToNBT(NBTTagCompound target) {
        if (isEmpty()) {
            target.removeTag("GTNGCircuitInputBuffer");
            return;
        }
        NBTTagCompound saved = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (ItemStack item : items) {
            NBTTagCompound entry = new NBTTagCompound();
            ItemStack identity = item.copy();
            identity.stackSize = 1;
            identity.writeToNBT(entry);
            entry.setInteger(KEY_QUANTITY, item.stackSize);
            list.appendTag(entry);
        }
        saved.setTag(KEY_ITEMS, list);
        if (fluid != null) saved.setTag(KEY_FLUID, fluid.writeToNBT(new NBTTagCompound()));
        saved.setInteger(KEY_SIDE, side.ordinal());
        target.setTag("GTNGCircuitInputBuffer", saved);
    }

    public void readFromNBT(NBTTagCompound source) {
        items.clear();
        fluid = null;
        side = ForgeDirection.UNKNOWN;
        if (!source.hasKey("GTNGCircuitInputBuffer", Constants.NBT.TAG_COMPOUND)) return;
        NBTTagCompound saved = source.getCompoundTag("GTNGCircuitInputBuffer");
        NBTTagList list = saved.getTagList(KEY_ITEMS, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            ItemStack item = ItemStack.loadItemStackFromNBT(entry);
            int quantity = entry.getInteger(KEY_QUANTITY);
            if (item != null && quantity > 0) {
                item.stackSize = quantity;
                items.add(item);
            }
        }
        if (saved.hasKey(KEY_FLUID, Constants.NBT.TAG_COMPOUND))
            fluid = FluidStack.loadFluidStackFromNBT(saved.getCompoundTag(KEY_FLUID));
        if (fluid != null && fluid.amount <= 0) fluid = null;
        int ordinal = saved.getInteger(KEY_SIDE);
        if (ordinal >= 0 && ordinal < ForgeDirection.VALID_DIRECTIONS.length)
            side = ForgeDirection.VALID_DIRECTIONS[ordinal];
        clearIfEmpty();
    }
}
