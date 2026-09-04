package com.xyp.gtnotgood.ae2thing.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.ae2thing.api.adapter.terminal.item.TerminalItems;

import appeng.util.Platform;
import baubles.api.BaublesApi;

public class BaublesUtil {

    public static IInventory getBaublesInv(EntityPlayer player) {
        return BaublesApi.getBaubles(player);
    }

    public static boolean isSameItemPrecise(ItemStack is1, ItemStack is2, int slotIndex, TerminalItems terminalItems) {
        // baubles can't sync inv to client side,so i use slot to make sure is same item
        if (Platform.isSameItem(is1, is2)) {
            int slot = terminalItems.getData()
                .getInteger(com.xyp.gtnotgood.ae2thing.api.Constants.SLOT);
            return slotIndex == slot;
        }
        return false;
    }
}
