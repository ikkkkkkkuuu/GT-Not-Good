package com.xyp.gtnotgood.utils.event;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.items.toolbelt.ConfigData;
import com.xyp.gtnotgood.common.items.toolbelt.client.RadialMenuScreen;
import com.xyp.gtnotgood.utils.keybind.KeyBindManager;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Opens and services the client-side tool belt radial menu.
 */
public class ToolBeltClientEvents {

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null || mc.currentScreen != null) return;

        if (KeyBindManager.openToolMenuKeybind.isPressed()) {
            ItemStack inHand = player.getHeldItem();
            if (inHand == null || ConfigData.isItemStackAllowed(inHand)) {
                GTNotGood.LOG.info("Opening tool belt radial menu");
                mc.displayGuiScreen(new RadialMenuScreen(player));
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen instanceof RadialMenuScreen) {
            ((RadialMenuScreen) mc.currentScreen).handleKeyInput();
        }
    }
}
