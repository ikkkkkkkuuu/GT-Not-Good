package com.xyp.gtnotgood.client.radialmenu.client.gui.editor;

import net.minecraft.client.resources.I18n;

final class EditorRestrictions {

    private EditorRestrictions() {}

    static String builtInTip() {
        // #tr radialmenu.editor.builtIn.tip
        // # Built-in toolbelt: reorder its entry; manage tools from the wheel.
        // # zh_CN 内置工具腰带可调整入口顺序；请在轮盘中存取工具。
        return I18n.format("radialmenu.editor.builtIn.tip");
    }
}
