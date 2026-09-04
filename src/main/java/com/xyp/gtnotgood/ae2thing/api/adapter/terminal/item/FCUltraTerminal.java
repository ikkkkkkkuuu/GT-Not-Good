package com.xyp.gtnotgood.ae2thing.api.adapter.terminal.item;

import static com.xyp.gtnotgood.ae2thing.nei.NEI_TH_Config.getConfigValue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.glodblock.github.common.item.ItemBaseWirelessTerminal;
import com.glodblock.github.common.item.ItemWirelessUltraTerminal;
import com.glodblock.github.util.UltraTerminalModes;
import com.xyp.gtnotgood.ae2thing.api.Constants;
import com.xyp.gtnotgood.ae2thing.nei.ButtonConstants;

import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.helpers.InventoryAction;

public class FCUltraTerminal implements IItemTerminal {

    public static FCUltraTerminal instance = new FCUltraTerminal();

    @Override
    public List<Class<? extends Item>> getClasses() {
        List<Class<? extends Item>> classes = new ArrayList<>();
        classes.add(ItemWirelessUltraTerminal.class);
        return classes;
    }

    @Override
    public boolean supportBaubles() {
        return true;
    }

    @Override
    public List<TerminalItems> getMainInvTerminals() {
        List<TerminalItems> terminal = new ArrayList<>();
        for (int i = 0; i < player().inventory.mainInventory.length; ++i) {
            ItemStack item = player().inventory.getStackInSlot(i);
            terminal.addAll(getTerminalItems(item, i));
        }
        return terminal;
    }

    private List<TerminalItems> getTerminalItems(ItemStack source, int slot) {
        List<TerminalItems> terminal = new ArrayList<>();
        if (source != null && source.getItem() instanceof ItemWirelessUltraTerminal) {
            NBTTagCompound tag = this.newNBT();
            tag.setInteger(Constants.SLOT, slot);
            if (getConfigValue(ButtonConstants.ULTRA_TERMINAL_MODE)) {
                for (UltraTerminalModes mode : UltraTerminalModes.values()) {
                    ItemStack t = source.copy();
                    if (!t.hasTagCompound()) {
                        t.setTagCompound(new NBTTagCompound());
                    }
                    ItemBaseWirelessTerminal.setMode(t, mode);
                    terminal.add(new TerminalItems(source, t, t.getDisplayName(), tag));
                }
            } else {
                terminal.add(new TerminalItems(source, source, tag));
            }
        }
        return terminal;
    }

    @Override
    public List<TerminalItems> getBaublesInvTerminals(IInventory handler) {
        List<TerminalItems> terminal = new ArrayList<>();
        for (int i = 0; i < handler.getSizeInventory(); ++i) {
            ItemStack item = handler.getStackInSlot(i);
            terminal.addAll(getTerminalItems(item, i));
        }
        return terminal;
    }

    @Override
    public void openCraftAmount() {
        NetworkHandler.instance.sendToServer(new PacketInventoryAction(InventoryAction.AUTO_CRAFT, 0, 0L));
    }

}
