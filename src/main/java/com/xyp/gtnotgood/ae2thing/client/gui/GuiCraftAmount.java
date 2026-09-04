package com.xyp.gtnotgood.ae2thing.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;

import org.lwjgl.input.Mouse;

import com.xyp.gtnotgood.ae2thing.AE2Thing;
import com.xyp.gtnotgood.ae2thing.inventory.gui.GuiType;
import com.xyp.gtnotgood.ae2thing.inventory.item.WirelessDualInterfaceTerminalInventory;
import com.xyp.gtnotgood.ae2thing.loader.ItemAndBlockHolder;
import com.xyp.gtnotgood.ae2thing.network.CPacketCraftRequest;

import appeng.api.config.CraftingMode;
import appeng.api.config.Settings;
import appeng.api.storage.ITerminalHost;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.container.implementations.ContainerCraftAmount;
import appeng.core.localization.GuiText;
import appeng.util.Platform;

public class GuiCraftAmount extends GuiAmount {

    protected GuiImgButton craftingMode;

    public GuiCraftAmount(final InventoryPlayer inventoryPlayer, final ITerminalHost te) {
        super(new ContainerCraftAmount(inventoryPlayer, te));
    }

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.add(
            this.craftingMode = new GuiImgButton(
                this.guiLeft + 10,
                this.guiTop + 53,
                Settings.CRAFTING_MODE,
                CraftingMode.STANDARD));
        this.amountBox.setText("1");
        this.amountBox.setCursorPositionEnd();
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.fontRendererObj.drawString(GuiText.SelectAmount.getLocal(), 8, 6, 0x404040);
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        super.drawBG(offsetX, offsetY, mouseX, mouseY);
        this.submit.displayString = isShiftKeyDown() ? GuiText.Start.getLocal() : GuiText.Next.getLocal();
        try {
            this.submit.enabled = getAmount() > 0;
        } catch (final NumberFormatException e) {
            this.submit.enabled = false;
        }
        this.amountBox.drawTextBox();
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        super.actionPerformed(btn);
        try {
            if (btn == this.craftingMode) {
                GuiImgButton iBtn = (GuiImgButton) btn;

                final Enum cv = iBtn.getCurrentValue();
                final boolean backwards = Mouse.isButtonDown(1);
                final Enum next = Platform.rotateEnum(
                    cv,
                    backwards,
                    iBtn.getSetting()
                        .getPossibleValues());

                iBtn.set(next);
            }
            if (btn == this.submit && this.submit.enabled) {
                AE2Thing.proxy.netHandler.sendToServer(
                    new CPacketCraftRequest(
                        getAmount(),
                        isShiftKeyDown(),
                        (CraftingMode) this.craftingMode.getCurrentValue()));
            }
        } catch (final NumberFormatException e) {
            this.amountBox.setText("1");
        }
    }

    @Override
    protected void setOriginGUI(Object target) {
        if (target instanceof WirelessDualInterfaceTerminalInventory) {
            this.myIcon = ItemAndBlockHolder.ITEM_WIRELESS_DUAL_INTERFACE_TERMINAL.stack();
            this.originalGui = GuiType.WIRELESS_DUAL_INTERFACE_TERMINAL;
        }
    }

    /**
     * The craft-amount screen for our terminal is always item-hosted; the view it was launched from (dual interface vs
     * AE2 wireless crafting terminal) is persisted on the terminal NBT. Route the back navigation through the server's
     * "restore last" path rather than the hard-coded {@link GuiType#WIRELESS_DUAL_INTERFACE_TERMINAL}, so it returns to
     * the correct view — and works for a baubles-slot terminal whose NBT the client can't read reliably.
     */
    @Override
    protected void switchToOriginalGui() {
        AE2Thing.proxy.netHandler.sendToServer(com.xyp.gtnotgood.ae2thing.network.CPacketSwitchGuis.restoreLast());
    }

    @Override
    protected String getBackground() {
        return "guis/craftAmt.png";
    }

    @Override
    public void setAmount(int amount) {
        this.amountBox.setText(String.valueOf(amount));
        this.amountBox.setCursorPositionEnd();
    }
}
