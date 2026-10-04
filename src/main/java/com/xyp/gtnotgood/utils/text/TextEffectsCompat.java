package com.xyp.gtnotgood.utils.text;

import net.minecraft.launchwrapper.Launch;

/** Detects the original text engine before Forge mod discovery, without loading its client classes. */
public final class TextEffectsCompat {

    private TextEffectsCompat() {}

    public static boolean hasUpstreamRenderer() {
        return Launch.classLoader.getResource("com/science/gtnl/client/text/EffectTextRenderer.class") != null;
    }
}
