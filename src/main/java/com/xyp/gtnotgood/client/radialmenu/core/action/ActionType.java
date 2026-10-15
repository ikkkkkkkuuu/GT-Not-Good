package com.xyp.gtnotgood.client.radialmenu.core.action;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ActionType {

    public final String id;

    public final String labelKey;

    public final List<ActionField> fields;

    public final boolean chain;

    public ActionType(String id, String labelKey, boolean chain, ActionField... fields) {
        this.id = id;
        this.labelKey = labelKey;
        this.chain = chain;
        this.fields = Collections.unmodifiableList(Arrays.asList(fields));
    }

    public String tooltipKey() {
        switch (labelKey) {
            // #tr radialmenu.action.keybind
            // # Keybind
            // # zh_CN 按键绑定
            case "radialmenu.action.keybind":
                // #tr radialmenu.action.keybind.tip
                // # Press a keybinding, even one with no key assigned in the controls screen.
                // # zh_CN 按下一个按键绑定，即使该按键在控制设置中没有分配按键。
                return "radialmenu.action.keybind.tip";
            // #tr radialmenu.action.profileSwitch
            // # Profile
            // # zh_CN 配置
            case "radialmenu.action.profileSwitch":
                // #tr radialmenu.action.profileSwitch.tip
                // # Switch to another profile. Leave the profile blank to cycle to the next one.
                // # zh_CN 切换到另一个配置。留空配置即可循环切换到下一个。
                return "radialmenu.action.profileSwitch.tip";
            // #tr radialmenu.action.command
            // # Command
            // # zh_CN 命令
            case "radialmenu.action.command":
                // #tr radialmenu.action.command.tip
                // # Send chat lines or slash commands as if you had typed them.
                // # zh_CN 像手动输入一样发送聊天内容或斜杠命令。
                return "radialmenu.action.command.tip";
            // #tr radialmenu.action.submenu
            // # Submenu
            // # zh_CN 子菜单
            case "radialmenu.action.submenu":
                // #tr radialmenu.action.submenu.tip
                // # Open a nested wheel. Its entries are edited by opening it and shift-clicking a slot.
                // # zh_CN 打开一个嵌套转盘。打开它并按住 Shift 点击槽位即可编辑其中条目。
                return "radialmenu.action.submenu.tip";
            // #tr radialmenu.action.sequence
            // # Chain
            // # zh_CN 操作链
            case "radialmenu.action.sequence":
                // #tr radialmenu.action.sequence.tip
                // # Run the nested steps in order.
                // # zh_CN 按顺序执行嵌套步骤。
                return "radialmenu.action.sequence.tip";
            // #tr radialmenu.action.script
            // # Script
            // # zh_CN 脚本
            case "radialmenu.action.script":
                // #tr radialmenu.action.script.tip
                // # Run Lua to read server replies and create menus. See the scripting guide.
                // # zh_CN 运行 Lua 脚本，读取服务器响应并创建菜单。详见脚本指南。
                return "radialmenu.action.script.tip";
            default:
                return labelKey + ".tip";
        }
    }

    public ActionField field(String key) {
        for (ActionField field : fields) {
            if (field.key.equals(key)) {
                return field;
            }
        }
        return null;
    }

    public void applyDefaults(ActionSpec spec) {
        for (ActionField field : fields) {
            if (spec.getString(field.key, null) == null) {
                spec.set(field.key, field.defaultValue);
            }
        }
    }

    public boolean isComplete(ActionSpec spec) {
        for (ActionField field : fields) {
            if (field.isMissingFrom(spec)) {
                return false;
            }
        }
        return true;
    }

    public ActionSpec newSpec() {
        ActionSpec spec = new ActionSpec(id);
        applyDefaults(spec);
        return spec;
    }
}
