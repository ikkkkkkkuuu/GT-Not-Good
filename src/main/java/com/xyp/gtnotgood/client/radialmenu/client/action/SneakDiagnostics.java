package com.xyp.gtnotgood.client.radialmenu.client.action;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import com.xyp.gtnotgood.client.radialmenu.RadialMenuMod;
import com.xyp.gtnotgood.client.radialmenu.client.input.KeyBuffer;
import com.xyp.gtnotgood.client.radialmenu.client.input.KeyInjector;
import com.xyp.gtnotgood.client.radialmenu.config.RadialMenuConfig;

public final class SneakDiagnostics {

    private SneakDiagnostics() {}

    public static void log(String what, KeyBinding binding) {
        if (!RadialMenuConfig.logKeyState) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP player = mc.thePlayer;
        KeyBinding sneak = mc.gameSettings.keyBindSneak;

        StringBuilder line = new StringBuilder("key state [").append(what).append("] ");
        line.append("buffer=").append(KeyBuffer.isAvailable());
        line.append(" binding=").append(binding == null ? "none" : binding.getKeyDescription()).append("/code=")
            .append(binding == null ? 0 : binding.getKeyCode()).append("/pressed=")
            .append(binding != null && binding.getIsKeyPressed())

            .append("/raw=").append(binding != null && KeyInjector.isHeld(binding));
        line.append(" sneakCode=").append(sneak.getKeyCode()).append("/pressed=").append(sneak.getIsKeyPressed())
            .append("/raw=").append(KeyInjector.isHeld(sneak)).append("/same=").append(binding == sneak);

        int sameDescription = 0;
        for (KeyBinding candidate : com.xyp.gtnotgood.client.radialmenu.client.input.KeyBindingLookup.all()) {
            if (sneak.getKeyDescription().equals(candidate.getKeyDescription())) {
                sameDescription++;
            }
        }
        line.append(" sneakBindings=").append(sameDescription);
        line.append(" lshift=").append(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)).append("/rshift=")
            .append(Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
        line.append(" focus=").append(mc.inGameHasFocus).append("/screen=")
            .append(mc.currentScreen == null ? "none" : mc.currentScreen.getClass().getSimpleName());

        if (player == null) {
            line.append(" player=none");
        } else {
            line.append(" isSneaking=").append(player.isSneaking()).append(" movementSneak=")
                .append(player.movementInput == null ? "none" : String.valueOf(player.movementInput.sneak))
                .append(" movementInput=")
                .append(player.movementInput == null ? "none" : player.movementInput.getClass().getName());
        }

        RadialMenuMod.LOG.info(line.toString());
    }
}
