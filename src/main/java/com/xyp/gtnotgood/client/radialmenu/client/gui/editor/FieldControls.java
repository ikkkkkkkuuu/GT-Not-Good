package com.xyp.gtnotgood.client.radialmenu.client.gui.editor;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.resources.I18n;

import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.Ui;
import com.xyp.gtnotgood.client.radialmenu.client.profile.ProfileManager;
import com.xyp.gtnotgood.client.radialmenu.client.profile.ProfileStorage;
import com.xyp.gtnotgood.client.radialmenu.config.AccentConfig;
import com.xyp.gtnotgood.client.radialmenu.config.ColorConfig;
import com.xyp.gtnotgood.client.radialmenu.config.WheelConfig;
import com.xyp.gtnotgood.client.radialmenu.core.Colors;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionField;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSpec;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionType;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionTypes;
import com.xyp.gtnotgood.client.radialmenu.core.action.Placeholders;
import com.xyp.gtnotgood.client.radialmenu.core.model.StyleResolver;
import com.xyp.gtnotgood.client.radialmenu.core.model.WheelColors;

import cpw.mods.fml.client.config.GuiSlider;

final class FieldControls {

    static final int CLEAR_WIDTH = 20;

    private FieldControls() {}

    static boolean hasClear(ActionField field) {
        return field.kind == ActionField.Kind.Color;
    }

    static boolean isRelevant(ActionField field, ActionSpec spec) {
        if (!ActionTypes.PARAM_SLOT_COUNT.equals(field.key) || spec == null) {
            return true;
        }
        return !"dynamic".equalsIgnoreCase(spec.getString(ActionTypes.PARAM_SLOT_MODE, ""));
    }

    static boolean standsApart(ActionField field) {
        return field.kind == ActionField.Kind.Accent;
    }

    /** Whether the field is shown as a button rather than something typed into. */
    static boolean isButton(ActionField field) {
        switch (field.kind) {
            case KeybindRef:
            case MultilineString:
            case Code:
            case Enum:
            case ProfileRef:
            case Color:
            case Accent:
                return true;
            default:
                return false;
        }
    }

    static int stepAfter(ActionField field, ActionField next) {
        boolean grouped = next != null && field.kind == ActionField.Kind.Boolean
            && next.kind == ActionField.Kind.Boolean;
        return grouped ? Ui.GROUP_STEP : Ui.STEP;
    }

    static boolean pickerHasAlpha(ActionField field) {
        return field.kind != ActionField.Kind.Accent;
    }

    /** Fills in what a descriptor cannot carry: a default that depends on the client rather than on the type. */
    static void applyClientDefaults(ActionSpec spec) {
        ActionType type = ActionTypes.get(spec == null ? null : spec.type);
        if (type == null) {
            return;
        }
        for (ActionField field : type.fields) {
            if (field.kind == ActionField.Kind.ProfileRef && isBlank(spec.getString(field.key, ""))) {
                spec.set(field.key, ProfileManager.activeName());
            }
        }
    }

    static List<String> profileOptions() {
        List<String> options = new ArrayList<>(ProfileStorage.listProfileNames());
        options.add("");
        return options;
    }

    static String buttonLabel(ActionField field, String current) {
        switch (field.kind) {
            case KeybindRef:
                // #tr radialmenu.editor.pickKeybind
                // # Pick a keybinding
                // # zh_CN 选择按键绑定
                return current == null || current.isEmpty() ? I18n.format("radialmenu.editor.pickKeybind")
                    : I18n.format(current);
            case ProfileRef:
                // #tr radialmenu.action.profileSwitch.next
                // # Next profile
                // # zh_CN 下一个配置
                return isBlank(current) ? I18n.format("radialmenu.action.profileSwitch.next") : current;
            case MultilineString:
                // #tr radialmenu.editor.editLines
                // # Edit lines (%d)
                // # zh_CN 编辑行 (%d)
                return I18n.format("radialmenu.editor.editLines", Placeholders.splitLines(current).length);
            case Code:
                return I18n
                    // #tr radialmenu.editor.editScript
                    // # Edit script (%d lines)
                    // # zh_CN 编辑脚本（%d 行）
                    .format("radialmenu.editor.editScript", current == null ? 0 : current.split("\r?\n", -1).length);
            case Color:
                // #tr radialmenu.editor.inherit
                // # Inherit
                // # zh_CN 继承
                return isBlank(current) ? I18n.format("radialmenu.editor.inherit") : current;
            case Accent:
                // Nothing is stored, so there is no value to show - only the offer.
                // #tr radialmenu.editor.pickAccent
                // # Fill in from a color...
                // # zh_CN 从颜色填充...
                return I18n.format("radialmenu.editor.pickAccent");
            case Enum:
                return localizedValue(field, current);
            default:
                return current == null ? "" : current;
        }
    }

