package com.xyp.gtnotgood.client.radialmenu.config;

import static com.xyp.gtnotgood.client.radialmenu.RadialMenuMod.MODID;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.xyp.gtnotgood.client.radialmenu.core.Colors;
import com.xyp.gtnotgood.client.radialmenu.core.model.WheelColors;
import com.xyp.gtnotgood.utils.enums.ModList;

@Config(
    modid = MODID,
    configSubDirectory = ModList.Names.GT_NOT_GOOD + "/RadialMenu",
    filename = "general",
    category = "colors")
@Config.LangKey("颜色")
@Config.Comment("轮盘颜色")
public class ColorConfig {

    @Config.Comment("轮盘填充颜色，格式 0xAARRGGBB。")
    @Config.DefaultString("0x99101010")
    @Config.Order(1)
    public static String ringColor;

    @Config.Comment("轮盘边线颜色，格式 0xAARRGGBB；透明时隐藏。")
    @Config.DefaultString("0x60FFFFFF")
    @Config.Order(2)
    public static String borderColor;

    @Config.Comment("选中扇区的填充颜色，格式 0xAARRGGBB。")
    @Config.DefaultString("0xCC4A90D9")
    @Config.Order(3)
    public static String highlightColor;

    @Config.Comment("选中扇区的边线颜色，格式 0xAARRGGBB。")
    @Config.DefaultString("0xCCFFFFFF")
    @Config.Order(4)
    public static String highlightBorderColor;

    @Config.Comment("轮盘背景遮罩颜色，格式 0xAARRGGBB；需启用背景遮罩。")
    @Config.DefaultString("0x80101010")
    @Config.Order(5)
    public static String backgroundColor;

    @Config.Comment("图标默认染色，格式 #RRGGBB；不改变物品图标。")
    @Config.DefaultString("#FFFFFF")
    @Config.Order(6)
    public static String iconColor;

    public static WheelColors defaultColors() {
        return new WheelColors(Colors.parseArgb(ringColor, 0x99101010), Colors.parseArgb(highlightColor, 0xCC4A90D9),
            Colors.parseArgb(borderColor, 0x60FFFFFF), Colors.parseArgb(highlightBorderColor, 0xCCFFFFFF),
            Colors.parseArgb(backgroundColor, 0x80101010), Colors.parseArgb(iconColor, 0xFFFFFFFF));
    }
}
