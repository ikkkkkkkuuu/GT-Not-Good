package com.xyp.gtnotgood.client.rts;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovementInput;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/**
 * Captures exactly the client state borrowed by an RTS view. Restoration never installs an entity
 * from an unloaded world or steals focus from another GUI. Physics and server player state are untouched.
 */
final class RtsViewSnapshot {

    private final EntityClientPlayerMP player;
    private final EntityLivingBase previousCamera;
    private final EntityLivingBase ownedCamera;
    private final GuiScreen ownedScreen;
    private final MovementInput previousInput;
    private final MovementInput neutralInput = new MovementInput();
    private final int perspective;
    private final boolean bobbing;
    private final boolean focused;
    private final boolean grabbed;

    RtsViewSnapshot(Minecraft mc, EntityLivingBase camera, GuiScreen screen) {
        player = mc.thePlayer;
        previousCamera = mc.renderViewEntity;
        ownedCamera = camera;
        ownedScreen = screen;
        previousInput = player.movementInput;
        perspective = mc.gameSettings.thirdPersonView;
        bobbing = mc.gameSettings.viewBobbing;
        focused = mc.inGameHasFocus;
        grabbed = Mouse.isGrabbed();
    }

    void apply(Minecraft mc) {
        KeyBinding.unPressAllKeys();
        clearInput(previousInput);
        player.movementInput = neutralInput;
        mc.renderViewEntity = ownedCamera;
        mc.gameSettings.thirdPersonView = 0;
        mc.gameSettings.viewBobbing = false;
        mc.displayGuiScreen(ownedScreen);
        mc.inGameHasFocus = false;
        mc.mouseHelper.ungrabMouseCursor();
    }

    /** GuiOpenEvent calls this before Minecraft installs the replacement screen; do not reopen/close any GUI there. */
    void restore(Minecraft mc, boolean replacingScreen) {
        if (player.movementInput == neutralInput) player.movementInput = previousInput;
        clearInput(previousInput);
        KeyBinding.unPressAllKeys();
        if (mc.renderViewEntity == ownedCamera) {
            mc.renderViewEntity = previousCamera != null && previousCamera.worldObj == mc.theWorld
                && !previousCamera.isDead ? previousCamera : mc.thePlayer;
        }
        mc.gameSettings.thirdPersonView = perspective;
        mc.gameSettings.viewBobbing = bobbing;
        if (!replacingScreen && mc.currentScreen == ownedScreen) mc.displayGuiScreen(null);
        boolean mayFocus = !replacingScreen && mc.currentScreen == null
            && mc.thePlayer == player
            && mc.theWorld == player.worldObj
            && player.isEntityAlive()
            && Display.isActive();
        mc.inGameHasFocus = mayFocus && focused;
        if (mayFocus && grabbed) mc.mouseHelper.grabMouseCursor();
        else mc.mouseHelper.ungrabMouseCursor();
    }

    boolean owns(GuiScreen screen) {
        return screen == ownedScreen;
    }

    private static void clearInput(MovementInput input) {
        if (input == null) return;
        input.moveForward = 0;
        input.moveStrafe = 0;
        input.jump = false;
        input.sneak = false;
    }
}
