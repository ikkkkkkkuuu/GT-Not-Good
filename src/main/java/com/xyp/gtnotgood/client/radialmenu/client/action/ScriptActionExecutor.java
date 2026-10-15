package com.xyp.gtnotgood.client.radialmenu.client.action;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.xyp.gtnotgood.client.radialmenu.client.script.ScriptHost;
import com.xyp.gtnotgood.client.radialmenu.config.RadialMenuConfig;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSpec;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionTypes;

public class ScriptActionExecutor implements IActionExecutor {

    @Override
    public String typeId() {
        return ActionTypes.SCRIPT;
    }

    @Override
    public boolean execute(ActionSpec spec) {
        if (!RadialMenuConfig.enableScripts) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null) {
                // #tr radialmenu.script.disabled
                // # Scripts are disabled in the radial menu settings.
                // # zh_CN 快捷轮盘设置已禁用脚本。
                String message = StatCollector.translateToLocal("radialmenu.script.disabled");
                mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + message));
            }
            return false;
        }
        return ScriptHost.start(spec);
    }
}
