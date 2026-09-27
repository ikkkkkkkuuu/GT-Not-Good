package com.xyp.gtnotgood.ae2thing.api;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.ae2thing.AE2Thing;
import com.xyp.gtnotgood.ae2thing.api.adapter.terminal.item.DualInterfaceTerminal;
import com.xyp.gtnotgood.ae2thing.api.adapter.terminal.item.FCBaseItemTerminal;
import com.xyp.gtnotgood.ae2thing.api.adapter.terminal.item.FCUltraTerminal;
import com.xyp.gtnotgood.ae2thing.api.adapter.terminal.item.IItemTerminal;
import com.xyp.gtnotgood.ae2thing.api.adapter.terminal.item.TerminalItems;
import com.xyp.gtnotgood.ae2thing.network.CPacketOpenTerminal;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import lombok.Getter;

@SideOnly(Side.CLIENT)
public class TerminalMenu {

    public static List<IItemTerminal> terminalHandlers = new ArrayList<>();
    @Getter
    private final List<TerminalItems> terminalItems = new ArrayList<>();
    @Getter
    private final List<ItemStack> items = new ArrayList<>();

    public TerminalMenu() {
        for (IItemTerminal handler : terminalHandlers) {
            terminalItems.addAll(handler.getTerminalItems());
        }
        for (TerminalItems t : terminalItems) {
            items.add(t.getTargetItem());
        }
    }

    public void openTerminal(int index) {
        try {
            if (index < 0 || index >= terminalItems.size()) {
                return;
            }
            TerminalItems terminal = terminalItems.get(index);
            AE2Thing.proxy.netHandler.sendToServer(new CPacketOpenTerminal(terminal));
        } catch (Exception ignored) {}
    }

    static {
        terminalHandlers.add(FCUltraTerminal.instance);
        terminalHandlers.add(FCBaseItemTerminal.instance);
        terminalHandlers.add(DualInterfaceTerminal.instance);
    }
}
