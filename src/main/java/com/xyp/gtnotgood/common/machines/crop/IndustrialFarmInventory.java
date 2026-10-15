package com.xyp.gtnotgood.common.machines.crop;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.cleanroommc.modularui.utils.item.ItemStackHandler;
import com.xyp.gtnotgood.common.machines.multiblock.LargeIndustrialFarm;

public final class IndustrialFarmInventory extends ItemStackHandler {

    public static final int CROP_COUNT = 36;
    public static final int SLOTS_PER_CROP = 3;

    private static final int INVENTORY_SIZE = CROP_COUNT * SLOTS_PER_CROP;

    private final LargeIndustrialFarm farm;

    public IndustrialFarmInventory(LargeIndustrialFarm farm) {
        super(INVENTORY_SIZE);
        this.farm = farm;
    }

    public static int slot(int crop, int kind) {
        if (crop < 0 || crop >= CROP_COUNT || kind < 0 || kind >= 4 || kind == 1) {
            throw new IndexOutOfBoundsException("Invalid industrial farm crop or slot kind");
        }
        return crop * SLOTS_PER_CROP + (kind == 0 ? 0 : kind - 1);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        validateSlotIndex(slot);
        if (stack == null || stack.stackSize <= 0 || !isItemValid(slot, stack)) return stack;
        return super.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        validateSlotIndex(slot);
        if (amount <= 0 || !farm.canEditCrops()) return null;
        return super.extractItem(slot, amount, simulate);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        validateSlotIndex(slot);
        if (stack == null || stack.stackSize <= 0 || !farm.canEditCrops()) return false;
        int crop = slot / SLOTS_PER_CROP;
        int kind = slot % SLOTS_PER_CROP;
        return kind == 0 ? farm.isCropSeedValid(crop, stack) : farm.isCropEnvironmentValid(crop, kind - 1, stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        validateSlotIndex(slot);
        return slot % SLOTS_PER_CROP == 0 ? Math.max(0, farm.seedCapacity) : 1;
    }

    @Override
    protected int getStackLimit(int slot, ItemStack stack) {
        if (stack == null) return 0;
        return getSlotLimit(slot);
    }

    @Override
    protected void onContentsChanged(int slot) {
        farm.onContentsChanged(slot);
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        NBTTagCompound fixedSize = (NBTTagCompound) nbt.copy();
        fixedSize.setInteger("Size", INVENTORY_SIZE);
        super.deserializeNBT(fixedSize);
    }
}
