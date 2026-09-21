// Ported from ABKQPO/GT-Not-Leisure @ 52345d2; LGPL-3.0. See META-INF/text-effects-port/.
package com.xyp.gtnotgood.mixins.early.texteffect;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xyp.gtnotgood.client.text.EffectTextFieldView;
import com.xyp.gtnotgood.client.text.EffectTextLayout.Layout;
import com.xyp.gtnotgood.client.text.EffectTextRenderer;
import com.xyp.gtnotgood.utils.text.effect.EffectTextParser;

/** Retains formatting while vanilla scrolls and draws either side of an input cursor. */
@Mixin(GuiTextField.class)
public abstract class MixinGuiTextFieldTextEffects {

    @Shadow
    @Final
    private FontRenderer field_146211_a;

    @Shadow
    private String text;

    @Shadow
    private int lineScrollOffset;

    @Shadow
    private int cursorPosition;

    @Unique
    private EffectTextFieldView gtng$effectView;

    @Unique
    private EffectTextFieldView gtng$view() {
        if (!EffectTextParser.containsMarkers(text)) {
            gtng$effectView = null;
            return null;
        }
        Layout layout = EffectTextRenderer.INSTANCE.layout(field_146211_a, text);
        if (gtng$effectView == null || !gtng$effectView.matches(text, layout))
            gtng$effectView = new EffectTextFieldView(text, layout);
        return gtng$effectView;
    }

    @WrapOperation(
        method = { "drawTextBox", "mouseClicked", "setSelectionPos" },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;trimStringToWidth(Ljava/lang/String;I)Ljava/lang/String;"))
    private String gtng$trimVisibleRange(FontRenderer font, String value, int width, Operation<String> original) {
        EffectTextFieldView view = gtng$view();
        return view == null ? original.call(font, value, width)
            : view.trim(lineScrollOffset, lineScrollOffset + value.length(), width, false);
    }

    @WrapOperation(
        method = "setSelectionPos",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;trimStringToWidth(Ljava/lang/String;IZ)Ljava/lang/String;"))
    private String gtng$trimScrollRange(FontRenderer font, String value, int width, boolean reverse,
        Operation<String> original) {
        EffectTextFieldView view = gtng$view();
        return view == null ? original.call(font, value, width, reverse) : view.trim(0, value.length(), width, reverse);
    }

    @WrapOperation(
        method = "drawTextBox",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;III)I",
            ordinal = 0))
    private int gtng$drawBeforeCursor(FontRenderer font, String value, int x, int y, int color,
        Operation<Integer> original) {
        EffectTextFieldView view = gtng$view();
        return original.call(
            font,
            view == null ? value : view.slice(lineScrollOffset, lineScrollOffset + value.length()),
            x,
            y,
            color);
    }

    @WrapOperation(
        method = "drawTextBox",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;III)I",
            ordinal = 1))
    private int gtng$drawAfterCursor(FontRenderer font, String value, int x, int y, int color,
        Operation<Integer> original) {
        EffectTextFieldView view = gtng$view();
        return original.call(
            font,
            view == null ? value : view.slice(cursorPosition, cursorPosition + value.length()),
            x,
            y,
            color);
    }

    @WrapOperation(
        method = "drawTextBox",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;getStringWidth(Ljava/lang/String;)I"))
    private int gtng$measureSelection(FontRenderer font, String value, Operation<Integer> original) {
        EffectTextFieldView view = gtng$view();
        return view == null ? original.call(font, value)
            : view.width(lineScrollOffset, lineScrollOffset + value.length());
    }
}
