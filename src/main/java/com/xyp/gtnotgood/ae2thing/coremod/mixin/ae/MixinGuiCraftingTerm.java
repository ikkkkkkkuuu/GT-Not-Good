package com.xyp.gtnotgood.ae2thing.coremod.mixin.ae;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnotgood.ae2thing.AE2Thing;
import com.xyp.gtnotgood.ae2thing.client.gui.widget.CompactItemTabButton;
import com.xyp.gtnotgood.ae2thing.common.item.ItemWirelessDualInterfaceTerminal;
import com.xyp.gtnotgood.ae2thing.inventory.gui.GuiType;
import com.xyp.gtnotgood.ae2thing.network.CPacketSwitchGuis;

import appeng.api.storage.ITerminalHost;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.implementations.GuiCraftingTerm;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.container.implementations.ContainerMEMonitorable;
import appeng.helpers.WirelessTerminalGuiObject;

/**
 * Adds a "switch back to the ME Wireless Dual Interface Terminal" button to the left side of AE2's crafting terminal
 * GUI, but only when that GUI was opened from our dual interface terminal (i.e. its host is a wireless crafting
 * terminal wrapping an {@link ItemWirelessDualInterfaceTerminal}). This is the return path of the button added to the
 * dual interface terminal's pattern panel; see {@link GuiType#WIRELESS_CRAFTING_TERMINAL}.
 */
@Mixin(GuiCraftingTerm.class)
public abstract class MixinGuiCraftingTerm extends AEBaseGui {

    private CompactItemTabButton switchBackButton = null;

    public MixinGuiCraftingTerm(Container container) {
        super(container);
    }

    private boolean openedFromDualInterfaceTerminal() {
        if (!(this.inventorySlots instanceof ContainerMEMonitorable)) return false;
        ITerminalHost host = ((AccessorContainerMEMonitorable) this.inventorySlots).getHost();
        if (!(host instanceof WirelessTerminalGuiObject wireless)) return false;
        ItemStack terminal = wireless.getItemStack();
        return terminal != null && terminal.getItem() instanceof ItemWirelessDualInterfaceTerminal;
    }

    @Inject(method = "initGui", at = @At("TAIL"))
    public void gtnotgood$addSwitchBackButton(CallbackInfo ci) {
        if (!openedFromDualInterfaceTerminal()) return;
        // Append both controls to AE2's existing left-side column. Deriving the
        // next position from the live buttons keeps the layout valid for every
        // terminal height and terminal-style setting.
        int x = this.guiLeft - 18;
        int y = this.guiTop;
        GuiImgButton pinsButton = null;
        for (Object o : this.buttonList) {
            if (o instanceof GuiImgButton pinsBtn && pinsBtn.getSetting() == appeng.api.config.Settings.ACTIONS
                && pinsBtn.getCurrentValue() == appeng.api.config.ActionItems.PINS) {
                pinsButton = pinsBtn;
            } else if (o instanceof GuiButton button && button.visible && button.xPosition == x) {
                y = Math.max(y, button.yPosition + 20);
            }
        }
        if (pinsButton != null) {
            pinsButton.xPosition = x;
            pinsButton.yPosition = y;
            y += 20;
        }
        // #tr gtnotgood.tooltip.switch_to_dual_interface_terminal
        // # Switch to Dual Interface Terminal
        // # zh_CN 切换到二合一接口终端
        this.switchBackButton = new CompactItemTabButton(
            new ItemStack(Blocks.crafting_table),
            I18n.format("gtnotgood.tooltip.switch_to_dual_interface_terminal"),
            AEBaseGui.aeRenderItem);
        this.switchBackButton.xPosition = x;
        this.switchBackButton.yPosition = y;
        this.buttonList.add(this.switchBackButton);
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"))
    private void gtnotgood$onSwitchBack(GuiButton btn, CallbackInfo ci) {
        if (this.switchBackButton != null && btn == this.switchBackButton) {
            AE2Thing.proxy.netHandler.sendToServer(new CPacketSwitchGuis(GuiType.WIRELESS_DUAL_INTERFACE_TERMINAL));
        }
    }
}
