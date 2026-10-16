package com.xyp.gtnotgood.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.init.Blocks;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ScreenShotHelper;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.relauncher.Side;

@Mod(modid = "journeymapremoteclientqa", name = "JourneyMap Remote Client QA", version = "1")
public final class JourneyMapRemoteClientChecks {

    private GuiScreen screen;
    private Object menu;
    private boolean started;
    private boolean finished;
    private boolean awaitingDenial;
    private boolean permissionRejected;
    private int ticks;
    private int stage;
    private int frames;
    private int targetX;
    private int targetZ;

    @NetworkCheckHandler
    public boolean accept(Map<String, String> mods, Side side) {
        return true;
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.journeyMap.remote.client")) return;
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void chat(ClientChatReceivedEvent event) {
        if (awaitingDenial) inspect(event.message);
    }

    private void inspect(IChatComponent message) {
        if (
            message instanceof ChatComponentTranslation
                && ((ChatComponentTranslation) message).getKey().equals("commands.generic.permission")
        ) {
            permissionRejected = true;
        }
        for (IChatComponent sibling : message.getSiblings()) inspect(sibling);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (finished) {
            mc.shutdown();
            return;
        }
        try {
            if (++ticks > 6000) throw new AssertionError("Remote client timed out at stage " + stage);
            if (file("server-result.txt").isFile() && text("server-result.txt").startsWith("FAIL")) {
                throw new AssertionError(text("server-result.txt"));
            }
            mc.gameSettings.pauseOnLostFocus = false;
            if (!started && mc.currentScreen instanceof GuiMainMenu && file("ready.txt").isFile()) {
                started = true;
                ServerData server = new ServerData("JourneyMap Local QA", "127.0.0.1:25576");
                mc.setServerData(server);
                FMLClientHandler.instance().setupServerList();
                FMLClientHandler.instance().connectToServer(mc.currentScreen, server);
            }
            if (
                stage == 0 && mc.thePlayer != null
                    && mc.theWorld != null
                    && file("joined.txt").isFile()
                    && mc.theWorld.getBlock(-33, 70, -47) == Blocks.stone
            ) {
                require(mc.getIntegratedServer() == null, "client is connected to a separate dedicated process");
                openMap();
                advance(1);
            } else if (stage == 3 && file("denied.txt").isFile() && permissionRejected) {
                require(
                    Math.abs(mc.thePlayer.posX + 22.5) < 0.01 && Math.abs(mc.thePlayer.boundingBox.minY - 71) < 0.01
                        && Math.abs(mc.thePlayer.posZ + 36.5) < 0.01,
                    "the server permission error and 40 unchanged server ticks also leave the client unmoved");
                write("grant-op.txt", "JourneyMapQA");
                awaitingDenial = false;
                advance(4);
            } else if (stage == 4 && file("op-granted.txt").isFile()) {
                openMap();
                advance(5);
            } else if (
                stage == 7 && file("teleported.txt").isFile()
                    && MathHelper.floor_double(mc.thePlayer.posX) == targetX
                    && MathHelper.floor_double(mc.thePlayer.posZ) == targetZ
            ) {
                require(
                    Math.abs(mc.thePlayer.posX - targetX - 0.5) < 0.01
                        && Math.abs(mc.thePlayer.boundingBox.minY - 71) < 0.01
                        && Math.abs(mc.thePlayer.posZ - targetZ - 0.5) < 0.01,
                    "the dedicated server's exact safe /tp destination synchronizes to the remote client");
                advance(8);
            }
        } catch (Throwable error) {
            fail(error);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || stage == 0 || ++frames < 30) return;
        try {
            if (stage == 1 || stage == 5) {
                openMenu();
                advance(stage == 1 ? 2 : 6);
            } else if (stage == 2 || stage == 6) {
                screenshot(stage == 2 ? "non-op-menu.png" : "op-menu.png");
                boolean denied = stage == 2;
                if (denied) awaitingDenial = true;
                int[] bounds = (int[]) read(menu, "bounds");
                nativeClick(bounds[0] + 8, bounds[1] + 35, 0);
                require(Minecraft.getMinecraft().currentScreen == null, "the actual menu sends /tp and closes the map");
                write(denied ? "deny-attempt.txt" : "allowed-attempt.txt", "vanilla /tp sent by the native menu");
                advance(denied ? 3 : 7);
            } else if (stage == 8) {
                screenshot("remote-teleport.png");
                write("client-result.txt", "PASS");
                write("stop-server.txt", "client PASS");
                finished = true;
            }
        } catch (Throwable error) {
            fail(error);
        }
    }

    private void openMap() throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        Object manager = call(Class.forName("journeymap.client.ui.UIManager"), "getInstance");
        call(manager, "openFullscreenMap");
        screen = mc.currentScreen;
        require(screen != null && screen.getClass().getName().equals("journeymap.client.ui.fullscreen.Fullscreen"),
            "the remote client opens JourneyMap's real Fullscreen UI");
        Class<?> waypoint = Class.forName("journeymap.client.model.Waypoint");
        Object normal = Class.forName(waypoint.getName() + "$Type").getField("Normal").get(null);
        call(screen, "setZoom", 2);
        call(screen, "centerOn", call(waypoint, "at", -33, 71, -47, normal, 0));
    }

