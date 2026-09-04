package com.xyp.gtnotgood.ae2thing.api;

import com.xyp.gtnotgood.ae2thing.AE2Thing;
import com.xyp.gtnotgood.ae2thing.network.CPacketSwitchGuis;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class AE2ThingAPI {

    private static final AE2ThingAPI API = new AE2ThingAPI();

    public static AE2ThingAPI instance() {
        return API;
    }

    public Terminal terminal() {
        return Terminal.API;
    }

    @SideOnly(Side.CLIENT)
    public Pinned getPinned() {
        return Pinned.INSTANCE;
    }

    @SideOnly(Side.CLIENT)
    public void openDualinterfaceTerminal() {
        // Reopen whichever view the player last switched to on the terminal, defaulting to the dual interface terminal.
        // Let the server resolve the saved mode from the terminal's authoritative NBT: when the terminal sits in a
        // Baubles slot the server-side NBT change is not synced back to the client, so reading it here would always
        // see the stale (default) value and, worse, send that stale value back and overwrite the real saved mode.
        AE2Thing.proxy.netHandler.sendToServer(CPacketSwitchGuis.restoreLast());
    }
}
