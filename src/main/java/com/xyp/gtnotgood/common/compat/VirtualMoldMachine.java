package com.xyp.gtnotgood.common.compat;

import net.minecraft.item.ItemStack;

/** Separate, non-extractable configuration storage added to GT electric singleblock machines by a late mixin. */
public interface VirtualMoldMachine {

    /** @return a disposable zero-size catalyst stack, or null when no mold is selected */
    ItemStack gtng$getVirtualMold();

    /** Sets configuration only; implementations copy and validate against the shared mold catalog. */
    void gtng$setVirtualMold(ItemStack mold);
}
