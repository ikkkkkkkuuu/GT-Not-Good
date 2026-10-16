package com.xyp.gtnotgood.mixins.late.journeymap;

import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnotgood.client.journeymap.JourneyMapMenu;

@Pseudo
@Mixin(targets = "journeymap.client.ui.fullscreen.Fullscreen", remap = false)
public abstract class FullscreenMenuMixin extends GuiScreen {

    @Unique
    private JourneyMapMenu journeyMap$menu;

    @Inject(method = { "mouseClicked(III)V", "func_73864_a(III)V" }, at = @At("HEAD"), cancellable = true, require = 0)
    private void journeyMap$mouseClicked(int mouseX, int mouseY, int button, CallbackInfo ci) {
        if (journeyMap$menu == null) {
            if (button != 1) return;
            journeyMap$menu = new JourneyMapMenu(this);
        }
        if (journeyMap$menu.mouseClicked(mouseX, mouseY, button, Mouse.getEventX(), Mouse.getEventY())) ci.cancel();
    }

    @Inject(method = { "keyTyped(CI)V", "func_73869_a(CI)V" }, at = @At("HEAD"), cancellable = true, require = 0)
    private void journeyMap$keyTyped(char character, int key, CallbackInfo ci) {
        if (journeyMap$menu != null && journeyMap$menu.keyTyped(key)) ci.cancel();
    }

    @Inject(method = { "drawScreen(IIF)V", "func_73863_a(IIF)V" }, at = @At("RETURN"), require = 0)
    private void journeyMap$drawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (journeyMap$menu != null) journeyMap$menu.draw(mouseX, mouseY);
    }

    @Inject(method = { "onGuiClosed()V", "func_146281_b()V" }, at = @At("HEAD"), require = 0)
    private void journeyMap$onGuiClosed(CallbackInfo ci) {
        if (journeyMap$menu == null) return;
        journeyMap$menu.close();
        journeyMap$menu = null;
    }
}
