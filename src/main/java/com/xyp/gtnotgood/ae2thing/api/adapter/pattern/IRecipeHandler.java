package com.xyp.gtnotgood.ae2thing.api.adapter.pattern;

import java.util.List;

import net.minecraft.inventory.Container;

import com.xyp.gtnotgood.ae2thing.nei.object.OrderStack;
import com.xyp.gtnotgood.ae2thing.network.CPacketTransferRecipe;

@FunctionalInterface
public interface IRecipeHandler {

    void transferPack(Container container, List<OrderStack<?>> inputs, List<OrderStack<?>> outputs, String identifier,
        IPatternTerminalAdapter adapter, CPacketTransferRecipe message);
}
