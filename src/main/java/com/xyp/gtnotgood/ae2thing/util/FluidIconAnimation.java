package com.xyp.gtnotgood.ae2thing.util;

import java.lang.reflect.Method;

import net.minecraft.util.IIcon;

import com.xyp.gtnotgood.ae2thing.integration.Mods;

/**
 * On GTNH, Hodgepodge patches {@code TextureAtlasSprite} so that animated sprites (such as fluid still-icons) only
 * upload their current animation frame to the atlas after being marked as needing an update. When we draw a fluid icon
 * directly (see {@code IGuiDrawSlot.drawWidget}) without that mark, the atlas region stays blank.
 * <p>
 * Hodgepodge is not on our compile classpath, so the {@code IPatchedTextureAtlasSprite#markNeedsAnimationUpdate()} call
 * the upstream mod makes is reproduced here via cached reflection, guarded by a mod-loaded check.
 */
public final class FluidIconAnimation {

    private static final String SPRITE_CLASS = "com.mitchej123.hodgepodge.textures.IPatchedTextureAtlasSprite";

    private static boolean resolved = false;
    private static Class<?> spriteClass = null;
    private static Method markMethod = null;

    private FluidIconAnimation() {}

    public static void mark(IIcon icon) {
        if (icon == null || !Mods.HODGEPODGE.isModLoaded()) return;
        resolve();
        if (spriteClass == null || markMethod == null) return;
        if (!spriteClass.isInstance(icon)) return;
        try {
            markMethod.invoke(icon);
        } catch (Throwable ignored) {
            // Rendering must not fail because the reflective mark did.
        }
    }

    private static void resolve() {
        if (resolved) return;
        resolved = true;
        try {
            spriteClass = Class.forName(SPRITE_CLASS);
            markMethod = spriteClass.getMethod("markNeedsAnimationUpdate");
        } catch (Throwable ignored) {
            spriteClass = null;
            markMethod = null;
        }
    }
}
