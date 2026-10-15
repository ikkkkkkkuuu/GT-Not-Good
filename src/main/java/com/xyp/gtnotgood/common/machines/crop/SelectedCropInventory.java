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
        if (isClient()) return super.getStackInSlot(slot);
        return farm.getCropInventory().getStackInSlot(storageSlot(slot));
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (isClient()) {
            super.setStackInSlot(slot, stack);
            return;
        }
        farm.getCropInventory().setStackInSlot(storageSlot(slot), stack);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (isClient()) {
            validateSlotIndex(slot);
            if (stack == null || stack.stackSize <= 0 || !isItemValid(slot, stack)) return stack;
            return super.insertItem(slot, stack, simulate);
        }
        return farm.getCropInventory().insertItem(storageSlot(slot), stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (isClient()) {
            validateSlotIndex(slot);
            return amount <= 0 ? null : super.extractItem(slot, amount, simulate);
        }
        return farm.getCropInventory().extractItem(storageSlot(slot), amount, simulate);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return farm.getCropInventory().isItemValid(storageSlot(slot), stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        if (isClient()) {
            validateSlotIndex(slot);
            if (slot >= 2) return 1;
            if (slot == 1) return Math.max(0, farm.seedCapacity);
            ItemStack seed = super.getStackInSlot(slot);
            long free = Math.max(0L, (long) farm.seedCapacity - farm.getUsedSeedCount());
            return (int) Math.min(Integer.MAX_VALUE, (seed == null ? 0L : seed.stackSize) + free);
        }
        return farm.getCropInventory().getSlotLimit(storageSlot(slot));
    }

    @Override
    protected int getStackLimit(int slot, ItemStack stack) {
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