    static String localizedValue(ActionField field, String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String key = field.valueLabelKey(value);
        String translated = I18n.format(key);
        return translated.equals(key) ? value : translated;
    }

    static GuiSlider slider(int id, int x, int y, int width, int height, ActionField field, String current) {
        int value = parseInt(current, parseInt(field.defaultValue, field.min));
        return new GuiSlider(id, x, y, width, height, "", "", field.min, field.max,
            Math.max(field.min, Math.min(field.max, value)), false, true);
    }

    static void applyAccent(ActionSpec spec, ActionField field, String accent) {
        if (isBlank(accent)) {
            return;
        }
        WheelColors derived = derive(accent);
        for (String key : field.options) {
            spec.set(key, String.format("#%08X", Integer.valueOf(colorFor(key, derived))));
        }
        spec.set(field.key, "");
    }

    static WheelColors derive(String accent) {
        return ColorConfig.defaultColors().fromAccent(Colors.parseArgb(accent, 0xFFFFFF) & 0x00FFFFFF,
            AccentConfig.coefficients());
    }

    static int colorFor(String key, WheelColors colors) {
        if (ActionTypes.PARAM_HIGHLIGHT_COLOR.equals(key)) {
            return colors.highlight;
        }
        if (ActionTypes.PARAM_BORDER_COLOR.equals(key)) {
            return colors.border;
        }
        if (ActionTypes.PARAM_HIGHLIGHT_BORDER_COLOR.equals(key)) {
            return colors.highlightBorder;
        }
        if (ActionTypes.PARAM_BACKGROUND_COLOR.equals(key)) {
            return colors.background;
        }
        return colors.ring;
    }

    static boolean colorEnabled(String key) {
        return switchFor(key) == null;
    }

    /** Why a colour cannot be edited: the name of the setting that switched its part of the wheel off. */
    static String colorOffTip(String key) {
        String setting = switchFor(key);
        // #tr radialmenu.editor.colorOff.tip
        // # %s is disabled in Wheel settings. This color is kept until it is enabled again
        // # zh_CN 轮盘设置中的 %s 已关闭；此颜色在重新启用后生效
        return setting == null ? null : I18n.format("radialmenu.editor.colorOff.tip", setting);
    }

    /** The setting that has to be on for this colour to be drawn, or null when it is always drawn. */
    private static String switchFor(String key) {
        if (ActionTypes.PARAM_BORDER_COLOR.equals(key)) {
            return WheelConfig.drawOutline ? null : "drawOutline";
        }
        if (ActionTypes.PARAM_HIGHLIGHT_BORDER_COLOR.equals(key)) {
            return WheelConfig.drawHighlightOutline ? null : "drawHighlightOutline";
        }
        if (ActionTypes.PARAM_BACKGROUND_COLOR.equals(key)) {
            return WheelConfig.dimBackground ? null : "dimBackground";
        }
        return null;
    }

    /** What a colour field is actually drawn with when it is left blank: the profile's, or the mod's. */
    static int inheritedColor(String key) {
        return colorFor(key, StyleResolver.resolve(ColorConfig.defaultColors(), ProfileManager.active().style, null));
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (RuntimeException broken) {
            return fallback;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
