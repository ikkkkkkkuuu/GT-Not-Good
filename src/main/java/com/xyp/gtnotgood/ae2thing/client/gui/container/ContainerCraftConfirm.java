package com.xyp.gtnotgood.ae2thing.client.gui.container;

import java.util.Objects;

import net.minecraft.entity.player.InventoryPlayer;

import com.xyp.gtnotgood.ae2thing.common.parts.THPart;
import com.xyp.gtnotgood.ae2thing.inventory.InventoryHandler;
import com.xyp.gtnotgood.ae2thing.inventory.gui.GuiType;
import com.xyp.gtnotgood.ae2thing.inventory.item.WirelessDualInterfaceTerminalInventory;
import com.xyp.gtnotgood.ae2thing.inventory.item.WirelessTerminal;
import com.xyp.gtnotgood.ae2thing.util.BlockPos;

import appeng.api.networking.security.IActionHost;
import appeng.api.storage.ITerminalHost;

public class ContainerCraftConfirm extends appeng.container.implementations.ContainerCraftConfirm {

    public ContainerCraftConfirm(final InventoryPlayer ip, final ITerminalHost te) {
        super(ip, te);
    }

    @Override
    public void switchToOriginalGUI() {
        GuiType originalGui = null;

        final IActionHost ah = this.getActionHost();
        if (ah instanceof WirelessDualInterfaceTerminalInventory) {
            // Return to whichever terminal view the craft was launched from (dual interface vs AE2 wireless crafting
            // terminal), not always the dual interface terminal. The current view is persisted on the terminal NBT;
            // read it here server-side (authoritative) so a baubles-slot terminal resolves correctly too.
            net.minecraft.item.ItemStack stack = com.xyp.gtnotgood.ae2thing.util.Util
                .getTerminalInSlot(this.getInventoryPlayer().player, ((WirelessTerminal) ah).getInventorySlot());
            originalGui = com.xyp.gtnotgood.ae2thing.util.Util
                .getLastGuiMode(stack, GuiType.WIRELESS_DUAL_INTERFACE_TERMINAL);
        }

        if (this.getOpenContext() != null && ah instanceof THPart) {
            InventoryHandler.openGui(
                this.getInventoryPlayer().player,
                getWorld(),
                new BlockPos(
                    this.getOpenContext()
                        .getTile()),
                Objects.requireNonNull(
                    this.getOpenContext()
                        .getSide()),
                originalGui);
        } else if (ah instanceof WirelessTerminal) {
            InventoryHandler.openGui(
                this.getInventoryPlayer().player,
                getWorld(),
                new BlockPos(((WirelessTerminal) ah).getInventorySlot(), 0, 0),
                Objects.requireNonNull(
                    this.getOpenContext()
                        .getSide()),
                originalGui);
        }
    }
}
