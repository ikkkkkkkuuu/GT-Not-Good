package com.xyp.gtnotgood.client.radialmenu.client.action;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.ClientCommandHandler;

import com.xyp.gtnotgood.client.radialmenu.RadialMenuMod;
import com.xyp.gtnotgood.client.radialmenu.core.action.Placeholders;

public final class ChatSender {

    private ChatSender() {}

    public static void send(String rawText) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return;
        }
        String text = Placeholders.apply(rawText, context(mc.thePlayer)).trim();
        if (text.isEmpty()) {
            return;
        }

        if (ClientCommandHandler.instance.executeCommand(mc.thePlayer, text) != 0) {
            RadialMenuMod.LOG.debug("Handled client-side: " + text);
            return;
        }

        mc.thePlayer.sendChatMessage(text);
        RadialMenuMod.LOG.debug("Sent: " + text);
    }

    public static Map<String, String> context(EntityPlayer player) {
        Map<String, String> values = new HashMap<>();
        values.put(Placeholders.PLAYER, player.getCommandSenderName());
        values.put(Placeholders.DIMENSION, Integer.toString(player.dimension));
        values.put(Placeholders.X, Integer.toString((int) Math.floor(player.posX)));
        values.put(Placeholders.Y, Integer.toString((int) Math.floor(player.posY)));
        values.put(Placeholders.Z, Integer.toString((int) Math.floor(player.posZ)));
        return values;
    }
}
