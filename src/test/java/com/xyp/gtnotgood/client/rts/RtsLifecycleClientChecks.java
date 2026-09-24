package com.xyp.gtnotgood.client.rts;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.MovementInput;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.rts.session.RtsSessionManager;
import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Opt-in lifecycle checks in a disposable world, following the project's existing real-client QA pattern. */
@Mod(
    modid = "rtslifecycleqa",
    name = "RTS Lifecycle QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class RtsLifecycleClientChecks {

    private int step;
    private int age;
    private int total;
    private String save;
    private EntityLivingBase previousCamera;
    private MovementInput previousInput;
    private volatile int serverAction;
    private volatile boolean leasePresent;
    private int oldDimension;
    private boolean finished;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.rts.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        var player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        leasePresent = RtsSessionManager.INSTANCE.getLease(player) != null;
        int action = serverAction;
        serverAction = 0;
        if (action == 1) player.setHealth(0);
        if (action == 2) server.getConfigurationManager()
            .transferPlayerToDimension(player, -1);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (++total > 3600) throw new AssertionError("RTS_QA timeout at step " + step);
        age++;
        mc.gameSettings.pauseOnLostFocus = false;
        RtsClientState state = RtsClientState.INSTANCE;
        switch (step) {
            case 0:
                if (mc.theWorld == null && mc.currentScreen != null) {
                    save = "rts-lifecycle-qa-" + System.currentTimeMillis();
                    Config.enableRTSBuilding = true;
                    mc.launchIntegratedServer(
                        save,
                        "RTS Lifecycle QA",
                        new WorldSettings(25L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
                    next();
                }
                break;
            case 1:
                if (mc.thePlayer != null && mc.currentScreen == null && age > 80) {
                    mc.gameSettings.thirdPersonView = 2;
                    mc.gameSettings.viewBobbing = true;
                    require(state.requestOpen(), "request first session");
                    next();
                }
                break;
            case 2:
                if (ready()) {
                    require(leasePresent, "server owns authorized lease");
                    attach(mc);
                    next();
                }
                break;
            case 3:
                if (age > 30) {
                    require(state.phase() == RtsSessionState.Phase.ACTIVE, "heartbeat keeps view active");
                    require(mc.thePlayer.movementInput != previousInput, "neutral player input installed");
                    require(mc.renderViewEntity != mc.thePlayer, "render camera separated from player");
                    require(
                        Math.abs(mc.renderViewEntity.posY - mc.thePlayer.posY) > 10,
                        "camera did not teleport player");
                    ScreenShotHelper.saveScreenshot(
                        mc.mcDataDir,
                        "rts-lifecycle-view.png",
                        mc.displayWidth,
                        mc.displayHeight,
                        mc.getFramebuffer());
                    ((ProbeScreen) mc.currentScreen).escape();
                    restored(mc);
                    next();
                }
                break;
            case 4:
                if (age > 30) {
                    require(!leasePresent, "ESC revokes server lease");
                    require(state.requestOpen(), "request that will be cancelled immediately");
                    state.close();
                    next();
                }
                break;
            case 5:
                if (age > 35) {
                    require(state.phase() == RtsSessionState.Phase.CLOSED && !leasePresent, "late grant cannot reopen");
                    require(state.requestOpen(), "request GUI replacement session");
                    next();
                }
                break;
            case 6:
                if (ready()) {
                    attach(mc);
                    GuiIngameMenu menu = new GuiIngameMenu();
                    mc.displayGuiScreen(menu);
                    restored(mc);
                    require(mc.currentScreen == menu, "replacement GUI retained");
                    mc.displayGuiScreen(null);
                    next();
                }
                break;
            case 7:
                if (age > 25) {
                    require(!leasePresent, "GUI replacement revokes server lease");
                    require(state.requestOpen(), "request death session");
                    next();
                }
                break;
            case 8:
                if (ready()) {
                    attach(mc);
                    serverAction = 1;
                    next();
                }
                break;
            case 9:
                if (mc.thePlayer.getHealth() <= 0 && age > 30) {
                    require(state.phase() == RtsSessionState.Phase.CLOSED, "death closes client session");
                    require(!leasePresent, "death revokes server lease");
                    restored(mc);
                    mc.thePlayer.respawnPlayer();
                    mc.displayGuiScreen(null);
                    next();
                }
                break;
            case 10:
                if (mc.thePlayer.isEntityAlive() && mc.currentScreen == null && age > 60) {
                    oldDimension = mc.thePlayer.dimension;
                    require(state.requestOpen(), "request dimension session");
                    next();
                }
                break;
            case 11:
                if (ready()) {
                    attach(mc);
                    serverAction = 2;
                    next();
                }
                break;
            case 12:
                if (mc.thePlayer != null && mc.thePlayer.dimension != oldDimension && age > 60) {
                    require(
                        state.phase() == RtsSessionState.Phase.CLOSED && !leasePresent,
                        "dimension change closes both sides");
                    require(mc.renderViewEntity == mc.thePlayer, "new dimension owns its own player camera");
                    if (mc.currentScreen == null) {
                        require(state.requestOpen(), "request disconnect session");
                        next();
                    }
                }
                break;
            case 13:
                if (ready()) {
                    attach(mc);
                    mc.theWorld.sendQuittingDisconnectingPacket();
                    mc.loadWorld(null);
                    mc.displayGuiScreen(new GuiMainMenu());
                    next();
                }
                break;
            case 14:
                if (mc.theWorld == null && age > 40) {
                    require(
                        state.phase() == RtsSessionState.Phase.CLOSED && state.lease() == null,
                        "disconnect releases world/session references");
                    mc.launchIntegratedServer(save, "RTS Lifecycle QA", null);
                    next();
                }
                break;
            case 15:
                if (mc.thePlayer != null && mc.currentScreen == null && age > 80) {
                    require(state.requestOpen(), "reconnect permits fresh session");
                    next();
                }
                break;
            case 16:
                if (ready()) {
                    attach(mc);
                    state.close();
                    state.close();
                    restored(mc);
                    next();
                }
                break;
            case 17:
                if (age > 30) {
                    require(!leasePresent, "repeated close is idempotent on both sides");
                    Config.enableRTSBuilding = false;
                    require(!state.requestOpen(), "disabled client cannot open session");
                    Files.write(
                        new File("rts-lifecycle-qa-result.txt").toPath(),
                        "PASS".getBytes(StandardCharsets.UTF_8));
                    GTNotGood.LOG.info(
                        "RTS_QA PASS: lease, camera/input restore, ESC, replacement GUI, cancel, death, dimension, disconnect, reconnect");
                    finished = true;
                    mc.shutdown();
                }
                break;
            default:
                throw new AssertionError("Unexpected QA step");
        }
    }

    private boolean ready() {
        return RtsClientState.INSTANCE.phase() == RtsSessionState.Phase.READY;
    }

    private void attach(Minecraft mc) {
        previousCamera = mc.renderViewEntity;
        previousInput = mc.thePlayer.movementInput;
        RtsClientState.INSTANCE.attachView(new ProbeScreen());
    }

    private void restored(Minecraft mc) {
        require(RtsClientState.INSTANCE.phase() == RtsSessionState.Phase.CLOSED, "session closed");
        require(mc.renderViewEntity == previousCamera, "original camera restored");
        require(mc.thePlayer.movementInput == previousInput, "original movement input restored");
        require(mc.gameSettings.thirdPersonView == 2 && mc.gameSettings.viewBobbing, "view settings restored");
    }

    private void next() {
        GTNotGood.LOG.info("RTS_QA step {} complete", step++);
        age = 0;
    }

    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError("RTS_QA: " + label);
    }

    /** Test fixture only; no placeholder screen is registered in production. */
    private static final class ProbeScreen extends GuiScreen {

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }

        void escape() {
            keyTyped((char) 0, org.lwjgl.input.Keyboard.KEY_ESCAPE);
        }
    }
}
