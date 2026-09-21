// Ported from ABKQPO/GT-Not-Leisure @ 52345d2; LGPL-3.0. See META-INF/text-effects-port/.
package com.xyp.gtnotgood.mixins.early.texteffect;

import net.minecraft.util.EnumChatFormatting;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.xyp.gtnotgood.utils.text.effect.EffectTextParser;
import com.xyp.gtnotgood.utils.text.effect.TextEffects;

/** Removes effect declarations wherever vanilla explicitly requests unformatted text. */
@Mixin(EnumChatFormatting.class)
public abstract class MixinEnumChatFormattingTextEffects {

    @ModifyVariable(method = "getTextWithoutFormattingCodes", at = @At("HEAD"), argsOnly = true)
    private static String gtng$stripEffects(String text) {
        return EffectTextParser.containsMarkers(text) ? TextEffects.plainText(text) : text;
    }
}
