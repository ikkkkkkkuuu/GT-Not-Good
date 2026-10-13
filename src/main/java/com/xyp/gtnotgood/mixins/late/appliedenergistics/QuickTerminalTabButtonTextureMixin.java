package com.xyp.gtnotgood.mixins.late.appliedenergistics;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.xyp.gtnotgood.ae2thing.quickterminal.client.GuiQuickEncodingTerminal;
import com.xyp.gtnotgood.ae2thing.quickterminal.client.QuickTerminalButtonTextures;

import appeng.client.gui.widgets.GuiTabButton;

@Mixin(value = GuiTabButton.class, remap = false)
public abstract class QuickTerminalTabButtonTextureMixin {

    @ModifyArg(
        method = "drawButton",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V",
            remap = true),
        index = 0,
        require = 1)
    private ResourceLocation gtnotgood$useModernTabButton(ResourceLocation original) {
        return Minecraft.getMinecraft().currentScreen instanceof GuiQuickEncodingTerminal
            ? QuickTerminalButtonTextures.states()
            : original;
    }
}
