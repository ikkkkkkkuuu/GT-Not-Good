package com.xyp.gtnotgood.common.machines.crop;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.cleanroommc.modularui.utils.item.ItemStackHandler;
import com.xyp.gtnotgood.common.machines.multiblock.LargeIndustrialFarm;

public final class IndustrialFarmInventory extends ItemStackHandler {

    public static final int CROP_COUNT = 36;
    public static final int SLOTS_PER_CROP = 2;

    private static final int ENVIRONMENT_START = CROP_COUNT * SLOTS_PER_CROP;
    private static final int INVENTORY_SIZE = ENVIRONMENT_START + 2;

    private final LargeIndustrialFarm farm;

    public IndustrialFarmInventory(LargeIndustrialFarm farm) {
        super(INVENTORY_SIZE);
        this.farm = farm;
    }

    public static int slot(int crop, int kind) {
        if (crop < 0 || crop >= CROP_COUNT || kind < 0 || kind >= 4) {
            throw new IndexOutOfBoundsException("Invalid industrial farm crop or slot kind");
        }
        return kind < SLOTS_PER_CROP ? crop * SLOTS_PER_CROP + kind : ENVIRONMENT_START + kind - 2;
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
        if (slot < ENVIRONMENT_START) {
            int crop = slot / SLOTS_PER_CROP;
            int kind = slot % SLOTS_PER_CROP;
            if (kind == 0 && farm.getCropSoil(crop) != null || kind == 1 && farm.getCropSeed(crop) != null) {
                return null;
            }
        }
        return super.extractItem(slot, amount, simulate);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        validateSlotIndex(slot);
        if (stack == null || stack.stackSize <= 0 || !farm.canEditCrops()) return false;
        if (slot >= ENVIRONMENT_START) return farm.isCropEnvironmentValid(0, slot - ENVIRONMENT_START, stack);
        int crop = slot / SLOTS_PER_CROP;
        return slot % SLOTS_PER_CROP == 0 ? farm.isCropSeedValid(crop, stack) : farm.isCropSoilValid(crop, stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        validateSlotIndex(slot);
        if (slot >= ENVIRONMENT_START) return 1;
        if (slot % SLOTS_PER_CROP == 1) return Math.max(0, farm.seedCapacity);
        ItemStack seed = getStackInSlot(slot);
        long free = Math.max(0L, (long) farm.seedCapacity - farm.getUsedSeedCount());
        return (int) Math.min(Integer.MAX_VALUE, (seed == null ? 0L : seed.stackSize) + free);
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
