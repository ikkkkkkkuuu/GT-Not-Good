package com.xyp.gtnotgood.common.machines.storage;

import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;

public final class VaultStackSnapshots {

    private VaultStackSnapshots() {}

    /**
     * Rebuilds AE's mutable item definition instead of sharing it through {@link IAEItemStack#copy()}.
     * Crafting flags do not describe stored inventory and are intentionally omitted.
     *
     * @param stack stored item identity and quantity
     * @return independent item definition with the original long quantity
     */
    public static IAEItemStack item(IAEItemStack stack) {
        return AEItemStack.create(stack.getItemStack()).setStackSize(stack.getStackSize());
    }
}
