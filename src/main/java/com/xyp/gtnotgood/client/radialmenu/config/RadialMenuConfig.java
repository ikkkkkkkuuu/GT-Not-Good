package com.xyp.gtnotgood.client.radialmenu.config;

import static com.xyp.gtnotgood.client.radialmenu.RadialMenuMod.MODID;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.xyp.gtnotgood.utils.enums.ModList;

@Config(
    modid = MODID,
    configSubDirectory = ModList.Names.GT_NOT_GOOD + "/RadialMenu",
    filename = "general",
    category = "general")
@Config.LangKey("行为")
@Config.Comment("轮盘操作")
public class RadialMenuConfig {

    @Config.Comment("松开轮盘键时执行选中动作；关闭后需点击执行。")
    @Config.DefaultBoolean(true)
    @Config.Order(1)
    public static boolean releaseToSelect;

    @Config.Comment("使用鼠标滚轮选择项目，此模式下子菜单替换当前轮盘。")
    @Config.DefaultBoolean(false)
    @Config.Order(2)
    public static boolean scrollToSelect;

    @Config.Comment("右键打开编辑器；关闭后使用 Shift 加左键编辑。")
    @Config.DefaultBoolean(false)
    @Config.Order(3)
    public static boolean rightClickToEdit;

    @Config.Comment("悬停多久后展开外环子菜单，单位毫秒；零表示立即展开。")
    @Config.DefaultInt(150)
    @Config.RangeInt(min = 0, max = 2000)
    @Config.Order(4)
    public static int inlineHoverDelayMs;

    @Config.Comment("打开轮盘时继续处理游戏输入，允许玩家移动。")
    @Config.DefaultBoolean(true)
    @Config.Order(5)
    public static boolean allowInputWhileOpen;

    @Config.Comment("打开轮盘时将鼠标移到屏幕中心。")
    @Config.DefaultBoolean(true)
    @Config.Order(6)
    public static boolean centerCursorOnOpen;

    @Config.Comment("执行未绑定按键的动作时临时借用 F15，以兼容拒绝零键码的模组。")
    @Config.DefaultBoolean(true)
    @Config.Order(7)
    public static boolean lendKeyCodeToUnbound;

    @Config.Comment("允许执行配置中的 Lua 脚本；脚本可以发送聊天和命令。")
    @Config.DefaultBoolean(true)
    @Config.Order(8)
    public static boolean enableScripts;

    @Config.Comment("记录注入按键和潜行状态，仅供兼容性调试。")
    @Config.DefaultBoolean(false)
    @Config.Order(9)
    public static boolean logKeyState;
}
