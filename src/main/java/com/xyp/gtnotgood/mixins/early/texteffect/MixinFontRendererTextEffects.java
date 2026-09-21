// Ported from ABKQPO/GT-Not-Leisure @ 52345d2; LGPL-3.0. See META-INF/text-effects-port/.
package com.xyp.gtnotgood.mixins.early.texteffect;

import java.util.List;

import net.minecraft.client.gui.FontRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.client.text.EffectTextLayout;
import com.xyp.gtnotgood.client.text.EffectTextRenderer;

/** Runs after low-priority font overwrites, but before other draw callbacks. */
@Mixin(value = FontRenderer.class, priority = 100)
public abstract class MixinFontRendererTextEffects {

    // Font replacements may handle these overloads without calling the vanilla implementation below them.
    @Inject(method = "drawString(Ljava/lang/String;III)I", at = @At("HEAD"), cancellable = true, order = 900)
    private void gtng$drawEffectText(String text, int x, int y, int color, CallbackInfoReturnable<Integer> cir) {
        if (EffectTextRenderer.handles(text)) cir
            .setReturnValue(EffectTextRenderer.INSTANCE.draw((FontRenderer) (Object) this, text, x, y, color, false));
    }

    @Inject(method = "drawStringWithShadow", at = @At("HEAD"), cancellable = true, order = 900)
    private void gtng$drawShadowedEffectText(String text, int x, int y, int color,
        CallbackInfoReturnable<Integer> cir) {
        if (EffectTextRenderer.handles(text))
            cir.setReturnValue(EffectTextRenderer.INSTANCE.draw((FontRenderer) (Object) this, text, x, y, color, true));
    }

    @Inject(method = "drawString(Ljava/lang/String;IIIZ)I", at = @At("HEAD"), cancellable = true, order = 900)
    private void gtng$drawEffectText(String text, int x, int y, int color, boolean shadow,
        CallbackInfoReturnable<Integer> cir) {
        if (EffectTextRenderer.handles(text)) {
            cir.setReturnValue(
                EffectTextRenderer.INSTANCE.draw((FontRenderer) (Object) this, text, x, y, color, shadow));
        }
    }

    @Inject(method = "renderString", at = @At("HEAD"), cancellable = true, order = 900)
    private void gtng$renderEffectText(String text, int x, int y, int color, boolean shadow,
        CallbackInfoReturnable<Integer> cir) {
        if (EffectTextRenderer.handles(text)) {
            cir.setReturnValue(
                EffectTextRenderer.INSTANCE.draw((FontRenderer) (Object) this, text, x, y, color, shadow));
        }
    }

    @Inject(method = "getStringWidth", at = @At("HEAD"), cancellable = true, order = 900)
    private void gtng$measureEffectText(String text, CallbackInfoReturnable<Integer> cir) {
        if (EffectTextRenderer.handles(text)) {
            cir.setReturnValue(
                (int) Math.ceil(
                    EffectTextRenderer.INSTANCE.layout((FontRenderer) (Object) this, text)
                        .width()));
        }
    }

    @Inject(
        method = "trimStringToWidth(Ljava/lang/String;I)Ljava/lang/String;",
        at = @At("HEAD"),
        cancellable = true,
        order = 900)
    private void gtng$trimEffectText(String text, int width, CallbackInfoReturnable<String> cir) {
        if (EffectTextRenderer.handles(text))
            cir.setReturnValue(EffectTextLayout.trim((FontRenderer) (Object) this, text, width, false));
    }

    @Inject(
        method = "trimStringToWidth(Ljava/lang/String;IZ)Ljava/lang/String;",
        at = @At("HEAD"),
        cancellable = true,
        order = 900)
    private void gtng$trimEffectText(String text, int width, boolean reverse, CallbackInfoReturnable<String> cir) {
        if (EffectTextRenderer.handles(text))
            cir.setReturnValue(EffectTextLayout.trim((FontRenderer) (Object) this, text, width, reverse));
    }

    @Inject(method = "sizeStringToWidth", at = @At("HEAD"), cancellable = true, order = 900)
    private void gtng$effectSourceLength(String text, int width, CallbackInfoReturnable<Integer> cir) {
        if (EffectTextRenderer.handles(text))
            cir.setReturnValue(EffectTextLayout.sizeToWidth((FontRenderer) (Object) this, text, width));
    }

    @Inject(method = "getFormatFromString", at = @At("HEAD"), cancellable = true, order = 900)
    private static void gtng$effectContinuation(String text, CallbackInfoReturnable<String> cir) {
        if (EffectTextRenderer.handles(text)) cir.setReturnValue(EffectTextLayout.continuation(text));
    }

    @Inject(method = "listFormattedStringToWidth", at = @At("HEAD"), cancellable = true, order = 900)
    private void gtng$wrapEffectText(String text, int width, CallbackInfoReturnable<List<String>> cir) {
        if (EffectTextRenderer.handles(text))
            cir.setReturnValue(EffectTextLayout.wrap((FontRenderer) (Object) this, text, width));
    }

    @Inject(method = "wrapFormattedStringToWidth", at = @At("HEAD"), cancellable = true, order = 900)
    private void gtng$wrapEffectString(String text, int width, CallbackInfoReturnable<String> cir) {
        if (EffectTextRenderer.handles(text))
            cir.setReturnValue(String.join("\n", EffectTextLayout.wrap((FontRenderer) (Object) this, text, width)));
    }

    @Inject(method = "drawSplitString", at = @At("HEAD"), cancellable = true, order = 900)
    private void gtng$drawWrappedEffect(String text, int x, int y, int width, int color, CallbackInfo ci) {
        if (!EffectTextRenderer.handles(text)) return;
        FontRenderer font = (FontRenderer) (Object) this;
        float lineY = y;
        for (String line : EffectTextLayout.wrap(font, text, width)) {
            EffectTextRenderer.INSTANCE.draw(font, line, x, lineY, color, false);
            lineY += EffectTextLayout.fontHeight(font);
        }
        ci.cancel();
    }

    @Inject(method = "splitStringWidth", at = @At("HEAD"), cancellable = true, order = 900)
    private void gtng$wrappedHeight(String text, int width, CallbackInfoReturnable<Integer> cir) {
        if (!EffectTextRenderer.handles(text)) return;
        FontRenderer font = (FontRenderer) (Object) this;
        cir.setReturnValue(
            (int) Math.ceil(
                EffectTextLayout.wrap(font, text, width)
                    .size() * EffectTextLayout.fontHeight(font)));
    }
}
