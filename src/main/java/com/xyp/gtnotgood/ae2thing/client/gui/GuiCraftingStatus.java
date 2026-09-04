package com.xyp.gtnotgood.ae2thing.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.ae2thing.inventory.InventoryHandler;
import com.xyp.gtnotgood.ae2thing.inventory.gui.GuiType;
import com.xyp.gtnotgood.ae2thing.inventory.item.WirelessDualInterfaceTerminalInventory;
import com.xyp.gtnotgood.ae2thing.loader.ItemAndBlockHolder;
import com.xyp.gtnotgood.ae2thing.util.Ae2ReflectClient;

import appeng.api.storage.ITerminalHost;
import appeng.client.gui.widgets.GuiTabButton;

public class GuiCraftingStatus extends appeng.client.gui.implementations.GuiCraftingStatus {

    private GuiTabButton originalGuiBtn;
    private final ITerminalHost host;

    public GuiCraftingStatus(InventoryPlayer inventoryPlayer, ITerminalHost te) {
        super(inventoryPlayer, te);
        host = te;
    }

    @Override
    public void initGui() {
        super.initGui();
        originalGuiBtn = Ae2ReflectClient.getOriginalGuiButton(this);
        // AE2 977 only builds the return button when a PrimaryGui icon is present, which is driven by the server
        // calling
        // setPrimaryGui. We open the crafting-status view via CPacketSwitchGuis(CRAFTING_STATUS_ITEM) without a
        // PrimaryGui, so the parent never creates it. Add our own return button (icon = the dual interface terminal),
        // mirroring MixinGuiCraftingTerm's client-side button; actionPerformed below already handles the switch-back.
        if (originalGuiBtn == null && host instanceof WirelessDualInterfaceTerminalInventory) {
            ItemStack icon = ItemAndBlockHolder.ITEM_WIRELESS_DUAL_INTERFACE_TERMINAL.stack();
            this.originalGuiBtn = new GuiTabButton(
                this.guiLeft + this.xSize - 25,
                this.guiTop - 4,
                icon,
                icon.getDisplayName(),
                this.itemRender);
            this.originalGuiBtn.setHideEdge(13);
            this.buttonList.add(this.originalGuiBtn);
        }
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        if (btn == originalGuiBtn) {
            if (host instanceof WirelessDualInterfaceTerminalInventory) {
                InventoryHandler.switchGui(GuiType.WIRELESS_DUAL_INTERFACE_TERMINAL);
            }
        } else {
            super.actionPerformed(btn);
        }
    }
}
