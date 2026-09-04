package com.xyp.gtnotgood.ae2thing.coremod.mixin.ae;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnotgood.ae2thing.api.TerminalMenu;

import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.container.slot.SlotPlayerHotBar;
import appeng.container.slot.SlotPlayerInv;
import codechicken.nei.recipe.StackInfo;

@Mixin(value = AEBaseGui.class)
public abstract class MixinAEBaseGui extends GuiContainer {

    public MixinAEBaseGui(Container container) {
        super(container);
    }

    @Shadow(remap = false)
    protected abstract GuiScrollbar getScrollBar();

    @Inject(method = "handleMouseClick", at = @At(value = "HEAD"), cancellable = true)
    protected void handleMouseClick(Slot slot, int slotIdx, int ctrlDown, int mouseButton, CallbackInfo ci) {
        if (ctrlDown == 1 && mouseButton == 0
            && (slot instanceof SlotPlayerInv || slot instanceof SlotPlayerHotBar)
            && slot.getHasStack()) {
            ItemStack item = slot.getStack();
            TerminalMenu menu = new TerminalMenu();
            for (int i = 0; i < menu.getItems()
                .size(); i++) {
                ItemStack term = menu.getItems()
                    .get(i);
                if (StackInfo.equalItemAndNBT(term, item, true)) {
                    menu.openTerminal(i);
                    ci.cancel();
                    break;
                }
            }

        }
    }

    @Inject(method = "handleMouseInput", at = @At("HEAD"))
    public void handleMouseInput(CallbackInfo ci) {
        if (this.getScrollBar() != null) {
            if (!Mouse.isButtonDown(0)) {
                ((AccessorGuiScrollbar) this.getScrollBar()).setIsLatestClickOnScrollbar(false);
            }
        }
    }
}
