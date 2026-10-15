package com.xyp.gtnotgood.client.radialmenu.config;

import static com.xyp.gtnotgood.client.radialmenu.RadialMenuMod.MODID;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.xyp.gtnotgood.client.radialmenu.core.animation.RevealAnimation;
import com.xyp.gtnotgood.utils.enums.ModList;

@Config(
    modid = MODID,
    configSubDirectory = ModList.Names.GT_NOT_GOOD + "/RadialMenu",
    filename = "general",
    category = "animation")
@Config.LangKey("动画")
@Config.Comment("轮盘动画")
public class AnimationConfig {

    @Config.Comment("展开动画：NONE、FADE、ZOOM、STAGGER_CW、STAGGER_CCW 或 STAGGER_BOTH。")
    @Config.DefaultEnum("ZOOM")
    @Config.Order(1)
    public static RevealAnimation revealAnimation;

    @Config.Comment("展开动画总时长，单位毫秒；零表示立即完成。")
    @Config.DefaultInt(180)
    @Config.RangeInt(min = 0, max = 2000)
    @Config.Order(2)
    public static int revealDurationMs;

    @Config.Comment("选中扇区向外移动的距离，单位为 GUI 像素。")
    @Config.DefaultInt(4)
    @Config.RangeInt(min = 0, max = 20)
    @Config.Order(3)
    public static int hoverPush;

    @Config.Comment("选中扇区移动时长，单位毫秒；零表示立即完成。")
    @Config.DefaultInt(120)
    @Config.RangeInt(min = 0, max = 2000)
    @Config.Order(4)
    public static int hoverDurationMs;
}
