package com.xyp.gtnotgood.client.radialmenu.client.gui.editor;

import net.minecraft.client.resources.I18n;

final class EditorRestrictions {

    private EditorRestrictions() {}

    static String builtInTip() {
        // #tr radialmenu.editor.builtIn.tip
        // # Manage stored items from the wheel; edit the toolbelt entry separately.
        // # zh_CN 请在轮盘中存取物品；腰带入口可单独编辑。
        return I18n.format("radialmenu.editor.builtIn.tip");
    }

    static String resizeTip() {
        // #tr radialmenu.editor.toolbelt.shared
        // # All profiles share the same stored items and slot count.
        // # zh_CN 所有配置共用同一份物品库存和槽位数量。
        String shared = I18n.format("radialmenu.editor.toolbelt.shared");
        // #tr radialmenu.editor.toolbelt.shrink
        // # Empty the slots being removed before reducing the slot count.
        // # zh_CN 缩减槽位前，请先清空将被移除的槽位。
        return shared + "\n" + I18n.format("radialmenu.editor.toolbelt.shrink");
    }
}
