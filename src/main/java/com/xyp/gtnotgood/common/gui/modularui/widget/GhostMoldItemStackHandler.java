package com.xyp.gtnotgood.common.gui.modularui.widget;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.utils.item.IItemHandlerModifiable;
import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtnotgood.common.utils.MoldDataManager;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.util.GTUtility;

/**
 * 幽灵模具槽位的物品处理器
 * 仿照 GhostCircuitItemStackHandler，但用于模具物品
 * 模具物品以 stackSize=0 的幽灵形式存储在真实库存中
 */
public class GhostMoldItemStackHandler implements IItemHandlerModifiable {

    public static final int NO_MOLD = -1;

    private final IItemHandlerModifiable inventory;
    private final int moldSlot;

    public GhostMoldItemStackHandler(IMetaTileEntity mte) {
        if (!(mte instanceof SuperMTEHatchCraftingInputME hatch)) {
            throw new IllegalArgumentException(mte + " does not implement SuperMTEHatchCraftingInputME");
        }
        this.inventory = mte.getInventoryHandler();
        this.moldSlot = hatch.getMoldSlot();
    }

    /**
     * 返回是否已选择模具
     */
    public boolean hasMold() {
        return inventory.getStackInSlot(moldSlot) != null;
    }

    /**
     * 获取当前模具的索引（在 CRIB_MOLDS 数组中的位置），未选择时返回 NO_MOLD
     */
    public int getMoldIndex() {
        ItemStack current = inventory.getStackInSlot(moldSlot);
        if (current == null) return NO_MOLD;
        for (int i = 0; i < MoldDataManager.getMoldCount(); i++) {
            if (GTUtility.areStacksEqual(MoldDataManager.getMolds()[i], current, true)) {
                return i;
            }
        }
        return NO_MOLD;
    }

    /**
     * 通过索引设置模具，NO_MOLD 表示清除
     */
    public void setMoldIndex(int index) {
        if (index == NO_MOLD) {
            inventory.setStackInSlot(moldSlot, null);
        } else if (index >= 0 && index < MoldDataManager.getMoldCount()) {
            inventory.setStackInSlot(moldSlot, MoldDataManager.getMolds()[index].copy());
        } else {
            throw new IllegalArgumentException("Invalid mold index: " + index);
        }
    }

    @Override
    public void setStackInSlot(int slot, @Nullable ItemStack stack) {
        validateSlot(slot);
        if (isItemValid(slot, stack)) {
            if (stack != null) {
                stack.stackSize = 0;
            }
            inventory.setStackInSlot(moldSlot, stack);
        }
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Nullable
    @Override
    public ItemStack getStackInSlot(int slot) {
        validateSlot(slot);
        return inventory.getStackInSlot(moldSlot);
    }

    @Nullable
    @Override
    public ItemStack insertItem(int slot, @Nullable ItemStack stack, boolean simulate) {
        validateSlot(slot);
        return stack;
    }

    @Nullable
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        validateSlot(slot);
        return null;
    }

    @Override
    public int getSlotLimit(int slot) {
        validateSlot(slot);
        return 0;
    }

    @Override
    public boolean isItemValid(int slot, @Nullable ItemStack stack) {
        validateSlot(slot);
        if (stack == null) return true;
        // 只允许模具类物品
        for (ItemStack mold : MoldDataManager.getMolds()) {
            if (GTUtility.areStacksEqual(mold, stack, true)) return true;
        }
        return false;
    }

    private void validateSlot(int slot) {
        if (slot != 0) throw new IndexOutOfBoundsException("Slot index out of bounds: " + slot);
    }
}
