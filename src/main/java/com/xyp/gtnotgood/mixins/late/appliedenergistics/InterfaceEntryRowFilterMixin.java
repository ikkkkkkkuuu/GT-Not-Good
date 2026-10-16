package com.xyp.gtnotgood.mixins.late.appliedenergistics;

import java.util.Arrays;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.xyp.gtnotgood.ae2thing.quickterminal.client.InterfacePatternRows;
import com.xyp.gtnotgood.ae2thing.quickterminal.client.InterfaceRowFilter;

import appeng.client.gui.implementations.GuiInterfaceTerminal;
import appeng.core.AEConfig;
import appeng.tile.inventory.AppEngInternalInventory;

@Mixin(targets = "appeng.client.gui.implementations.GuiInterfaceTerminal$InterfaceTerminalEntry", remap = false)
public abstract class InterfaceEntryRowFilterMixin implements InterfacePatternRows {

    @Shadow
    @Final
    private GuiInterfaceTerminal this$0;

    @Shadow
    private AppEngInternalInventory inv;

    @Shadow
    private int rows;

    @Shadow
    private int rowSize;

    @Shadow
    private int numSlots;

    @Shadow
    private int guiHeight;

    @Unique
    private int[] visibleRows;

    @Unique
    private int cachedRows = -1;

    @Unique
    private int cachedRowSize;

    @Unique
    private int cachedSlots;

    @Unique
    private boolean rowsDirty = true;

    @Unique
    private boolean filtersRows() {
        return this$0 instanceof InterfaceRowFilter
            && AEConfig.instance.showOnlyInterfacesWithFreeSlotsInInterfaceTerminal;
    }

    @Inject(method = "setItemInSlot", at = @At("RETURN"))
    private void invalidateRows(ItemStack stack, int slot, CallbackInfo ci) {
        rowsDirty = true;
    }

    @Unique
    private void updateVisibleRows() {
        if (!rowsDirty && cachedRows == rows && cachedRowSize == rowSize && cachedSlots == numSlots) return;
        int[] availableRows = new int[Math.max(0, rows)];
        int count = 0;
        int limit = Math.min(numSlots, inv.getSizeInventory());
        int lastFullRow = -1;
        if (rowSize > 0) {
            for (int row = 0; row < rows && row * rowSize < limit; row++) {
                int end = Math.min((row + 1) * rowSize, limit);
                boolean full = true;
                for (int slot = row * rowSize; slot < end; slot++) {
                    if (inv.getStackInSlot(slot) == null) {
                        full = false;
                        break;
                    }
                }
                if (full) {
                    lastFullRow = row;
                } else {
                    availableRows[count++] = row;
                }
            }
            if (lastFullRow >= 0) {
                availableRows[count++] = lastFullRow;
                Arrays.sort(availableRows, 0, count);
            }
        }
        visibleRows = Arrays.copyOf(availableRows, count);
        cachedRows = rows;
        cachedRowSize = rowSize;
        cachedSlots = numSlots;
        rowsDirty = false;
    }

    @Override
    public int getVisibleRowCount() {
        if (!filtersRows()) return rows;
        updateVisibleRows();
        return visibleRows.length;
    }

    @Override
    public int getVisibleHeight() {
        return filtersRows() ? getVisibleRowCount() * 18 + 1 : guiHeight;
    }

    @Override
    public int getActualSlot(int visibleSlot) {
        if (visibleSlot < 0 || rowSize <= 0) return -1;
        int actualSlot = visibleSlot;
        if (filtersRows()) {
            updateVisibleRows();
            int row = visibleSlot / rowSize;
            if (row >= visibleRows.length) return -1;
            actualSlot = visibleRows[row] * rowSize + visibleSlot % rowSize;
        }
        return actualSlot < numSlots ? actualSlot : -1;
    }

    @Override
    public int getVisibleSlot(int actualSlot) {
        if (actualSlot < 0 || actualSlot >= numSlots || rowSize <= 0) return -1;
        if (!filtersRows()) return actualSlot;
        updateVisibleRows();
        int actualRow = actualSlot / rowSize;
        for (int row = 0; row < visibleRows.length; row++) {
            if (visibleRows[row] == actualRow) return row * rowSize + actualSlot % rowSize;
        }
        return -1;
    }

    @ModifyVariable(method = "mouseClicked", at = @At("STORE"), index = 9, require = 1)
    private int mapClickedSlot(int slot) {
        int actual = getActualSlot(slot);
        return actual < 0 ? numSlots : actual;
    }

    @ModifyExpressionValue(
        method = "mouseClicked",
        at = @At(
            value = "FIELD",
            target = "Lappeng/client/gui/implementations/GuiInterfaceTerminal$InterfaceTerminalEntry;guiHeight:I"),
        require = 1)
    private int visibleClickHeight(int original) {
        return getVisibleHeight();
    }
}
