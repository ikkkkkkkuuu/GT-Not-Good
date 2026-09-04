package com.xyp.gtnotgood.ae2thing.api.adapter.pattern;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;

import appeng.helpers.IContainerCraftingPacket;

public class FCPatternTerminal implements IPatternTerminalAdapter {

    private final Class<? extends Container> container;

    public FCPatternTerminal(Class<? extends Container> containerClass) {
        this.container = containerClass;
    }

    @Override
    public boolean supportFluid() {
        return true;
    }

    @Override
    public Class<? extends Container> getContainer() {
        return this.container;
    }

    @Override
    public IInventory getInventoryByName(Container container, String name) {
        if (container instanceof IContainerCraftingPacket c) {
            return c.getInventoryByName(name);
        }
        return null;
    }

}
