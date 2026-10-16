package com.xyp.gtnotgood.client.radialmenu.core.action;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ActionField {

    public enum Kind {
        String,
        MultilineString,

        Code,
        Int,
        Boolean,
        Enum,

        KeybindRef,

        ProfileRef,

        Color,

        Accent
    }

    public final String key;

    public final String labelKey;

    public final Kind kind;

    public final String defaultValue;

    public final List<String> options;

    public final int min;

    public final int max;

    public final boolean required;

    public String valueLabelKey(String value) {
        switch (labelKey + "." + value) {
            // #tr radialmenu.action.keybind.mode.tap
            // # Tap
            // # zh_CN 点击
            case "radialmenu.action.keybind.mode.tap":
                return "radialmenu.action.keybind.mode.tap";
            // #tr radialmenu.action.keybind.mode.toggle
            // # Toggle
            // # zh_CN 切换
            case "radialmenu.action.keybind.mode.toggle":
                return "radialmenu.action.keybind.mode.toggle";
            // #tr radialmenu.action.keybind.mode.hold
            // # Hold
            // # zh_CN 按住
            case "radialmenu.action.keybind.mode.hold":
                return "radialmenu.action.keybind.mode.hold";
            // #tr radialmenu.action.submenu.opening.replace
            // # Replace
            // # zh_CN 替换
            case "radialmenu.action.submenu.opening.replace":
                return "radialmenu.action.submenu.opening.replace";
            // #tr radialmenu.action.submenu.opening.inline
            // # Inline
            // # zh_CN 内嵌
            case "radialmenu.action.submenu.opening.inline":
                return "radialmenu.action.submenu.opening.inline";
            // #tr radialmenu.action.submenu.mode.fixed
            // # Fixed
            // # zh_CN 固定
            case "radialmenu.action.submenu.mode.fixed":
                return "radialmenu.action.submenu.mode.fixed";
            // #tr radialmenu.action.submenu.mode.dynamic
            // # Dynamic
            // # zh_CN 动态
            case "radialmenu.action.submenu.mode.dynamic":
                return "radialmenu.action.submenu.mode.dynamic";
            default:
                return labelKey + "." + value;
        }
    }

    public String tooltipKey() {
        switch (labelKey) {
            // #tr radialmenu.action.keybind.mode
            // # Mode
            // # zh_CN 模式
            case "radialmenu.action.keybind.mode":
                // #tr radialmenu.action.keybind.mode.tip
                // # Tap: press once. Toggle: hold or release. Hold: press for the selected duration
                // # zh_CN 单击：按一次；切换：按住或松开；按住：持续指定时间
                return "radialmenu.action.keybind.mode.tip";
            // #tr radialmenu.action.keybind.holdTicks
            // # Hold (ticks)
            // # zh_CN 按住时间（刻）
            case "radialmenu.action.keybind.holdTicks":
                // #tr radialmenu.action.keybind.holdTicks.tip
                // # Key hold duration in ticks. 20 ticks = 1 second
                // # zh_CN 按住模式的持续时间，20 刻 = 1 秒
                return "radialmenu.action.keybind.holdTicks.tip";
            // #tr radialmenu.action.command.delayTicks
            // # Delay (ticks)
            // # zh_CN 延迟（刻）
            case "radialmenu.action.command.delayTicks":
                // #tr radialmenu.action.command.delayTicks.tip
                // # Delay between lines in ticks. 0 sends all lines at once; 20 ticks = 1 second
                // # zh_CN 逐行发送间隔，20 刻 = 1 秒；0 则同时发送全部行
                return "radialmenu.action.command.delayTicks.tip";
            // #tr radialmenu.action.command.cycle
            // # One line per press
            // # zh_CN 每次按下发送一行
            case "radialmenu.action.command.cycle":
                // #tr radialmenu.action.command.cycle.tip
                // # Send one line per press, cycling through the list
                // # zh_CN 每次触发发送一行，按列表顺序循环
                return "radialmenu.action.command.cycle.tip";
            // #tr radialmenu.action.sequence.delayTicks
            // # Delay (ticks)
            // # zh_CN 延迟（刻）
            case "radialmenu.action.sequence.delayTicks":
                // #tr radialmenu.action.sequence.delayTicks.tip
                // # Delay between steps in ticks. 0 runs all steps at once
                // # zh_CN 步骤间隔，单位为刻；0 则同时执行全部步骤
                return "radialmenu.action.sequence.delayTicks.tip";
            // #tr radialmenu.action.submenu.opening
            // # Opens as
            // # zh_CN 打开方式
            case "radialmenu.action.submenu.opening":
                // #tr radialmenu.action.submenu.opening.tip
                // # Replace: right-click to return. Inline: hover to expand one submenu per ring
                // # zh_CN 替换：右键返回；内联：悬停展开，同级同时展开一个子菜单
                return "radialmenu.action.submenu.opening.tip";
            // #tr radialmenu.action.submenu.mode
            // # Slot layout
            // # zh_CN 槽位布局
            case "radialmenu.action.submenu.mode":
                // #tr radialmenu.action.submenu.mode.tip
                // # Fixed: keep slot angles. Dynamic: divide the ring evenly among entries
                // # zh_CN 固定：保持槽位角度；动态：按条目数量均分轮盘
                return "radialmenu.action.submenu.mode.tip";
            // #tr radialmenu.action.submenu.slots
            // # Slot count
            // # zh_CN 槽位数量
            case "radialmenu.action.submenu.slots":
                // #tr radialmenu.action.submenu.slots.tip
                // # Number of sectors in a fixed-layout submenu
                // # zh_CN 固定布局下的子菜单扇区数量
                return "radialmenu.action.submenu.slots.tip";
            // #tr radialmenu.action.submenu.ringColor
            // # Ring fill
            // # zh_CN 环形填充色
            case "radialmenu.action.submenu.ringColor":
                // #tr radialmenu.action.submenu.ringColor.tip
                // # Submenu ring color. Inherit to use the profile color
                // # zh_CN 子菜单环形区域颜色；继承则使用配置方案颜色
                return "radialmenu.action.submenu.ringColor.tip";
            // #tr radialmenu.action.submenu.highlightColor
            // # Highlight fill
            // # zh_CN 高亮填充色
            case "radialmenu.action.submenu.highlightColor":
                // #tr radialmenu.action.submenu.highlightColor.tip
                // # Hovered sector color. Inherit to use the profile color
                // # zh_CN 悬停扇区颜色；继承则使用配置方案颜色
                return "radialmenu.action.submenu.highlightColor.tip";
            // #tr radialmenu.action.submenu.borderColor
            // # Ring outline
            // # zh_CN 环形边框
            case "radialmenu.action.submenu.borderColor":
                // #tr radialmenu.action.submenu.borderColor.tip
                // # Divider and edge color. Leave blank to inherit the profile color
                // # zh_CN 分隔线和边缘颜色；留空则继承配置方案颜色
                return "radialmenu.action.submenu.borderColor.tip";
            // #tr radialmenu.action.submenu.accent
            // # Accent
            // # zh_CN 强调色
            case "radialmenu.action.submenu.accent":
                // #tr radialmenu.action.submenu.accent.tip
                // # Generate submenu colors from one accent. Each color can then be adjusted
                // # zh_CN 用强调色生成子菜单配色，再分别调整
                return "radialmenu.action.submenu.accent.tip";
            // #tr radialmenu.action.submenu.highlightBorderColor
            // # Highlight outline
            // # zh_CN 高亮边框
            case "radialmenu.action.submenu.highlightBorderColor":
                // #tr radialmenu.action.submenu.highlightBorderColor.tip
                // # Hovered sector outline. Leave blank to inherit the profile color
                // # zh_CN 悬停扇区边框颜色；留空则继承配置方案颜色
                return "radialmenu.action.submenu.highlightBorderColor.tip";
            // #tr radialmenu.action.submenu.backgroundColor
            // # Background
            // # zh_CN 背景
            case "radialmenu.action.submenu.backgroundColor":
                // #tr radialmenu.action.submenu.backgroundColor.tip
                // # Screen tint behind this submenu. Leave blank to inherit the profile color
                // # zh_CN 子菜单后方的屏幕蒙层颜色；留空则继承配置方案颜色
                return "radialmenu.action.submenu.backgroundColor.tip";
            // #tr radialmenu.action.script.source
            // # Lua
            // # zh_CN Lua
            case "radialmenu.action.script.source":
                // #tr radialmenu.action.script.source.tip
                // # Lua script source, one line per field
                // # zh_CN Lua 脚本内容，每个输入框对应一行
                return "radialmenu.action.script.source.tip";
            // #tr radialmenu.action.script.timeoutTicks
            // # Timeout (ticks)
            // # zh_CN 超时时间（刻）
            case "radialmenu.action.script.timeoutTicks":
                // #tr radialmenu.action.script.timeoutTicks.tip
                // # Maximum script lifetime, including waits. 600 ticks = 30 seconds
                // # zh_CN 脚本运行时限，包含等待时间；600 刻 = 30 秒
                return "radialmenu.action.script.timeoutTicks.tip";
            // #tr radialmenu.action.keybind.shift
            // # Hold Shift
            // # zh_CN 按住 Shift
            case "radialmenu.action.keybind.shift":
                // #tr radialmenu.action.keybind.shift.tip
                // # Hold Shift while triggering this keybinding
                // # zh_CN 触发此按键时同时按住 Shift
                return "radialmenu.action.keybind.shift.tip";
            // #tr radialmenu.action.keybind.ctrl
            // # Hold Ctrl
            // # zh_CN 按住 Ctrl
            case "radialmenu.action.keybind.ctrl":
                // #tr radialmenu.action.keybind.ctrl.tip
                // # Hold Ctrl while triggering this keybinding
                // # zh_CN 触发此按键时同时按住 Ctrl
                return "radialmenu.action.keybind.ctrl.tip";
            // #tr radialmenu.action.keybind.alt
            // # Hold Alt
            // # zh_CN 按住 Alt
            case "radialmenu.action.keybind.alt":
                // #tr radialmenu.action.keybind.alt.tip
                // # Hold Alt while triggering this keybinding
                // # zh_CN 触发此按键时同时按住 Alt
                return "radialmenu.action.keybind.alt.tip";
            // #tr radialmenu.action.keybind.sneak
            // # Hold Sneak
            // # zh_CN 按住潜行
            case "radialmenu.action.keybind.sneak":
                // #tr radialmenu.action.keybind.sneak.tip
                // # Start sneaking, then trigger this keybinding one tick later
                // # zh_CN 先开始潜行，1 刻后触发此按键
                return "radialmenu.action.keybind.sneak.tip";
            default:
                return labelKey + ".tip";
        }
    }

    private ActionField(String key, String labelKey, Kind kind, String defaultValue, List<String> options,
        boolean required, int min, int max) {
        this.key = key;
        this.labelKey = labelKey;
        this.kind = kind;
        this.defaultValue = defaultValue;
        this.options = options == null ? Collections.<String>emptyList() : options;
        this.required = required;
        this.min = min;
        this.max = max;
    }

    public ActionField required() {
        return new ActionField(key, labelKey, kind, defaultValue, options, true, min, max);
    }

    public boolean hasRange() {
        return kind == Kind.Int && max > min;
    }

    public boolean isMissingFrom(ActionSpec spec) {
        if (!required) {
            return false;
        }
        String value = spec.getString(key, null);
        return value == null || value.trim().isEmpty();
    }

    public static ActionField string(String key, String labelKey, String defaultValue) {
        return new ActionField(key, labelKey, Kind.String, defaultValue, null, false, 0, 0);
    }

    public static ActionField multiline(String key, String labelKey, String defaultValue) {
        return new ActionField(key, labelKey, Kind.MultilineString, defaultValue, null, false, 0, 0);
    }

    public static ActionField code(String key, String labelKey, String defaultValue) {
        return new ActionField(key, labelKey, Kind.Code, defaultValue, null, false, 0, 0);
    }

    public static ActionField integer(String key, String labelKey, int defaultValue) {
        return new ActionField(key, labelKey, Kind.Int, Integer.toString(defaultValue), null, false, 0, 0);
    }

    public static ActionField range(String key, String labelKey, int defaultValue, int min, int max) {
        return new ActionField(key, labelKey, Kind.Int, Integer.toString(defaultValue), null, false, min, max);
    }

    public static ActionField bool(String key, String labelKey, boolean defaultValue) {
        return new ActionField(key, labelKey, Kind.Boolean, Boolean.toString(defaultValue), null, false, 0, 0);
    }

    public static ActionField enumeration(String key, String labelKey, String defaultValue, String... options) {
        return new ActionField(key, labelKey, Kind.Enum, defaultValue, Arrays.asList(options), false, 0, 0);
    }

    public static ActionField keybindRef(String key, String labelKey) {
        return new ActionField(key, labelKey, Kind.KeybindRef, "", null, false, 0, 0);
    }

    public static ActionField profileRef(String key, String labelKey) {
        return new ActionField(key, labelKey, Kind.ProfileRef, "", null, false, 0, 0);
    }

    public static ActionField color(String key, String labelKey) {
        return new ActionField(key, labelKey, Kind.Color, "", null, false, 0, 0);
    }

    public static ActionField accent(String key, String labelKey, String... derives) {
        return new ActionField(key, labelKey, Kind.Accent, "", Arrays.asList(derives), false, 0, 0);
    }
}
