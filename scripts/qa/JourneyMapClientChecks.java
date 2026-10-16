package com.xyp.gtnotgood.qa;

import java.awt.geom.Point2D;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.EmptyChunk;

import org.lwjgl.input.Keyboard;

import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mod(
    modid = "journeymapqa",
    name = "JourneyMap QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class JourneyMapClientChecks {

    private static final String FULLSCREEN = "journeymap.client.ui.fullscreen.Fullscreen";
    private static final String WAYPOINT = "journeymap.client.model.Waypoint";
    private static final String MENU = "com.xyp.gtnotgood.client.journeymap.JourneyMapMenu";
    private static final int CENTER_X = -33;
    private static final int CENTER_Z = -47;
    private static final int FLOOR_Y = 70;

    private boolean started;
    private volatile boolean finished;
    private volatile boolean serverReady;
    private volatile boolean teleportRequested;
    private volatile boolean serverTeleported;
    private volatile boolean disablePermissionsRequested;
    private volatile boolean permissionsDisabled;
    private volatile boolean restorePermissionsRequested;
    private volatile boolean permissionsRestored;
    private volatile Throwable failure;
    private volatile int expectedX;
    private volatile int expectedZ;
    private int ticks;
    private int serverTicks;
    private int stage;
    private int frames;
    private int coordinateCase;
    private int checks;
    private int waypointX;
    private int waypointZ;
    private GuiScreen screen;
    private Object originalMapType;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.journeyMap.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (finished) {
            mc.shutdown();
            return;
        }
        try {
            if (failure != null) throw new AssertionError("Server checks failed", failure);
            if (++ticks > 6000) throw new AssertionError("Client timed out at stage " + stage);
            mc.gameSettings.pauseOnLostFocus = false;
            if (!started && mc.theWorld == null && mc.currentScreen instanceof GuiMainMenu) {
                started = true;
                if (Boolean.getBoolean("gtng.journeyMap.qa.absent")) {
                    advance(-1);
                    return;
                }
                require(Loader.isModLoaded(ModList.JourneyMap.getID()), "the real JourneyMap mod is loaded");
                WorldSettings settings = new WorldSettings(73L, WorldSettings.GameType.CREATIVE, false, false,
                    WorldType.FLAT).enableCommands();
                mc.launchIntegratedServer("journeymap-qa-" + System.currentTimeMillis(), "JourneyMap QA", settings);
            }
            if (
                serverReady && stage == 0
                    && mc.thePlayer != null
                    && mc.theWorld != null
                    && mc.theWorld.getChunkProvider().chunkExists(CENTER_X >> 4, CENTER_Z >> 4)
            ) {
                openMap(2, 2, CENTER_X, CENTER_Z);
                advance(1);
            }
        } catch (Throwable error) {
            fail(error);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            if (++serverTicks > 2400) throw new AssertionError("Server timed out at stage " + stage);
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            if (!serverReady) {
                World world = player.getServerForPlayer();
                for (int x = CENTER_X - 12; x <= CENTER_X + 12; x++) {
                    for (int z = CENTER_Z - 12; z <= CENTER_Z + 12; z++) {
                        world.setBlock(x, FLOOR_Y, z, Blocks.stone, 0, 2);
                        for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 5; y++) world.setBlockToAir(x, y, z);
                    }
                }
                player.capabilities.allowFlying = true;
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
                player.setPositionAndUpdate(CENTER_X + 10.5, FLOOR_Y + 1, CENTER_Z + 10.5);
                serverReady = true;
            }
            if (
                teleportRequested && !serverTeleported
                    && MathHelper.floor_double(player.posX) == expectedX
                    && MathHelper.floor_double(player.posZ) == expectedZ
            ) {
                require(Math.abs(player.posY - (FLOOR_Y + 1)) < 0.01,
                    "the real server receives /tp and lands above the solid platform: " + player.posY);
                serverTeleported = true;
            }
            if (disablePermissionsRequested && !permissionsDisabled) {
                write(server.worldServers[0].getWorldInfo(), "allowCommands", false);
                server.getConfigurationManager().setCommandsAllowedForAll(false);
                permissionsDisabled = true;
            }
            if (restorePermissionsRequested && !permissionsRestored) {
                write(server.worldServers[0].getWorldInfo(), "allowCommands", true);
                permissionsRestored = true;
            }
        } catch (Throwable error) {
            failure = error;
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || stage == 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (failure != null) throw new AssertionError("Server checks failed", failure);
            if (++frames < 30) return;
            switch (stage) {
                case -1 -> {
                    checkAbsent();
                    screenshot("without-journeymap.png");
                    finish("PASS");
                }
                case 1 -> {
                    nativeClick(screen, screen.width / 2, screen.height / 2, 1);
                    require(isOpen(), "the native Fullscreen mouse method opens the injected menu");
                    requireBounds();
                    advance(2);
                }
                case 2 -> {
                    screenshot("right-click-menu.png");
                    nativeKey(screen, Keyboard.KEY_ESCAPE);
                    require(!isOpen() && mc.currentScreen == screen,
                        "Escape closes the menu and retains the native map");
                    nativeClick(screen, screen.width / 2, screen.height / 2, 1);
                    nativeClick(screen, screen.width / 2, screen.height / 2, 1);
                    require(!isOpen(), "a second right click dismisses the menu");
                    nativeClick(screen, screen.width / 2, screen.height / 2, 1);
                    nativeKey(screen, Keyboard.KEY_F6);
                    require(!isOpen() && mc.currentScreen == screen,
                        "an unrelated key dismisses the menu while the original map handler remains active");
                    coordinateCase = 0;
                    openCoordinateCase();
                    advance(3);
                }
                case 3 -> {
                    checkCoordinates();
                    checkEdges();
                    if (coordinateCase == 4) {
                        int[] point = availablePoint(screen.width - 3, screen.height - 3);
                        openAt(point[0], point[1]);
                        advance(11);
                    } else {
                        nextCoordinateCase();
                    }
                }
                case 4 -> {
                    checkWaypointSelection();
                    openAt(screen.width / 2, screen.height / 2);
                    Object target = read(menu(), "target");
                    waypointX = number(target, "x");
                    waypointZ = number(target, "z");
                    select(1);
                    require(
                        mc.currentScreen != null && mc.currentScreen.getClass().getName()
                            .equals("journeymap.client.ui.waypoint.WaypointEditor"),
                        "the second native menu click opens JourneyMap's actual waypoint editor");
                    Object waypoint = read(mc.currentScreen, "editedWaypoint");
                    require(
                        (Integer) invoke(waypoint, "getX", new Class<?>[0]) == waypointX
                            && (Integer) invoke(waypoint, "getZ", new Class<?>[0]) == waypointZ,
                        "the native waypoint editor receives the selected map coordinates");
                    advance(5);
                }
                case 5 -> {
                    screenshot("waypoint-editor.png");
                    openMap(2, 2, CENTER_X, CENTER_Z);
                    advance(6);
                }
                case 6 -> {
                    openAt(screen.width / 2, screen.height / 2);
                    Object target = read(menu(), "target");
                    expectedX = number(target, "x");
                    expectedZ = number(target, "z");
                    require(Math.abs(expectedX - CENTER_X) <= 2 && Math.abs(expectedZ - CENTER_Z) <= 2,
                        "the loaded teleport target is on the prepared negative-coordinate platform");
                    require(mc.theWorld.getBlock(expectedX, FLOOR_Y, expectedZ) == Blocks.stone,
                        "the destination's solid floor is actually loaded on the client");
                    require(
                        MathHelper.floor_double(mc.thePlayer.posX) != expectedX
                            || MathHelper.floor_double(mc.thePlayer.posZ) != expectedZ,
                        "the player starts away from the destination");
                    teleportRequested = true;
                    select(0);
                    require(mc.currentScreen == null, "a safe menu teleport closes the map without opening chat");
                    advance(7);
                }
                case 7 -> {
                    if (
                        !serverTeleported || MathHelper.floor_double(mc.thePlayer.posX) != expectedX
                            || MathHelper.floor_double(mc.thePlayer.posZ) != expectedZ
                    ) return;
                    require(Math.abs(mc.thePlayer.boundingBox.minY - (FLOOR_Y + 1)) < 0.01,
                        "the real server position synchronizes back to the client");
                    screenshot("teleport-result.png");
                    openMap(2, 2, 1_000_000, 1_000_000);
                    advance(8);
                }
                case 8 -> {
                    openAt(screen.width / 2, screen.height / 2);
                    Object target = read(menu(), "target");
                    require(
                        mc.theWorld.getChunkFromChunkCoords(number(target, "x") >> 4,
                            number(target, "z") >> 4) instanceof EmptyChunk,
                        "the unknown-terrain fixture is not loaded on the client");
                    select(0);
                    require(mc.currentScreen instanceof GuiYesNo,
                        "unknown terrain opens an actual confirmation screen before sending /tp");
                    advance(9);
                }
                case 9 -> {
                    GuiButton cancel = button(mc.currentScreen, 1);
                    if (!cancel.enabled) return;
                    screenshot("unknown-terrain-confirmation.png");
                    nativeClick(mc.currentScreen, cancel.xPosition + cancel.width / 2,
                        cancel.yPosition + cancel.height / 2, 0);
                    require(mc.currentScreen == screen, "cancel returns to the existing JourneyMap screen");
                    advance(10);
                }
                case 10 -> {
                    require(
                        MathHelper.floor_double(mc.thePlayer.posX) == expectedX
                            && MathHelper.floor_double(mc.thePlayer.posZ) == expectedZ,
                        "cancelling unknown-terrain teleport leaves the player at the verified server destination");
                    Object grid = read(screen, "gridRenderer");
                    originalMapType = read(grid, "mapType");
                    Object otherDimension = invoke(Class.forName("journeymap.client.model.MapType"), "day",
                        new Class<?>[] { int.class }, mc.thePlayer.dimension == 0 ? -1 : 0);
                    write(grid, "mapType", otherDimension);
                    openAt(screen.width / 2, screen.height / 2);
                    require(
                        number(read(menu(), "target"), "dimension") != mc.thePlayer.dimension
                            && !(Boolean) read(menu(), "teleportEnabled"),
                        "a native map type for another dimension disables teleport");
                    advance(15);
                }
                case 11 -> {
                    screenshot("corner-menu.png");
                    closeMenu();
                    nextCoordinateCase();
                }
                case 12 -> {
                    if (!permissionsDisabled) return;
                    require(
                        !(Boolean) invoke(Class.forName("journeymap.client.command.CmdTeleportWaypoint"), "isPermitted",
                            new Class<?>[] { Minecraft.class }, mc),
                        "the native JourneyMap permission check rejects the non-cheat single-player fixture");
                    openAt(screen.width / 2, screen.height / 2);
                    require(!(Boolean) read(menu(), "teleportEnabled"),
                        "the menu disables teleport without permission");
                    advance(13);
                }
                case 13 -> {
                    screenshot("permission-denied.png");
                    select(0);
                    require(mc.currentScreen == screen,
                        "clicking the disabled teleport item keeps the native map open");
                    restorePermissionsRequested = true;
                    advance(14);
                }
                case 14 -> {
                    if (!permissionsRestored) return;
                    require(
                        MathHelper.floor_double(mc.thePlayer.posX) == expectedX
                            && MathHelper.floor_double(mc.thePlayer.posZ) == expectedZ,
                        "cross-dimension and denied-permission clicks never move the player");
                    finish("PASS");
                }
                case 15 -> {
                    screenshot("other-dimension.png");
                    select(0);
                    require(mc.currentScreen == screen, "the cross-dimension teleport item cannot send a command");
                    write(read(screen, "gridRenderer"), "mapType", originalMapType);
                    checkDisabledConfig();
                    disablePermissionsRequested = true;
                    openMap(2, 2, CENTER_X, CENTER_Z);
                    advance(12);
                }
                default -> throw new AssertionError("Unknown stage " + stage);
            }
        } catch (Throwable error) {
            fail(error);
        }
    }

    private void checkAbsent() throws Exception {
        require(!Loader.isModLoaded(ModList.JourneyMap.getID()), "the client reaches the main menu without JourneyMap");
        require(Launch.classLoader.getClassBytes(FULLSCREEN) == null, "the optional Fullscreen class is absent");
        Object loader = Class.forName("com.xyp.gtnotgood.loader.LateMixinsLoader").getConstructor().newInstance();
        List<?> mixins = (List<?>) invoke(loader, "getMixins", new Class<?>[] { Set.class }, Collections.emptySet());
        require(mixins.stream().noneMatch(mixin -> mixin.toString().startsWith("journeymap.")),
            "absent JourneyMap registers no optional menu mixin");
        Map<?, ?> loaded = (Map<?, ?>) read(Launch.classLoader, "cachedClasses");
        for (Object name : loaded.keySet().toArray()) {
            String className = name.toString();
            if (
                className.startsWith("com.xyp.gtnotgood.client.journeymap.")
                    || className.startsWith("com.xyp.gtnotgood.mixins.late.journeymap.")
            ) {
                throw new AssertionError("Optional client class was loaded without JourneyMap: " + className);
            }
        }
        require(true, "no JourneyMap menu or mixin class is defined in the absent-mod client");
    }

    private void openCoordinateCase() throws Exception {
        openMap(coordinateCase / 3 + 1, coordinateCase % 3 * 2, CENTER_X, CENTER_Z);
    }

    private void nextCoordinateCase() throws Exception {
        if (++coordinateCase < 9) {
            openCoordinateCase();
            advance(3);
        } else {
            openMap(2, 2, CENTER_X, CENTER_Z);
            advance(4);
        }
    }

    private void checkDisabledConfig() throws Exception {
        Field enabled = Class.forName("com.xyp.gtnotgood.config.Config").getField("enableJourneyMapContextMenu");
        boolean saved = enabled.getBoolean(null);
        try {
            closeMenu();
            enabled.setBoolean(null, false);
            nativeClick(screen, screen.width / 2, screen.height / 2, 1);
            require(!isOpen(), "disabling the config prevents the actual Fullscreen hook from opening the menu");
        } finally {
            enabled.setBoolean(null, saved);
        }
    }

    private void openMap(int scale, int zoom, int x, int z) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.guiScale = scale;
        Object manager = invoke(Class.forName("journeymap.client.ui.UIManager"), "getInstance", new Class<?>[0]);
        invoke(manager, "openFullscreenMap", new Class<?>[0]);
        require(mc.currentScreen != null && mc.currentScreen.getClass().getName().equals(FULLSCREEN),
            "JourneyMap's UI manager opens the real full-screen map");
        screen = mc.currentScreen;
        invoke(screen, "setZoom", new Class<?>[] { int.class }, zoom);
        Class<?> type = Class.forName(WAYPOINT + "$Type");
        Object waypoint = invoke(Class.forName(WAYPOINT), "at",
            new Class<?>[] { int.class, int.class, int.class, type, int.class }, x, FLOOR_Y + 1, z,
            type.getField("Normal").get(null), mc.thePlayer.dimension);
        invoke(screen, "centerOn", new Class<?>[] { Class.forName(WAYPOINT) }, waypoint);
    }

    private void checkCoordinates() throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        int x = screen.width / 2 - 3;
        int y = screen.height / 2 - 2;
        int[] raw = rawCoordinates(x, y);
        Object grid = read(screen, "gridRenderer");
        Object nativeCoordinate = invoke(grid, "getBlockUnderMouse",
            new Class<?>[] { double.class, double.class, int.class, int.class }, (double) raw[0], (double) raw[1],
            mc.displayWidth, mc.displayHeight);
        openAt(x, y);
        Object target = read(menu(), "target");
        require(
            number(target, "x") == number(nativeCoordinate, "x")
                && number(target, "z") == number(nativeCoordinate, "z"),
            "selected coordinates match native projection at GUI scale " + mc.gameSettings.guiScale
                + ", zoom "
                + coordinateCase % 3 * 2);
        require(number(target, "x") < 0 && number(target, "z") < 0,
            "negative map coordinates retain their sign and floor correctly");
        closeMenu();
    }

    private void checkWaypointSelection() throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        Object state = invoke(Class.forName(FULLSCREEN), "state", new Class<?>[0]);
        Object grid = read(screen, "gridRenderer");
        Object previousSteps = read(state, "drawWaypointStepList");
        Object previousMapType = read(grid, "mapType");
        int previousDimension = mc.thePlayer.dimension;
        try {
            for (int dim : new int[] { 0, -1 }) {
                mc.thePlayer.dimension = dim;
                write(grid, "mapType",
                    invoke(Class.forName("journeymap.client.model.MapType"), "day", new Class<?>[] { int.class }, dim));
                Class<?> waypointType = Class.forName(WAYPOINT + "$Type");
                Object waypoint = invoke(Class.forName(WAYPOINT), "at",
                    new Class<?>[] { int.class, int.class, int.class, waypointType, int.class }, -35, 55, -49,
                    waypointType.getField("Normal").get(null), dim);
                Object step = Class.forName("journeymap.client.render.draw.DrawWayPointStep")
                    .getConstructor(Class.forName(WAYPOINT)).newInstance(waypoint);
                int[] raw = rawCoordinates(screen.width / 2, screen.height / 2);
                write(step, "lastWindowPosition", new Point2D.Double(raw[0] + 2, mc.displayHeight - raw[1] + 2));
                List<Object> steps = new ArrayList<>();
                steps.add(step);
                write(state, "drawWaypointStepList", steps);
                openAt(screen.width / 2, screen.height / 2);
                Object target = read(menu(), "target");
                require(number(target, "x") == -35 && number(target, "y") == 55 && number(target, "z") == -49,
                    "a hovered native waypoint uses its full coordinates, including Nether scaling, in dimension "
                        + dim);
                closeMenu();
            }
        } finally {
            mc.thePlayer.dimension = previousDimension;
            write(grid, "mapType", previousMapType);
            write(state, "drawWaypointStepList", previousSteps);
        }
    }

    private void checkEdges() throws Exception {
        int[][] corners = { { 2, 2 }, { screen.width - 3, 2 }, { 2, screen.height - 3 },
            { screen.width - 3, screen.height - 3 } };
        for (int[] corner : corners) {
            int[] point = availablePoint(corner[0], corner[1]);
            openAt(point[0], point[1]);
            requireBounds();
            closeMenu();
        }
    }

    private int[] availablePoint(int x, int y) throws Exception {
        int directionX = x < screen.width / 2 ? 1 : -1;
        int directionY = y < screen.height / 2 ? 1 : -1;
        for (int offsetX = 0; offsetX <= 80; offsetX += 16) {
            for (int offsetY = 0; offsetY <= 80; offsetY += 16) {
                int candidateX = x + directionX * offsetX;
                int candidateY = y + directionY * offsetY;
                if (
                    !(Boolean) invoke(screen, "isMouseOverButton", new Class<?>[] { int.class, int.class }, candidateX,
                        candidateY)
                ) return new int[] { candidateX, candidateY };
            }
        }
        throw new AssertionError("No unblocked map area near corner " + x + "," + y);
    }

    private void openAt(int x, int y) throws Exception {
        if (menu() == null) {
            nativeClick(screen, screen.width / 2, screen.height / 2, 1);
            require(menu() != null, "the real mouse hook owns a menu instance");
        }
        closeMenu();
        int[] raw = rawCoordinates(x, y);
        boolean consumed = (Boolean) invoke(menu(), "mouseClicked",
            new Class<?>[] { int.class, int.class, int.class, int.class, int.class }, x, y, 1, raw[0], raw[1]);
        require(consumed && isOpen(), "the menu opens with explicit native display coordinates");
    }

    private int[] rawCoordinates(int x, int y) {
        Minecraft mc = Minecraft.getMinecraft();
        return new int[] { (int) ((x + 0.5) * mc.displayWidth / screen.width),
            mc.displayHeight - 1 - (int) ((y + 0.5) * mc.displayHeight / screen.height) };
    }

    private void requireBounds() throws Exception {
        int[] bounds = (int[]) read(menu(), "bounds");
        require(bounds[0] >= 0 && bounds[1] >= 0
            && bounds[0] + bounds[2] <= screen.width
            && bounds[1] + bounds[3] <= screen.height, "all menu edges remain inside the current GUI viewport");
    }

    private void select(int item) throws Exception {
        int[] bounds = (int[]) read(menu(), "bounds");
        nativeClick(screen, bounds[0] + 8, bounds[1] + 26 + item * 18 + 9, 0);
    }

    private boolean isOpen() throws Exception {
        Object current = menu();
        return current != null && (Boolean) read(current, "open");
    }

    private void closeMenu() throws Exception {
        Object current = menu();
        if (current != null) invoke(current, "close", new Class<?>[0]);
    }

    private Object menu() throws Exception {
        if (screen == null) return null;
        for (Class<?> type = screen.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!field.getType().getName().equals(MENU)) continue;
                field.setAccessible(true);
                return field.get(screen);
            }
        }
        throw new AssertionError("The actual JourneyMap Fullscreen class has no injected menu field");
    }

    private static void nativeClick(GuiScreen target, int x, int y, int button) throws Exception {
        method(target.getClass(), new String[] { "mouseClicked", "func_73864_a" }, int.class, int.class, int.class)
            .invoke(target, x, y, button);
    }

    private static void nativeKey(GuiScreen target, int key) throws Exception {
        method(target.getClass(), new String[] { "keyTyped", "func_73869_a" }, char.class, int.class).invoke(target,
            '\0', key);
    }

    private static GuiButton button(GuiScreen target, int id) throws Exception {
        for (Object candidate : (List<?>) read(target, "buttonList")) {
            if (candidate instanceof GuiButton && ((GuiButton) candidate).id == id) return (GuiButton) candidate;
        }
        throw new AssertionError("Missing confirmation button " + id);
    }

    private static int number(Object target, String name) throws Exception {
        return ((Number) read(target, name)).intValue();
    }

    private static Object read(Object target, String name) throws Exception {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(target.getClass().getName() + "." + name);
    }

    private static void write(Object target, String name, Object value) throws Exception {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(target.getClass().getName() + "." + name);
    }

    private static Object invoke(Object target, String name, Class<?>[] arguments, Object... values) throws Exception {
        Class<?> type = target instanceof Class<?> ? (Class<?>) target : target.getClass();
        return method(type, new String[] { name }, arguments).invoke(target instanceof Class<?> ? null : target,
            values);
    }

    private static Method method(Class<?> type, String[] names, Class<?>... arguments) throws NoSuchMethodException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (String name : names) {
                try {
                    Method method = current.getDeclaredMethod(name, arguments);
                    method.setAccessible(true);
                    return method;
                } catch (NoSuchMethodException ignored) {}
            }
        }
        throw new NoSuchMethodException(type.getName() + "." + names[0]);
    }

    private void advance(int next) {
        stage = next;
        frames = 0;
    }

    private static void screenshot(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(outputDirectory(), name, mc.displayWidth, mc.displayHeight,
            mc.getFramebuffer());
    }

    private static File outputDirectory() {
        return new File(System.getProperty("gtng.journeyMap.qa.output", "."));
    }

    private synchronized void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
        System.out.println("JOURNEYMAP_QA: " + message);
    }

    private void fail(Throwable error) {
        error.printStackTrace();
        finish("FAIL: stage " + stage + ": " + error);
    }

    private void finish(String result) {
        try {
            Files.write(new File(outputDirectory(), "result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
        } catch (Exception error) {
            error.printStackTrace();
        }
        System.out.println("JOURNEYMAP_QA: " + result + "; " + checks + " checks");
        finished = true;
    }
}
