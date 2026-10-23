// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtnotgood.mixins.late.appliedenergistics.quantum;

import net.minecraft.client.gui.GuiButton;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.client.quantum.QuantumCraftingClientConfig;
import com.xyp.gtnotgood.client.quantum.QuantumCraftingModeButton;
import com.xyp.gtnotgood.common.crafting.quantum.ae2.IQuantumCraftAmountGui;
import com.xyp.gtnotgood.common.network.quantum.QuantumCraftingModePacket;

import appeng.client.gui.implementations.GuiAmount;
import appeng.client.gui.implementations.GuiCraftAmount;
import appeng.client.gui.widgets.GuiToggleButton;
import appeng.core.AEConfig;

@Mixin(value = GuiCraftAmount.class, remap = false)
public abstract class MixinGuiCraftAmount extends GuiAmount implements IQuantumCraftAmountGui {

    @Shadow(remap = false)
    private GuiToggleButton liteMode;

    @Shadow(remap = false)
    private boolean isControlButtonPressed;

    @Unique
    private QuantumCraftingModeButton gtng$quantumMode;

    @Unique
    private boolean gtng$quantumAvailable;

    public MixinGuiCraftAmount() {
        super(null);
    }

    @Inject(method = "initGui", at = @At("TAIL"), remap = true)
    private void gtng$addQuantumModeButton(CallbackInfo ci) {
        this.gtng$quantumMode = new QuantumCraftingModeButton(this.guiLeft - 18, this.guiTop + 48);
        this.gtng$quantumMode.setState(QuantumCraftingClientConfig.isQuantumCraftingMode());
        this.buttonList.add(this.gtng$quantumMode);
        this.gtng$updateQuantumModeVisibility();
        // The answer tells whether the network can run quantum jobs
        GTNotGood.channel.sendToServer(new QuantumCraftingModePacket(this.gtng$quantumMode.getState()));
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true, remap = true)
    private void gtng$toggleQuantumMode(GuiButton btn, CallbackInfo ci) {
        if (this.gtng$quantumMode == null) return;

        if (btn == this.gtng$quantumMode) {
            final boolean enabled = !this.gtng$quantumMode.getState();
            if (enabled && AEConfig.instance.getUseLiteCraftingMode()) {
                this.liteMode.setState(AEConfig.instance.toggleUseLiteCraftingMode());
            }
            this.gtng$setQuantumMode(enabled);
            ci.cancel();
        } else if (
            btn == this.liteMode && this.gtng$quantumMode.getState() && !AEConfig.instance.getUseLiteCraftingMode()
        ) {
            // Lite mode is about to turn on
            this.gtng$setQuantumMode(false);
        }
    }

    @Inject(method = "setControlButtonPressed", at = @At("TAIL"))
    private void gtng$hideQuantumModeWhileEditing(boolean pressed, CallbackInfo ci) {
        this.gtng$updateQuantumModeVisibility();
    }

    @Override
    public void gtng$setQuantumAvailable(boolean available) {
        this.gtng$quantumAvailable = available;
        if (
            available && this.gtng$quantumMode != null
                && this.gtng$quantumMode.getState()
                && AEConfig.instance.getUseLiteCraftingMode()
        ) {
            this.liteMode.setState(AEConfig.instance.toggleUseLiteCraftingMode());
        }
        this.gtng$updateQuantumModeVisibility();
    }

    @Unique
    private void gtng$setQuantumMode(boolean enabled) {
        this.gtng$quantumMode.setState(enabled);
        QuantumCraftingClientConfig.setQuantumCraftingMode(enabled);
        GTNotGood.channel.sendToServer(new QuantumCraftingModePacket(enabled));
    }

    @Unique
    private void gtng$updateQuantumModeVisibility() {
        if (this.gtng$quantumMode == null) return;
        final boolean visible = this.gtng$quantumAvailable && !this.isControlButtonPressed;
        this.gtng$quantumMode.visible = visible;
        this.gtng$quantumMode.enabled = visible;
    }
}
