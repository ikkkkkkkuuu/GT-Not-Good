package com.xyp.gtnotgood.client.radialmenu.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class WheelOverlayHandler {

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            return;
        }
        if (Minecraft.getMinecraft().currentScreen instanceof GuiRadialWheel) {
            event.setCanceled(true);
        }
    }
}
