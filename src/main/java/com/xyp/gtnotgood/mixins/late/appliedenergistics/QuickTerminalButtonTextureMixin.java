package com.xyp.gtnotgood.mixins.late.appliedenergistics;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.xyp.gtnotgood.ae2thing.quickterminal.client.GuiQuickEncodingTerminal;
import com.xyp.gtnotgood.ae2thing.quickterminal.client.QuickTerminalButtonTextures;

import appeng.client.gui.widgets.GuiImgButton;

@Mixin(value = GuiImgButton.class, remap = false)
public abstract class QuickTerminalButtonTextureMixin {

    @ModifyArg(
        method = "drawButton",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V",
            remap = true),
        index = 0,
        require = 1)
    private ResourceLocation gtnotgood$useModernTerminalButtons(ResourceLocation original) {
        return Minecraft.getMinecraft().currentScreen instanceof GuiQuickEncodingTerminal
            ? QuickTerminalButtonTextures.states()
            : original;
    }
}
