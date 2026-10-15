package com.xyp.gtnotgood.client.radialmenu.config;

import static com.xyp.gtnotgood.client.radialmenu.RadialMenuMod.MODID;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.xyp.gtnotgood.client.radialmenu.core.model.AccentCoefficients;
import com.xyp.gtnotgood.utils.enums.ModList;

@Config(
    modid = MODID,
    configSubDirectory = ModList.Names.GT_NOT_GOOD + "/RadialMenu",
    filename = "general",
    category = "accent")
@Config.LangKey("主题色")
@Config.Comment("主题色参数")
public class AccentConfig {

    @Config.Comment("主题色生成的轮盘透明度，范围 0 至 255。")
    @Config.DefaultInt(153)
    @Config.RangeInt(min = 0, max = 255)
    @Config.Order(1)
    public static int ringAlpha;

    @Config.Comment("主题色生成的轮盘亮度，单位为主题色亮度百分比。")
    @Config.DefaultInt(18)
    @Config.RangeInt(min = 0, max = 200)
    @Config.Order(2)
    public static int ringBrightness;

    @Config.Comment("主题色生成的边线透明度，范围 0 至 255。")
    @Config.DefaultInt(96)
    @Config.RangeInt(min = 0, max = 255)
    @Config.Order(3)
    public static int borderAlpha;

    @Config.Comment("主题色生成的边线亮度，单位为主题色亮度百分比。")
    @Config.DefaultInt(140)
    @Config.RangeInt(min = 0, max = 200)
    @Config.Order(4)
    public static int borderBrightness;

    @Config.Comment("主题色生成的高亮透明度，范围 0 至 255。")
    @Config.DefaultInt(204)
    @Config.RangeInt(min = 0, max = 255)
    @Config.Order(5)
    public static int highlightAlpha;

    @Config.Comment("主题色生成的高亮亮度，单位为主题色亮度百分比。")
    @Config.DefaultInt(100)
    @Config.RangeInt(min = 0, max = 200)
    @Config.Order(6)
    public static int highlightBrightness;

    @Config.Comment("主题色生成的高亮边线透明度，范围 0 至 255。")
    @Config.DefaultInt(204)
    @Config.RangeInt(min = 0, max = 255)
    @Config.Order(7)
    public static int highlightBorderAlpha;

    @Config.Comment("主题色生成的高亮边线亮度，单位为主题色亮度百分比。")
    @Config.DefaultInt(160)
    @Config.RangeInt(min = 0, max = 200)
    @Config.Order(8)
    public static int highlightBorderBrightness;

    @Config.Comment("主题色生成的背景透明度，范围 0 至 255。")
    @Config.DefaultInt(128)
    @Config.RangeInt(min = 0, max = 255)
    @Config.Order(9)
    public static int backgroundAlpha;

    @Config.Comment("主题色生成的背景亮度，单位为主题色亮度百分比。")
    @Config.DefaultInt(8)
    @Config.RangeInt(min = 0, max = 200)
    @Config.Order(10)
    public static int backgroundBrightness;

    public static AccentCoefficients coefficients() {
        return new AccentCoefficients(new AccentCoefficients.Tone(ringAlpha, ringBrightness),
            new AccentCoefficients.Tone(highlightAlpha, highlightBrightness),
            new AccentCoefficients.Tone(borderAlpha, borderBrightness),
            new AccentCoefficients.Tone(highlightBorderAlpha, highlightBorderBrightness),
            new AccentCoefficients.Tone(backgroundAlpha, backgroundBrightness));
    }
}
