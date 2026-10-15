package com.xyp.gtnotgood.client.radialmenu.client.gui.editor;

import net.minecraft.client.resources.I18n;

import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSpec;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionType;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionTypes;
import com.xyp.gtnotgood.client.radialmenu.core.action.Placeholders;

final class ActionSummary {

    private ActionSummary() {}

    static String of(ActionSpec spec) {
        if (spec == null || spec.type == null) {
            // #tr radialmenu.steps.unset
            // # not set
            // # zh_CN 未设置
            return I18n.format("radialmenu.steps.unset");
        }
        ActionType type = ActionTypes.get(spec.type);
        String name = type == null ? spec.type : I18n.format(type.labelKey);
        String detail = detailOf(spec);
        return detail.isEmpty() ? name : name + ": " + detail;
    }

    private static String detailOf(ActionSpec spec) {
        if (ActionTypes.KEYBIND.equals(spec.type)) {
            String binding = spec.getString(ActionTypes.PARAM_BINDING, "");
            return binding.isEmpty() ? I18n.format("radialmenu.steps.unset") : I18n.format(binding);
        }
        if (ActionTypes.COMMAND.equals(spec.type)) {
            String[] lines = Placeholders.splitLines(spec.getString(ActionTypes.PARAM_COMMAND, ""));
            if (lines.length == 0) {
                return I18n.format("radialmenu.steps.unset");
            }
            // #tr radialmenu.steps.andMore
            // # %s (+%s more)
            // # zh_CN %s（另外还有 %s 个）
            return lines.length == 1 ? lines[0] : I18n.format("radialmenu.steps.andMore", lines[0], lines.length - 1);
        }
        if (ActionTypes.PROFILE_SWITCH.equals(spec.type)) {
            String profile = spec.getString(ActionTypes.PARAM_PROFILE, "");
            // #tr radialmenu.steps.nextProfile
            // # next
            // # zh_CN 下一个
            return profile.isEmpty() ? I18n.format("radialmenu.steps.nextProfile") : profile;
        }
        if (ActionTypes.SEQUENCE.equals(spec.type)) {
            // #tr radialmenu.steps.count
            // # %s steps
            // # zh_CN %s 个步骤
            return I18n.format("radialmenu.steps.count", Integer.valueOf(spec.steps == null ? 0 : spec.steps.size()));
        }
        return "";
    }
}
