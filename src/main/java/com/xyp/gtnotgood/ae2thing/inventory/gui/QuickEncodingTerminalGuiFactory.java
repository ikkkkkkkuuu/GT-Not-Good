package com.xyp.gtnotgood.ae2thing.inventory.gui;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.ae2thing.api.Constants;
import com.xyp.gtnotgood.ae2thing.quickterminal.ContainerQuickEncodingTerminal;
import com.xyp.gtnotgood.ae2thing.quickterminal.DualTerminalGuiObject;
import com.xyp.gtnotgood.ae2thing.quickterminal.client.GuiQuickEncodingTerminal;

import appeng.api.AEApi;
import appeng.api.features.IWirelessTermHandler;
import appeng.core.localization.PlayerMessages;
import baubles.api.BaublesApi;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Opens the native-AE2 combined interface/pattern terminal for the GT-Not-Cool wireless terminal item. */
public final class QuickEncodingTerminalGuiFactory implements IGuiFactory {

    @Nullable
    private static DualTerminalGuiObject buildHost(EntityPlayer player, World world, int slot) {
        ItemStack item = getItem(player, slot);
        if (item == null || item.getItem() == null) return null;

        IWirelessTermHandler handler = AEApi.instance()
            .registries()
            .wireless()
            .getWirelessTerminalHandler(item);
        if (handler == null) return null;

        // GT-Not-Cool and AE2 use different sentinel offsets for Baubles slots.
        // AEBaseContainer validates the item against AE2's offset every tick, so
        // normalize it before the slot is stored in WirelessTerminalGuiObject.
        int aeSlot = slot;
        if (slot >= Constants.BAUBLE_SLOT_OFFSET) {
            aeSlot = appeng.util.Platform.baublesSlotsOffset + (slot - Constants.BAUBLE_SLOT_OFFSET);
        }
        return new DualTerminalGuiObject(handler, item, player, world, aeSlot);
    }

    @Nullable
    private static ItemStack getItem(EntityPlayer player, int slot) {
        if (slot == -1) return player.getCurrentEquippedItem();
        if (slot >= Constants.BAUBLE_SLOT_OFFSET) {
            net.minecraft.inventory.IInventory baubles = BaublesApi.getBaubles(player);
            int baubleSlot = slot - Constants.BAUBLE_SLOT_OFFSET;
            return baubles != null && baubleSlot >= 0 && baubleSlot < baubles.getSizeInventory()
                ? baubles.getStackInSlot(baubleSlot)
                : null;
        }
        return slot >= 0 && slot < player.inventory.getSizeInventory() ? player.inventory.getStackInSlot(slot) : null;
    }

    @Nullable
    @Override
    public Object createServerGui(EntityPlayer player, World world, int x, int y, int z, ForgeDirection face) {
        DualTerminalGuiObject host = buildHost(player, world, x);
        if (host == null) return null;
        if (host.getActionableNode() == null) {
            player.addChatMessage(PlayerMessages.OutOfRange.get());
            return null;
        }
        return new ContainerQuickEncodingTerminal(player.inventory, host);
    }

    @SideOnly(Side.CLIENT)
    @Nullable
    @Override
    public Object createClientGui(EntityPlayer player, World world, int x, int y, int z, ForgeDirection face) {
        DualTerminalGuiObject host = buildHost(player, world, x);
        if (host == null) {
            if (Minecraft.getMinecraft().currentScreen != null) player.closeScreen();
            return null;
        }
        return new GuiQuickEncodingTerminal(player.inventory, host);
    }
}
