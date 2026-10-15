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
                // # Tap once, toggle held state, or hold for the configured duration.
                // # zh_CN 单击按下一次，切换保持按下状态，或按住指定时长。
                return "radialmenu.action.keybind.mode.tip";
            // #tr radialmenu.action.keybind.holdTicks
            // # Hold (ticks)
            // # zh_CN 按住时间（刻）
            case "radialmenu.action.keybind.holdTicks":
                // #tr radialmenu.action.keybind.holdTicks.tip
                // # How long Hold keeps the key down. 20 ticks is one second.
                // # zh_CN 按住模式保持按键的时间。20 刻等于 1 秒。
                return "radialmenu.action.keybind.holdTicks.tip";
            // #tr radialmenu.action.command.delayTicks
            // # Delay (ticks)
            // # zh_CN 延迟（刻）
            case "radialmenu.action.command.delayTicks":
                // #tr radialmenu.action.command.delayTicks.tip
                // # Ticks between lines. 0 sends them all at once. 20 ticks is one second.
                // # zh_CN 每行之间的间隔刻数。0 表示同时发送全部内容。20 刻等于 1 秒。
                return "radialmenu.action.command.delayTicks.tip";
            // #tr radialmenu.action.command.cycle
            // # One line per press
            // # zh_CN 每次按下发送一行
            case "radialmenu.action.command.cycle":
                // #tr radialmenu.action.command.cycle.tip
                // # Send one line per press, advancing through the list, instead of all of them.
                // # zh_CN 每次按下发送一行，并按列表顺序循环，而不是一次发送全部。
                return "radialmenu.action.command.cycle.tip";
            // #tr radialmenu.action.sequence.delayTicks
            // # Delay (ticks)
            // # zh_CN 延迟（刻）
            case "radialmenu.action.sequence.delayTicks":
                // #tr radialmenu.action.sequence.delayTicks.tip
                // # Ticks between steps. 0 runs them all at once.
                // # zh_CN 步骤之间的间隔刻数。0 表示同时执行全部步骤。
                return "radialmenu.action.sequence.delayTicks.tip";
            // #tr radialmenu.action.submenu.opening
            // # Opens as
            // # zh_CN 打开方式
            case "radialmenu.action.submenu.opening":
                // #tr radialmenu.action.submenu.opening.tip
                // # Replace: right-click to return. Inline: hover to expand one submenu ring.
                // # zh_CN 替换：右键返回。内联：悬停展开外圈，同级同时仅展开一个子菜单。
                return "radialmenu.action.submenu.opening.tip";
            // #tr radialmenu.action.submenu.mode
            // # Slot layout
            // # zh_CN 槽位布局
            case "radialmenu.action.submenu.mode":
                // #tr radialmenu.action.submenu.mode.tip
                // # Fixed keeps slot angles. Dynamic divides the ring among existing entries.
                // # zh_CN 固定布局保持槽位角度，动态布局按现有条目数量均分轮盘。
                return "radialmenu.action.submenu.mode.tip";
            // #tr radialmenu.action.submenu.slots
            // # Slot count
            // # zh_CN 槽位数量
            case "radialmenu.action.submenu.slots":
                // #tr radialmenu.action.submenu.slots.tip
                // # How many sectors the ring has when the layout is Fixed.
                // # zh_CN 当布局为固定模式时，环形菜单拥有的区域数量。
                return "radialmenu.action.submenu.slots.tip";
            // #tr radialmenu.action.submenu.ringColor
            // # Ring fill
            // # zh_CN 环形填充色
            case "radialmenu.action.submenu.ringColor":
                // #tr radialmenu.action.submenu.ringColor.tip
                // # Color of this submenu's ring. Leave it inherited to use the profile's color.
                // # zh_CN 此子菜单环形区域的颜色。保持继承即可使用配置的颜色。
                return "radialmenu.action.submenu.ringColor.tip";
            // #tr radialmenu.action.submenu.highlightColor
            // # Highlight fill
            // # zh_CN 高亮填充色
            case "radialmenu.action.submenu.highlightColor":
                // #tr radialmenu.action.submenu.highlightColor.tip
                // # Color of the sector under the cursor. Leave it inherited to use the profile's color.
                // # zh_CN 光标所在区域的颜色。保持继承即可使用配置的颜色。
                return "radialmenu.action.submenu.highlightColor.tip";
            // #tr radialmenu.action.submenu.borderColor
            // # Ring outline
            // # zh_CN 环形边框
            case "radialmenu.action.submenu.borderColor":
                // #tr radialmenu.action.submenu.borderColor.tip
                // # Submenu dividers and edges. Leave blank to inherit the profile color.
                // # zh_CN 子菜单的分隔线与边缘颜色。留空则继承配置方案的颜色。
                return "radialmenu.action.submenu.borderColor.tip";
            // #tr radialmenu.action.submenu.accent
            // # Accent
            // # zh_CN 强调色
            case "radialmenu.action.submenu.accent":
                // #tr radialmenu.action.submenu.accent.tip
                // # Generate this submenu's colors from one accent, then adjust each color.
                // # zh_CN 用一种强调色生成子菜单配色，再分别调整各个颜色。
                return "radialmenu.action.submenu.accent.tip";
            // #tr radialmenu.action.submenu.highlightBorderColor
            // # Highlight outline
            // # zh_CN 高亮边框
            case "radialmenu.action.submenu.highlightBorderColor":
                // #tr radialmenu.action.submenu.highlightBorderColor.tip
                // # Hovered sector outline. Leave blank to inherit the profile color.
                // # zh_CN 悬停扇区的边框颜色。留空则继承配置方案的颜色。
                return "radialmenu.action.submenu.highlightBorderColor.tip";
            // #tr radialmenu.action.submenu.backgroundColor
            // # Background
            // # zh_CN 背景
            case "radialmenu.action.submenu.backgroundColor":
                // #tr radialmenu.action.submenu.backgroundColor.tip
                // # Screen tint behind this submenu. Leave blank to inherit the profile color.
                // # zh_CN 子菜单后方屏幕的蒙层颜色。留空则继承配置方案的颜色。
                return "radialmenu.action.submenu.backgroundColor.tip";
            // #tr radialmenu.action.script.source
            // # Lua
            // # zh_CN Lua
            case "radialmenu.action.script.source":
                // #tr radialmenu.action.script.source.tip
                // # The script. One line per field, as it would be written in a file.
                // # zh_CN 脚本内容。每个字段一行，格式与写入文件时相同。
                return "radialmenu.action.script.source.tip";
            // #tr radialmenu.action.script.timeoutTicks
            // # Timeout (ticks)
            // # zh_CN 超时时间（刻）
            case "radialmenu.action.script.timeoutTicks":
                // #tr radialmenu.action.script.timeoutTicks.tip
                // # How long the script may stay alive, waiting included. 600 ticks is 30 seconds.
                // # zh_CN 脚本允许保持运行的时间，包括等待时间。600 刻等于 30 秒。
                return "radialmenu.action.script.timeoutTicks.tip";
            // #tr radialmenu.action.keybind.shift
            // # Hold Shift
            // # zh_CN 按住 Shift
            case "radialmenu.action.keybind.shift":
                // #tr radialmenu.action.keybind.shift.tip
                // # Hold Shift while pressing this binding, including direct keyboard checks.
                // # zh_CN 触发此按键时按住 Shift，兼容直接检查键盘状态的模组。
                return "radialmenu.action.keybind.shift.tip";
            // #tr radialmenu.action.keybind.ctrl
            // # Hold Ctrl
            // # zh_CN 按住 Ctrl
            case "radialmenu.action.keybind.ctrl":
                // #tr radialmenu.action.keybind.ctrl.tip
                // # Hold Ctrl down while the binding is pressed.
                // # zh_CN 按下绑定键时保持按住 Ctrl。
                return "radialmenu.action.keybind.ctrl.tip";
            // #tr radialmenu.action.keybind.alt
            // # Hold Alt
            // # zh_CN 按住 Alt
            case "radialmenu.action.keybind.alt":
                // #tr radialmenu.action.keybind.alt.tip
                // # Hold Alt down while the binding is pressed.
                // # zh_CN 按下绑定键时保持按住 Alt。
                return "radialmenu.action.keybind.alt.tip";
            // #tr radialmenu.action.keybind.sneak
            // # Hold Sneak
            // # zh_CN 按住潜行
            case "radialmenu.action.keybind.sneak":
                // #tr radialmenu.action.keybind.sneak.tip
                // # Start sneaking, then press the binding one tick later for sneak-aware mods.
                // # zh_CN 先开始潜行，一刻后触发按键，兼容检查潜行状态的模组。
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