    private void openMenu() throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        Object grid = read(screen, "gridRenderer");
        call(grid, "center", call(grid, "getMapType"), -33.0, -47.0, 2);
        nativeClick(screen.width / 2, screen.height / 2, 1);
        menu = null;
        for (Field field : screen.getClass().getDeclaredFields()) {
            if (!field.getType().getName().equals("com.xyp.gtnotgood.client.journeymap.JourneyMapMenu")) continue;
            field.setAccessible(true);
            menu = field.get(screen);
        }
        require(menu != null, "the real mouse hook owns the remote menu");
        call(menu, "close");
        call(menu, "mouseClicked", screen.width / 2, screen.height / 2, 1, mc.displayWidth / 2, mc.displayHeight / 2);
        require((Boolean) read(menu, "open") && (Boolean) read(menu, "teleportEnabled"),
            "the remote menu defers command authorization to the dedicated server");
        Object target = read(menu, "target");
        targetX = ((Number) read(target, "x")).intValue();
        targetZ = ((Number) read(target, "z")).intValue();
        require(Math.abs(targetX + 33) <= 2 && Math.abs(targetZ + 47) <= 2,
            "the selected remote destination is on the prepared safe platform: " + targetX + ", " + targetZ);
        write("target.txt", targetX + "," + targetZ);
    }

    private void nativeClick(int x, int y, int button) throws Exception {
        try {
            call(screen, "mouseClicked", x, y, button);
        } catch (NoSuchMethodException missing) {
            call(screen, "func_73864_a", x, y, button);
        }
    }

    private static Object call(Object target, String name, Object... args) throws Exception {
        Class<?> type = target instanceof Class<?> ? (Class<?>) target : target.getClass();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.getName().equals(name) || method.getParameterTypes().length != args.length) continue;
                try {
                    method.setAccessible(true);
                    return method.invoke(target instanceof Class<?> ? null : target, args);
                } catch (IllegalArgumentException wrongOverload) {}
            }
        }
        throw new NoSuchMethodException(type.getName() + "." + name);
    }

    private static Object read(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private void advance(int next) {
        stage = next;
        frames = 0;
    }

    private static void screenshot(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(file("."), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private static File file(String name) {
        return new File(System.getProperty("gtng.journeyMap.remote.output"), name);
    }

    private static String text(String name) throws Exception {
        return new String(Files.readAllBytes(file(name).toPath()), StandardCharsets.UTF_8).trim();
    }

    private static void write(String name, String text) throws Exception {
        Files.write(file(name).toPath(), text.getBytes(StandardCharsets.UTF_8));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("JOURNEYMAP_REMOTE_CLIENT_QA: " + message);
    }

    private void fail(Throwable error) {
        error.printStackTrace();
        try {
            write("client-result.txt", "FAIL: stage " + stage + ": " + error);
            write("stop-server.txt", "client FAIL");
        } catch (Exception reportFailure) {
            reportFailure.printStackTrace();
        }
        finished = true;
    }
}
