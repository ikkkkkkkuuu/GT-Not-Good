package com.xyp.gtnotgood.utils.text;

import net.minecraft.launchwrapper.Launch;

import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.text.effect.TextEffectStyle;
import com.xyp.gtnotgood.utils.text.effect.TextEffects;

/** Selects the installed text engine without loading optional client classes. */
public final class TextEffectsCompat {

    private static final String localNamespace = ModList.GTNotGood.getID() + ":";
    private static final String upstreamNamespace = ModList.GTNotLeisure.getID() + ":";

    private TextEffectsCompat() {}

    public static boolean hasUpstreamRenderer() {
        return Launch.classLoader != null
            && Launch.classLoader.getResource("com/science/gtnl/client/text/EffectTextRenderer.class") != null;
    }

    /**
     * Encodes a display span for the active engine while keeping saved preferences in this mod's namespace.
     * GTNL owns the font hooks when present, so its registry must also own the displayed preset identifier.
     *
     * @param text  visible text, including native formatting codes
     * @param style preset and visual parameters stored by this mod
     * @return span understood by the installed renderer
     */
    public static String apply(String text, TextEffectStyle style) {
        return TextEffects.apply(text, renderingStyle(style, hasUpstreamRenderer()));
    }

    /** @return an inline display declaration for the active engine, preserving palette and speed */
    public static String format(TextEffectStyle style) {
        return TextEffects.format(renderingStyle(style, hasUpstreamRenderer()));
    }

    static TextEffectStyle renderingStyle(TextEffectStyle style, boolean upstream) {
        String identifier = style.rendererId();
        if (!upstream || !identifier.startsWith(localNamespace)) return style;
        return new TextEffectStyle(upstreamNamespace + identifier.substring(localNamespace.length()), style.colors(),
            style.speed());
    }
}
