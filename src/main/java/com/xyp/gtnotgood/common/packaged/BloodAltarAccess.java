package com.xyp.gtnotgood.common.packaged;

import net.minecraft.item.ItemStack;

/** Persistent completion stop installed only on Blood Magic altars by the optional late mixin. */
public interface BloodAltarAccess {

    /** Records the exact result to hold, or clears the stop with null. No items are created by this marker. */
    void gtnotgood$holdResult(ItemStack expected);
}
