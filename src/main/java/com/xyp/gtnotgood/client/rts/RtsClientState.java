package com.xyp.gtnotgood.client.rts;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import org.lwjgl.opengl.Display;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.packet.RtsSessionMessage;
import com.xyp.gtnotgood.common.rts.session.RtsSessionLease;
import com.xyp.gtnotgood.config.Config;

/**
 * Single client-thread owner for a pending/authorized RTS session and its borrowed view.
 * Phase 2 exposes lifecycle operations for the later faithful camera/GUI and the opt-in runtime checks;
 * it deliberately does not substitute an unfinished production screen for the upstream interface.
 */
public final class RtsClientState {

    public static final RtsClientState INSTANCE = new RtsClientState();
    private final RtsSessionState state = new RtsSessionState();
    private Object connection;
    private World world;
    private EntityClientPlayerMP player;
    private RtsViewSnapshot snapshot;
    private RtsCameraController cameraController;
    private long ticks;

    private RtsClientState() {}

    /** Creates the detached production camera after authorization, sharing the normal restoration path. */
    public void attachView(GuiScreen screen) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!validContext(mc) || state.phase() != RtsSessionState.Phase.READY) {
            throw new IllegalStateException("RTS camera requires a live server lease");
        }
        RtsCameraController controller = new RtsCameraController(world, state.lease(), player.rotationYaw);
        attachView(controller.entity(), screen);
        if (state.phase() == RtsSessionState.Phase.ACTIVE) cameraController = controller;
    }

    /** Returns the active camera input endpoint, or null when no camera is owned. */
    public RtsCameraController camera() {
        return cameraController;
    }

    /** Runs before world rendering; never takes the view back from another mod. */
    public void renderFrame() {
        if (cameraController == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!validContext(mc) || mc.renderViewEntity != cameraController.entity()) {
            close();
            return;
        }
        if (!Display.isActive()) cameraController.resetInput();
        cameraController.frame(System.nanoTime());
    }

    /** Requests server authorization without changing camera, input, mouse, or world state. */
    public boolean requestOpen() {
        Minecraft mc = Minecraft.getMinecraft();
        if (!Config.enableRTSBuilding || state.phase() != RtsSessionState.Phase.CLOSED
            || mc.thePlayer == null
            || mc.theWorld == null
            || mc.getNetHandler() == null
            || !mc.thePlayer.isEntityAlive()
            || mc.currentScreen != null) return false;
        connection = mc.getNetHandler();
        world = mc.theWorld;
        player = mc.thePlayer;
        long request = state.begin(player.dimension, ticks);
        send(RtsSessionMessage.control(RtsSessionMessage.OPEN, request, 0, player.dimension));
        return true;
    }

    /**
     * Attaches the actual camera and screen only after authorization. Called on the client thread.
     * The camera must be a separate entity in the current client world; the player is never moved.
     *
     * @param camera detached render camera owned by the camera subsystem
     * @param screen actual RTS screen owned by the GUI subsystem
     */
    public void attachView(EntityLivingBase camera, GuiScreen screen) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!validContext(mc) || state.phase() != RtsSessionState.Phase.READY
            || camera == null
            || camera == player
            || camera.worldObj != world
            || screen == null
            || mc.currentScreen != null) {
            throw new IllegalStateException("Cannot attach RTS view without a live lease and matching world");
        }
        state.activate();
        snapshot = new RtsViewSnapshot(mc, camera, screen);
        try {
            snapshot.apply(mc);
            // Another mod may cancel or replace GuiOpenEvent. Never retain invisible input ownership.
            if (mc.currentScreen != screen || mc.renderViewEntity != camera) close();
        } catch (RuntimeException failure) {
            close();
            throw failure;
        }
    }

    /**
     * Receives replies after the proxy schedules them on the client thread, with their original connection identity.
     */
    public void receive(RtsSessionMessage message, Object sourceConnection) {
        Minecraft mc = Minecraft.getMinecraft();
        if (connection != sourceConnection || !validContext(mc)) return;
        if (state.receive(message, ticks) && state.phase() == RtsSessionState.Phase.CLOSED) restore(false);
    }

    public void tick() {
        ticks++;
        if (state.phase() == RtsSessionState.Phase.CLOSED) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!Config.enableRTSBuilding || !validContext(mc) || state.expired(ticks)) {
            close();
            return;
        }
        RtsSessionLease lease = state.lease();
        if (cameraController != null) {
            if (!Display.isActive()) cameraController.resetInput();
            cameraController.tick();
        }
        if (lease != null && ticks % 20 == 0) {
            send(RtsSessionMessage.control(RtsSessionMessage.HEARTBEAT, lease.requestId, lease.token, lease.dimension));
        }
    }

    private boolean validContext(Minecraft mc) {
        return connection != null && mc.getNetHandler() == connection
            && mc.theWorld == world
            && mc.thePlayer == player
            && player != null
            && player.dimension == state.dimension()
            && player.isEntityAlive();
    }

    public void close() {
        close(false);
    }

    private void close(boolean replacingScreen) {
        if (state.phase() == RtsSessionState.Phase.CLOSED && snapshot == null) return;
        RtsSessionMessage closing = null;
        if (Minecraft.getMinecraft()
            .getNetHandler() == connection && connection != null) {
            RtsSessionLease lease = state.lease();
            closing = RtsSessionMessage.control(
                RtsSessionMessage.CLOSE,
                state.requestId(),
                lease == null ? 0 : lease.token,
                state.dimension());
        }
        state.close();
        restore(replacingScreen);
        // A failing/disconnecting transport must never prevent local input and camera restoration.
        if (closing != null) send(closing);
    }

    private void restore(boolean replacingScreen) {
        if (cameraController != null) cameraController.resetInput();
        cameraController = null;
        RtsViewSnapshot captured = snapshot;
        snapshot = null;
        connection = null;
        world = null;
        player = null;
        if (captured != null) captured.restore(Minecraft.getMinecraft(), replacingScreen);
    }

    /** Called before another screen takes ownership, including ESC, menu changes and death screens. */
    public void screenOpening(GuiScreen next) {
        if (snapshot != null && !snapshot.owns(next)) close(true);
        else if (snapshot == null && next != null && state.phase() != RtsSessionState.Phase.CLOSED) close(true);
    }

    /** A disconnect event is scoped to its connection, so a delayed old event cannot close a new session. */
    public void disconnected(Object networkManager) {
        Minecraft mc = Minecraft.getMinecraft();
        if (connection != null && (mc.getNetHandler() == null || mc.getNetHandler()
            .getNetworkManager() == networkManager)) close();
    }

    public void worldUnloading(World unloading) {
        if (world == unloading) close();
    }

    public RtsSessionState.Phase phase() {
        return state.phase();
    }

    public RtsSessionLease lease() {
        return state.lease();
    }

    private static void send(RtsSessionMessage message) {
        GTNotGood.channel.sendToServer(message);
    }
}
