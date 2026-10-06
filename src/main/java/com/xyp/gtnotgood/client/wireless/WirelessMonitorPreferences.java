package com.xyp.gtnotgood.client.wireless;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import com.xyp.gtnotgood.client.text.TextEffectRegistry;
import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.text.effect.TextEffectFormat;
import com.xyp.gtnotgood.utils.text.effect.TextEffectStyle;
import com.xyp.gtnotgood.utils.text.effect.TextEffects;

/** Client-local HUD preference; never synchronized from a server. */
public final class WirelessMonitorPreferences {

    private static final String CATEGORY = "WirelessMonitor";
    private static Configuration configuration;
    public static boolean enabled;
    public static boolean scientific;
    public static int xOffset;
    public static int yOffset;
    public static float scale;
    public static boolean animatedColors;
    public static boolean animateValues;
    public static boolean customPalette;
    public static boolean bold;
    public static boolean italic;
    private static TextEffectStyle effectStyle = TextEffects.EXOTIC_RAINBOW;
    private static TextEffectStyle paletteStyle;

    private WirelessMonitorPreferences() {}

    public static void load() {
        configuration = new Configuration(
            new File(Config.getConfigDirectory(), ModList.GTNotGood.getID() + "-wireless-monitor.cfg"));
        configuration.load();
        enabled = configuration.getBoolean("Enabled", CATEGORY, false, "无线电网 HUD 开关，由游戏内按键切换并保存。");
        scientific = configuration.getBoolean("ScientificNotation", CATEGORY, false, "使用科学计数法；超长数值始终自动缩短。");
        xOffset = configuration.getInt("XOffset", CATEGORY, 0, -10000, 10000, "HUD 水平偏移，正数向右。");
        yOffset = configuration.getInt("YOffset", CATEGORY, 0, -10000, 10000, "HUD 垂直偏移，正数向上。");
        scale = configuration.getFloat("Scale", CATEGORY, 1F, 0.5F, 3F, "HUD 字体缩放。");
        animatedColors = configuration.getBoolean("AnimatedColors", CATEGORY, true, "启用与机器署名相同的滚动文字特效。");
        animateValues = configuration.getBoolean("AnimateValues", CATEGORY, true, "数值也使用特效；关闭后净变化保留红绿颜色。");
        customPalette = configuration.getBoolean("CustomPalette", CATEGORY, false, "使用自定义颜色列表替代特效默认配色。");
        bold = configuration.getBoolean("Bold", CATEGORY, false, "HUD 文字粗体。");
        italic = configuration.getBoolean("Italic", CATEGORY, false, "HUD 文字斜体。");
        String renderer = configuration
            .getString("Renderer", CATEGORY, TextEffects.EXOTIC_RAINBOW.rendererId(), "滚动特效，可在游戏内颜色预览页选择。");
        String palette = configuration
            .getString("Palette", CATEGORY, "#33CCFF,#FFAA33,#DD77FF", "自定义 RGB 颜色，逗号分隔，最多八种。");
        float speed = configuration.getFloat("Speed", CATEGORY, 1F, 0F, 10F, "特效滚动速度，0 为静止。");
        TextEffectStyle selected = TextEffectFormat.readInline(renderer);
        effectStyle = selected == null || TextEffectRegistry.get(selected.rendererId()) == null
            ? TextEffects.EXOTIC_RAINBOW
            : selected;
        effectStyle = effectStyle.withSpeed(speed);
        paletteStyle = TextEffectFormat.readInline(effectStyle.rendererId() + ";colors=" + palette + ";speed=" + speed);
        if (paletteStyle == null) paletteStyle = effectStyle.withColors(0x33CCFF, 0xFFAA33, 0xDD77FF);
        if (configuration.hasChanged()) configuration.save();
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        configuration.get(CATEGORY, "Enabled", false)
            .set(value);
        configuration.save();
    }

    /** Persists the bottom-left-relative position once editing ends, rather than writing on every mouse movement. */
    public static void savePosition() {
        configuration.get(CATEGORY, "XOffset", 0)
            .set(xOffset);
        configuration.get(CATEGORY, "YOffset", 0)
            .set(yOffset);
        configuration.save();
    }

    public static TextEffectStyle style() {
        return customPalette ? paletteStyle : effectStyle;
    }

    public static TextEffectStyle paletteStyle() {
        return paletteStyle;
    }

    /** Applies the existing effect preview to the HUD without changing the machine-credit preference. */
    public static void apply(String rendererId, boolean palette, boolean useBold, boolean useItalic) {
        effectStyle = new TextEffectStyle(rendererId, effectStyle.colors(), effectStyle.speed());
        paletteStyle = new TextEffectStyle(rendererId, paletteStyle.colors(), effectStyle.speed());
        customPalette = palette;
        bold = useBold;
        italic = useItalic;
        animatedColors = true;
        configuration.get(CATEGORY, "Renderer", rendererId)
            .set(rendererId);
        configuration.get(CATEGORY, "CustomPalette", palette)
            .set(palette);
        configuration.get(CATEGORY, "Bold", useBold)
            .set(useBold);
        configuration.get(CATEGORY, "Italic", useItalic)
            .set(useItalic);
        configuration.get(CATEGORY, "AnimatedColors", true)
            .set(true);
        configuration.save();
    }

    public static String decorate(String text) {
        String formatting = (bold ? "\u00a7l" : "") + (italic ? "\u00a7o" : "");
        return animatedColors ? TextEffects.apply(formatting + text, style()) + "\u00a7r" : formatting + text;
    }
}
