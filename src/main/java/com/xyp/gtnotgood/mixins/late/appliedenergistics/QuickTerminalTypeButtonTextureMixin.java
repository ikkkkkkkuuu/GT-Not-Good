package com.xyp.gtnotgood.mixins.late.appliedenergistics;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.xyp.gtnotgood.ae2thing.quickterminal.client.GuiQuickEncodingTerminal;
import com.xyp.gtnotgood.ae2thing.quickterminal.client.QuickTerminalButtonTextures;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.client.gui.widgets.TypeToggleButton;

@Mixin(value = TypeToggleButton.class, remap = false)
public abstract class QuickTerminalTypeButtonTextureMixin {

    private static final ResourceLocation AE2_STATES = ModList.AE2.getResourceLocation("textures/guis/states.png");

    @ModifyArg(
        method = "drawButton",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V",
            remap = true),
        index = 0,
        require = 2)
    private ResourceLocation gtnotgood$useModernTypeButton(ResourceLocation original) {
        return Minecraft.getMinecraft().currentScreen instanceof GuiQuickEncodingTerminal && AE2_STATES.equals(original)
            ? QuickTerminalButtonTextures.states()
            : original;
    }
}
