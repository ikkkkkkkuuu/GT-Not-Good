package com.xyp.gtnotgood.common.machines.crop;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizon.cropsnh.tileentity.multi.MTEIndustrialFarmItemStackHandler;
import com.xyp.gtnotgood.common.machines.multiblock.LargeIndustrialFarm;

public final class SelectedCropInventory extends MTEIndustrialFarmItemStackHandler {

    private final LargeIndustrialFarm farm;

    public SelectedCropInventory(LargeIndustrialFarm farm) {
        super(farm);
        this.farm = farm;
    }

    private int storageSlot(int slot) {
        validateSlotIndex(slot);
        return IndustrialFarmInventory.slot(farm.getSelectedCrop(), slot);
    }

    private boolean isClient() {
        return farm.getBaseMetaTileEntity() != null && farm.getBaseMetaTileEntity().isClientSide();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        validateSlotIndex(slot);
        if (slot == 1) return null;
        if (isClient()) return super.getStackInSlot(slot);
        return farm.getCropInventory().getStackInSlot(storageSlot(slot));
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        validateSlotIndex(slot);
        if (slot == 1) {
            if (stack != null) throw new UnsupportedOperationException("The industrial farm has no sub-soil slot");
            return;
        }
        if (isClient()) {
            super.setStackInSlot(slot, stack);
            return;
        }
        farm.getCropInventory().setStackInSlot(storageSlot(slot), stack);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        validateSlotIndex(slot);
        if (slot == 1) return stack;
        if (isClient()) {
            if (stack == null || stack.stackSize <= 0 || !isItemValid(slot, stack)) return stack;
            return super.insertItem(slot, stack, simulate);
        }
        return farm.getCropInventory().insertItem(storageSlot(slot), stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        validateSlotIndex(slot);
        if (slot == 1) return null;
        if (isClient()) {
            return amount <= 0 ? null : super.extractItem(slot, amount, simulate);
        }
        return farm.getCropInventory().extractItem(storageSlot(slot), amount, simulate);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        validateSlotIndex(slot);
        if (slot == 1) return false;
        return farm.getCropInventory().isItemValid(storageSlot(slot), stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        validateSlotIndex(slot);
        if (slot == 1) return 0;
        if (isClient()) {
            if (slot >= 2) return 1;
            return Math.max(0, farm.seedCapacity);
        }
        return farm.getCropInventory().getSlotLimit(storageSlot(slot));
    }

    @Override
    protected int getStackLimit(int slot, ItemStack stack) {
        if (slot == 1) return 0;
        if (isClient()) return stack == null ? 0 : getSlotLimit(slot);
        return farm.getCropInventory().getStackLimit(storageSlot(slot), stack);
    }

    public ItemStack getLegacyStack(int slot) {
        return super.getStackInSlot(slot);
    }

    public void clearLegacy() {
        super.setSize(4);
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        NBTTagCompound fixedSize = (NBTTagCompound) nbt.copy();
        fixedSize.setInteger("Size", 4);
        super.deserializeNBT(fixedSize);
    }
}
