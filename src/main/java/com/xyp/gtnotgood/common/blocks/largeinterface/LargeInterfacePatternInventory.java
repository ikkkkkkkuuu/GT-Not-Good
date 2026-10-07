package com.xyp.gtnotgood.common.blocks.largeinterface;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.inventory.IAEAppEngInventory;

/** Caches the terminal row count while retaining all 900 native pattern slots. */
public final class LargeInterfacePatternInventory extends AppEngInternalInventory {

    private boolean terminalRowsDirty = true;
    private int terminalRows = 2;

    public LargeInterfacePatternInventory(IAEAppEngInventory owner) {
        super(owner, LargeInterfaceHost.PATTERN_COUNT);
    }

    /** Uses the super pattern input assembly's empty and full-row expansion rules. */
    public int getTerminalRows() {
        if (terminalRowsDirty) {
            int lastNonNullIndex = getSizeInventory() - 1;
            while (lastNonNullIndex >= 0 && getStackInSlot(lastNonNullIndex) == null) lastNonNullIndex--;
            int calculatedRows = (lastNonNullIndex + LargeInterfaceHost.PATTERN_COLUMNS)
                / LargeInterfaceHost.PATTERN_COLUMNS + 1;
            if ((lastNonNullIndex + 1) % LargeInterfaceHost.PATTERN_COLUMNS == 0) calculatedRows++;
            terminalRows = Math.min(calculatedRows, LargeInterfaceHost.PATTERN_ROWS);
            terminalRowsDirty = false;
        }
        return terminalRows;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        terminalRowsDirty = true;
        super.setInventorySlotContents(slot, stack);
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        terminalRowsDirty = true;
        return super.decrStackSize(slot, amount);
    }

    @Override
    public void markDirty() {
        terminalRowsDirty = true;
        super.markDirty();
    }

    @Override
    public void markDirty(int slot) {
        terminalRowsDirty = true;
        super.markDirty(slot);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        terminalRowsDirty = true;
        super.readFromNBT(data);
    }
}
