package com.xyp.gtnotgood.client.radialmenu.client.input;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import com.xyp.gtnotgood.client.radialmenu.client.action.ActionExecutors;
import com.xyp.gtnotgood.client.radialmenu.client.action.WithoutScreen;
import com.xyp.gtnotgood.client.radialmenu.client.gui.GuiRadialWheel;
import com.xyp.gtnotgood.client.radialmenu.client.gui.GuiStack;
import com.xyp.gtnotgood.client.radialmenu.client.gui.editor.GuiProfileManager;
import com.xyp.gtnotgood.client.radialmenu.client.profile.ProfileManager;
import com.xyp.gtnotgood.client.radialmenu.client.script.ScriptHost;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class WheelInputHandler {

    private boolean wheelKeyDown;

    private boolean autoBindApplied;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();

        if (mc.theWorld == null || mc.thePlayer == null) {
            if (wheelKeyDown) {
                wheelKeyDown = false;
            }
            autoBindApplied = false;
            KeybindStateTracker.releaseAll();

            if (ScriptHost.hasRunning()) {
                ScriptHost.cancelAll("left the world");
            }
            return;
        }

        if (!autoBindApplied) {
            autoBindApplied = true;
            applyAutoBind(mc);
        }

        KeybindStateTracker.onClientTick();
        if (ActionExecutors.runPending() && WithoutScreen.wheelIsUp()) {
            WithoutScreen.armNextTick();
        }

        ScriptHost.onClientTick();

        GuiStack.openRequested();

        if (WheelKeyBindings.nextProfile.isPressed()) {
            ProfileManager.cycle(1);
        }
        if (WheelKeyBindings.previousProfile.isPressed()) {
            ProfileManager.cycle(-1);
        }
        if (WheelKeyBindings.openEditor.isPressed()) {
            GuiStack.requestOpen(new GuiProfileManager());
        }

        boolean down = KeyInjector.isPhysicallyDown(WheelKeyBindings.openWheel);
        if (down != wheelKeyDown) {
            wheelKeyDown = down;
            if (down) {
                openWheel(mc);
            } else {
                releaseWheel(mc);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onTickFirst(TickEvent.ClientTickEvent event) {
        WithoutScreen.beginTick();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onTickLast(TickEvent.ClientTickEvent event) {
        WithoutScreen.endTick(event.phase == TickEvent.Phase.START);
    }

    private void applyAutoBind(Minecraft mc) {
        String serverAddress = null;
        String worldName = null;

        if (mc.isSingleplayer()) {
            if (mc.getIntegratedServer() != null) {
                worldName = mc.getIntegratedServer().getFolderName();
            }
        } else {
            ServerData data = mc.func_147104_D();
            if (data != null) {
                serverAddress = data.serverIP;
            }
        }

        ProfileManager.applyAutoBind(serverAddress, worldName);
    }

    private void openWheel(Minecraft mc) {
        if (mc.currentScreen != null) {
            return;
        }
        mc.displayGuiScreen(new GuiRadialWheel());
    }

    private void releaseWheel(Minecraft mc) {
        if (mc.currentScreen instanceof GuiRadialWheel) {
            ((GuiRadialWheel) mc.currentScreen).onWheelKeyReleased();
        }
    }
}
