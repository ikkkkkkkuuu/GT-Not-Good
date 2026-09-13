package com.xyp.ldlib.gui.texture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

/**
 * Uses Minecraft's bitmap Latin glyphs while retaining Unicode fallback for Chinese and other scripts.
 * In 1.7.10 Chinese locales otherwise force the thin Unicode glyphs even for ASCII characters.
 * Scope both measurement and drawing/input so centering, wrapping and caret positions agree.
 * Must be used on the client thread in try-with-resources; nested scopes restore their caller's state.
 */
public final class PixelFontScope implements AutoCloseable {

    private final FontRenderer font;
    private final boolean unicode;

    public PixelFontScope() {
        font = Minecraft.getMinecraft().fontRenderer;
        unicode = font.getUnicodeFlag();
        font.setUnicodeFlag(false);
    }

    @Override
    public void close() {
        font.setUnicodeFlag(unicode);
    }
}
