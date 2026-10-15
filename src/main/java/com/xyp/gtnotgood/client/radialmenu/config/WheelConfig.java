package com.xyp.gtnotgood.client.radialmenu.config;

import static com.xyp.gtnotgood.client.radialmenu.RadialMenuMod.MODID;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.xyp.gtnotgood.utils.enums.ModList;

@Config(
    modid = MODID,
    configSubDirectory = ModList.Names.GT_NOT_GOOD + "/RadialMenu",
    filename = "general",
    category = "wheel")
@Config.LangKey("外观")
@Config.Comment("轮盘外观")
public class WheelConfig {

    @Config.Comment("轮盘外半径，单位为 GUI 像素。")
    @Config.DefaultInt(80)
    @Config.RangeInt(min = 32, max = 240)
    @Config.Order(1)
    public static int outerRadius;

    @Config.Comment("轮盘内半径；中心区域不选中任何项目。")
    @Config.DefaultInt(32)
    @Config.RangeInt(min = 8, max = 200)
    @Config.Order(2)
    public static int innerRadius;

    @Config.Comment("扇区间的视觉间距，单位为 GUI 像素；不改变点击区域。")
    @Config.DefaultFloat(0f)
    @Config.RangeFloat(min = 0f, max = 12f)
    @Config.Order(3)
    public static float sectorGap;

    @Config.Comment("外环子菜单与父轮盘的间距，单位为 GUI 像素。")
    @Config.DefaultInt(2)
    @Config.RangeInt(min = 0, max = 32)
    @Config.Order(6)
    public static int inlineRingSpacing;

    @Config.Comment("轮盘边线宽度，单位为 GUI 像素。")
    @Config.DefaultInt(1)
    @Config.RangeInt(min = 1, max = 6)
    @Config.Order(4)
    public static int borderWidth;

    @Config.Comment("绘制轮盘边缘和扇区分隔线。")
    @Config.DefaultBoolean(true)
    @Config.Order(5)
    public static boolean drawOutline;

    @Config.Comment("绘制选中扇区的边线。")
    @Config.DefaultBoolean(true)
    @Config.Order(7)
    public static boolean drawHighlightOutline;

    @Config.Comment("使用一像素渐变平滑轮盘边缘。")
    @Config.DefaultBoolean(false)
    @Config.Order(8)
    public static boolean smoothEdges;

    @Config.Comment("轮盘图标尺寸，单位为 GUI 像素；背景槽位同步缩放。")
    @Config.DefaultInt(16)
    @Config.RangeInt(min = 8, max = 48)
    @Config.Order(9)
    public static int iconSize;

    @Config.Comment("图标背景：NONE 无，SLOT 槽位，HOTBAR 选中高亮，SELECTED 全部高亮。")
    @Config.DefaultEnum("NONE")
    @Config.Order(10)
    public static SlotPlate slotPlate;

    @Config.Comment("在轮盘后绘制背景遮罩。")
    @Config.DefaultBoolean(false)
    @Config.Order(11)
    public static boolean dimBackground;

    public static int effectiveInnerRadius() {
        return Math.min(innerRadius, outerRadius - 8);
    }

    public static int effectiveIconSize() {
        return Math.max(4, iconSize);
    }
}
