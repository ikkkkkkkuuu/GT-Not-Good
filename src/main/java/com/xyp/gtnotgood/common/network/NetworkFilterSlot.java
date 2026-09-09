package com.xyp.gtnotgood.common.network;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.utils.item.IItemHandlerModifiable;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;

/** Phantom samples accept any item without simulating insertion into the non-consuming filter handler. */
final class NetworkFilterSlot extends ModularSlot {

    NetworkFilterSlot(IItemHandlerModifiable handler, int index) {
        super(handler, index);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return stack != null && stack.stackSize > 0;
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        return 1;
    }
}
