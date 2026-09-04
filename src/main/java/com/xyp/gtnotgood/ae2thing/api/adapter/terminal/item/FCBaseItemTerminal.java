package com.xyp.gtnotgood.ae2thing.api.adapter.terminal.item;

import java.util.Arrays;
import java.util.List;

import net.minecraft.item.Item;

import com.glodblock.github.common.item.ItemWirelessInterfaceTerminal;
import com.glodblock.github.common.item.ItemWirelessLevelTerminal;
import com.glodblock.github.common.item.ItemWirelessPatternTerminal;

import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.helpers.InventoryAction;

public class FCBaseItemTerminal implements IItemTerminal {

    public static FCBaseItemTerminal instance = new FCBaseItemTerminal();

    @Override
    public List<Class<? extends Item>> getClasses() {
        return Arrays.asList(
            ItemWirelessLevelTerminal.class,
            ItemWirelessInterfaceTerminal.class,
            ItemWirelessPatternTerminal.class);
    }

    @Override
    public void openCraftAmount() {
        NetworkHandler.instance.sendToServer(new PacketInventoryAction(InventoryAction.AUTO_CRAFT, 0, 0L));
    }

}
