// Ported from ABKQPO/GT-Not-Leisure @ 52345d2; LGPL-3.0. See META-INF/text-effects-port/.
package com.xyp.gtnotgood.mixins.late.texteffect;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xyp.gtnotgood.utils.text.effect.EffectTextParser;

import codechicken.nei.FormattedTextField;

/** Keeps editable declarations intact and shares the source-aware vanilla text field renderer. */
@Mixin(value = FormattedTextField.class, remap = false)
public abstract class MixinNEIFormattedTextField extends GuiTextField {

    public MixinNEIFormattedTextField(FontRenderer font, int x, int y, int width, int height) {
        super(font, x, y, width, height);
    }

    @WrapOperation(
        method = "setText",
        remap = true,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/EnumChatFormatting;getTextWithoutFormattingCodes(Ljava/lang/String;)Ljava/lang/String;",
            remap = true))
    private String gtng$preserveEditableEffects(String value, Operation<String> original) {
        return EffectTextParser.containsMarkers(value) ? value : original.call(value);
    }

    @Inject(method = "drawTextBox", remap = true, at = @At("HEAD"), cancellable = true)
    private void gtng$drawEffectInput(CallbackInfo ci) {
        if (EffectTextParser.containsMarkers(getText())) {
            super.drawTextBox();
            ci.cancel();
        }
    }
}
