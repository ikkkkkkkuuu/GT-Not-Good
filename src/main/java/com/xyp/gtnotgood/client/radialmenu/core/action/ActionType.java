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
                // # Trigger a keybinding, including bindings with no assigned key
                // # zh_CN 触发按键绑定，也支持未分配按键的绑定
                return "radialmenu.action.keybind.tip";
            // #tr radialmenu.action.profileSwitch
            // # Profile
            // # zh_CN 配置
            case "radialmenu.action.profileSwitch":
                // #tr radialmenu.action.profileSwitch.tip
                // # Switch profiles. Leave blank to cycle to the next profile
                // # zh_CN 切换配置方案；留空则切换到下一个
                return "radialmenu.action.profileSwitch.tip";
            // #tr radialmenu.action.command
            // # Command
            // # zh_CN 命令
            case "radialmenu.action.command":
                // #tr radialmenu.action.command.tip
                // # Send chat messages or slash commands
                // # zh_CN 发送聊天消息或斜杠命令
                return "radialmenu.action.command.tip";
            // #tr radialmenu.action.submenu
            // # Submenu
            // # zh_CN 子菜单
            case "radialmenu.action.submenu":
                // #tr radialmenu.action.submenu.tip
                // # Open a submenu. Shift-click a slot in it to edit its action
                // # zh_CN 打开子菜单；Shift+点击其中的槽位以编辑动作
                return "radialmenu.action.submenu.tip";
            // #tr radialmenu.action.sequence
            // # Chain
            // # zh_CN 操作链
            case "radialmenu.action.sequence":
                // #tr radialmenu.action.sequence.tip
                // # Run the configured steps in order
                // # zh_CN 按顺序执行设定的步骤
                return "radialmenu.action.sequence.tip";
            // #tr radialmenu.action.script
            // # Script
            // # zh_CN 脚本
            case "radialmenu.action.script":
                // #tr radialmenu.action.script.tip
                // # Run Lua scripts to read server replies and create menus. See the scripting guide
                // # zh_CN 运行 Lua 脚本，读取服务器回复并创建菜单；详见脚本指南
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
