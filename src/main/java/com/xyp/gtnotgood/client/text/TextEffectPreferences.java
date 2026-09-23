package com.xyp.gtnotgood.client.text;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.text.AnimatedText;
import com.xyp.gtnotgood.utils.text.effect.TextEffectFormat;
import com.xyp.gtnotgood.utils.text.effect.TextEffectStyle;
import com.xyp.gtnotgood.utils.text.effect.TextEffects;

/**
 * Stores the machine credit's visual choice in a client-only Forge configuration file.
 * Loading occurs after built-in effect registration, so removed or invalid identifiers fall back safely.
 * The file is separate from the shared gameplay configuration and does not affect server settings.
 */
public final class TextEffectPreferences {

    private static final String CATEGORY = "MachineCredit";
    private static final int[] PREVIEW_PALETTE = { 0x33CCFF, 0xFFAA33, 0xDD77FF };
    private static Configuration config;

    private TextEffectPreferences() {}

    /**
     * Loads the saved choice after the effect registry has been populated.
     *
     * @see com.xyp.gtnotgood.client.text.effect.BuiltinTextEffects#register()
     */
    public static void load() {
        File file = new File(Config.getConfigDirectory(), ModList.GTNotGood.getID() + "-text-effects.cfg");
        config = new Configuration(file);
        config.load();
        String renderer = config
            .getString("Renderer", CATEGORY, TextEffects.EXOTIC_RAINBOW.rendererId(), "机器提示署名使用的文字特效。");
        boolean palette = config.getBoolean("CustomPalette", CATEGORY, false, "机器署名使用预览页中的自定义配色。");
        boolean bold = config.getBoolean("Bold", CATEGORY, false, "机器署名使用粗体。");
        boolean italic = config.getBoolean("Italic", CATEGORY, false, "机器署名使用斜体。");
        TextEffectStyle style = TextEffectRegistry.get(renderer) == null ? TextEffects.EXOTIC_RAINBOW
            : TextEffectFormat.readInline(renderer);
        if (style == null) style = TextEffects.EXOTIC_RAINBOW;
        AnimatedText.configureCredit(palette ? style.withColors(PREVIEW_PALETTE) : style, bold, italic);
        if (config.hasChanged()) config.save();
    }

    /**
     * Applies the preview selection immediately and persists it for future client sessions.
     *
     * @param rendererId    registered effect identifier selected in the preview
     * @param customPalette whether the preview's alternate colors should be used
     * @param bold          whether the machine credit should be bold
     * @param italic        whether the machine credit should be italic
     */
    public static void apply(String rendererId, boolean customPalette, boolean bold, boolean italic) {
        if (TextEffectRegistry.get(rendererId) == null) {
            throw new IllegalArgumentException("Unknown text effect: " + rendererId);
        }
        if (config == null) load();
        TextEffectStyle style = TextEffectFormat.readInline(rendererId);
        AnimatedText.configureCredit(customPalette ? style.withColors(PREVIEW_PALETTE) : style, bold, italic);
        config.get(CATEGORY, "Renderer", rendererId)
            .set(rendererId);
        config.get(CATEGORY, "CustomPalette", customPalette)
            .set(customPalette);
        config.get(CATEGORY, "Bold", bold)
            .set(bold);
        config.get(CATEGORY, "Italic", italic)
            .set(italic);
        config.save();
    }

    /** @return true when the saved machine credit uses the preview's alternate colors */
    public static boolean customPalette() {
        return !AnimatedText.creditStyle()
            .colors()
            .isEmpty();
    }
}
