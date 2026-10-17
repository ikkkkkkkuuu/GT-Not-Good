package com.xyp.gtnotgood.qa;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

import javax.imageio.ImageIO;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.sun.net.httpserver.HttpServer;
import com.xyp.gtnotgood.client.webscreen.ChromiumSession;
import com.xyp.gtnotgood.client.webscreen.ClientWebScreens;
import com.xyp.gtnotgood.client.webscreen.GuiWebScreen;
import com.xyp.gtnotgood.client.webscreen.GuiWebScreenSettings;
import com.xyp.gtnotgood.client.webscreen.WebScreenSettings;
import com.xyp.gtnotgood.client.webscreen.WebScreenTexture;
import com.xyp.gtnotgood.common.blocks.webscreen.TileWebScreen;
import com.xyp.gtnotgood.common.blocks.webscreen.WebScreenResolution;
import com.xyp.gtnotgood.common.blocks.webscreen.WebScreenShape;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.client.config.GuiSlider;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;

@Mod(
    modid = "webscreenqa",
    name = "Web Display QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class WebScreenClientChecks {

    private static final int[] FACINGS = { 2, 3, 4, 5 };
    private final Set<String> events = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, LayoutState> latestLayouts = new ConcurrentHashMap<>();
    private final AtomicLong layoutDocuments = new AtomicLong();
    private final Set<ResourceLocation> textureLocations = new HashSet<>();
    private HttpServer fixture;
    private String fixtureUrl;
    private volatile Throwable failure;
    private volatile boolean serverReady;
    private volatile boolean browserReady;
    private volatile boolean finished;
    private volatile ChromiumSession ownedBrowser;
    private volatile int wallChangeRequested;
    private volatile int wallChangeCompleted;
    private boolean started;
    private boolean guiCaptured;
    private boolean fixtureNavigationRequested;
    private int ticks;
    private int facingIndex;
    private int stage;
    private int frames;
    private int checks;
    private int guiProbeStep;
    private int guiProbeTicks;
    private TileWebScreen activeTile;
    private ChromiumSession wallSession;
    private EntityOtherPlayerMP camera;
    private long videoProbeStarted;
    private boolean videoProbeClicked;
    private long presentationWarmup;
    private long presentationStarted;
    private long presentationPreviousFrame;
    private long presentationMaximumGap;
    private int presentationSignature;
    private int presentationFrames;
    private int presentationRenderCalls;
    private final List<WebScreenResolution> presentationPresets = new ArrayList<>();
    private int presentationPresetIndex;
    private String presentationMarker = "presentation";
    private boolean presentationNavigationRequested;
    private TileWebScreen settingsPeerTile;
    private ChromiumSession settingsPeerSession;
    private ChromiumSession settingsPreviousSession;
    private WebScreenSettings settingsExpected;
    private WebScreenSettings peerExpected;
    private File settingsProfile;
    private String settingsUrl;
    private final boolean recipeOnly = Boolean.getBoolean("gtng.webscreen.qa.recipeOnly");
    private final boolean resolutionOnly = Boolean.getBoolean("gtng.webscreen.qa.resolutionOnly");
    private final boolean baselineOnly = Boolean.getBoolean("gtng.webscreen.qa.baselineOnly");

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.webscreen.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0F);
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.limitFramerate = 160;
        if (camera != null) mc.renderViewEntity = camera;
        if (finished) {
            mc.shutdown();
            return;
        }
        try {
            if (failure != null) throw new AssertionError("Web display checks failed", failure);
            if (++ticks > 12000) throw new AssertionError("Web display timed out at stage " + stage);
            if (!started && mc.theWorld == null && mc.currentScreen instanceof GuiMainMenu) {
                started = true;
                if (!recipeOnly) {
                    startFixture();
                    Thread worker = new Thread(this::checkBrowser, "Web display QA browser");
                    worker.setDaemon(true);
                    worker.start();
                }
                if (resolutionOnly || baselineOnly) return;
                mc.launchIntegratedServer("web-screen-qa-" + System.currentTimeMillis(), "Web Display QA",
                    new WorldSettings(96L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT)
                        .enableCommands());
            }
            if (resolutionOnly || baselineOnly) {
                if (
                    browserReady
                ) finish("PASS; " + checks
                    + " checks; "
                    + (baselineOnly ? "baseline 1440p and 2160p frame delivery"
                        : "1080p, 1440p and 2160p capture, zoom and input"));
                return;
            }
            if (!serverReady || mc.thePlayer == null || mc.theWorld == null) return;
            if (recipeOnly) {
                finish("PASS; " + checks + " checks; native workbench recipe and LV circuit alternatives");
                return;
            }
            if (stage == 0) {
                for (int index = 0; index < FACINGS.length; index++) {
                    if (
                        !(mc.theWorld.getTileEntity(x(index), 70, 0) instanceof TileWebScreen tile)
                            || tile.getScreenId() == null
                    ) return;
                    require(tile.getFacing() == FACINGS[index], "client receives facing " + FACINGS[index]);
                }
                openNext();
            } else if (stage == 1) {
                ChromiumSession session = ClientWebScreens.getSession(activeTile);
                if (session == null) throw new AssertionError("Display loses its browser session while loading");
                String status = session.getStatus();
                if ("failed".equals(status) || "missingBrowser".equals(status)) {
                    throw new AssertionError("Display browser status: " + status);
                }
                if (!fixtureNavigationRequested && "ready".equals(status)) {
                    session.navigate(fixtureUrl + (facingIndex == 0 ? "?gui" : ""));
                    fixtureNavigationRequested = true;
                    frames = 0;
                }
                if (fixtureNavigationRequested && ++frames >= 40 && ClientWebScreens.getTexture(activeTile) != null) {
                    require(
                        mc.currentScreen != null && mc.currentScreen.getClass().getSimpleName().equals("GuiWebScreen"),
                        "right-click opens the browser GUI");
                    textureLocations.add(ClientWebScreens.getTexture(activeTile));
                    require(textureLocations.size() == 1, "sequential display sessions reuse the same texture slot");
                    verifyTexture(activeTile);
                    if (facingIndex == 0) {
                        verifyRenderPresentation(activeTile);
                        verifyLargeTextureUploads();
                    }
                    advance(facingIndex == 0 ? 5 : 2);
                }
            } else if (stage == 4 && browserReady) {
                finish("PASS; " + checks + " checks; browser, GUI, four orientations and expandable screen wall");
            } else if (stage == 5) {
                checkGuiInput();
            } else if (stage >= 6) {
                checkWallClient();
            }
        } catch (Throwable error) {
            fail(error);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || failure != null) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            World world = player.worldObj;
            if (serverReady) {
                changeWall(world);
                return;
            }
            verifyCrafting(world);
            if (recipeOnly) {
                serverReady = true;
                return;
            }
            world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
            world.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
            world.setWorldTime(6000);
            world.getWorldInfo().setRaining(false);
            for (int x = -5; x <= 15; x++) {
                for (int z = -5; z <= 5; z++) {
                    world.setBlock(x, 68, z, Blocks.stone, 0, 2);
                    for (int y = 69; y <= 76; y++) world.setBlockToAir(x, y, z);
                }
            }
            ItemStack stack = GTNGItemList.WebScreen.get(1);
            Block block = Block.getBlockFromItem(stack.getItem());
            require(stack.getItem() instanceof ItemBlock, "display is registered as an ItemBlock");
            Set<UUID> identities = new HashSet<>();
            for (int index = 0; index < FACINGS.length; index++) {
                player.rotationYaw = switch (FACINGS[index]) {
                    case 2 -> 0;
                    case 3 -> 180;
                    case 4 -> 270;
                    default -> 90;
                };
                require(((ItemBlock) stack.getItem()).placeBlockAt(stack.copy(), player, world, x(index), 70, 0, 1,
                    0.5F, 0.5F, 0.5F, FACINGS[index]), "display places with facing " + FACINGS[index]);
                TileWebScreen tile = (TileWebScreen) world.getTileEntity(x(index), 70, 0);
                require(tile.getFacing() == FACINGS[index] && tile.getScreenId() != null,
                    "placed display has its facing and server UUID");
                require(identities.add(tile.getScreenId()), "each placed display receives a unique profile identity");
                NBTTagCompound saved = new NBTTagCompound();
                tile.writeToNBT(saved);
                TileWebScreen restored = new TileWebScreen();
                restored.readFromNBT(saved);
                require(tile.getScreenId().equals(restored.getScreenId()), "NBT reload preserves profile identity");
                require(!saved.hasKey("url") && !saved.hasKey("cookies") && !tile.canUpdate(),
                    "world NBT stores no browser URL or cookies and display has no server tick work");
                require(block.getDrops(world, x(index), 70, 0, FACINGS[index], 0).size() == 1,
                    "display drops a single item");
                world.markBlockForUpdate(x(index), 70, 0);
            }
            for (int x = 12; x <= 13; x++) {
                for (int y = 70; y <= 71; y++) {
                    world.setBlock(x, y, 0, block, 3, 3);
                    require(world.getTileEntity(x, y, 0) instanceof TileWebScreen tile && tile.getFacing() == 3,
                        "2x2 wall member places with a shared facing");
                }
            }
            player.capabilities.allowFlying = true;
            player.capabilities.isFlying = true;
            player.sendPlayerAbilities();
            player.setPositionAndUpdate(0.5, 70, -3);
            serverReady = true;
        } catch (Throwable error) {
            failure = error;
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        if (
            stage != 2 && stage != 3
                && stage != 8
                && stage != 9
                && stage != 11
                && stage != 13
                && stage != 15
                && stage != 17
                && stage != 18
        ) return;
        try {
            if (stage == 18) {
                measurePresentation();
                return;
            }
            if (++frames < 60) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (stage >= 8) {
                renderWall();
                return;
            }
            if (stage == 2) {
                if (!guiCaptured) {
                    screenshot("web-screen-gui.png");
                    guiCaptured = true;
                }
                mc.displayGuiScreen(null);
                mc.gameSettings.hideGUI = true;
                int facing = FACINGS[facingIndex];
                double cameraX = x(facingIndex) + 0.5 + (facing == 4 ? -3 : facing == 5 ? 3 : 0);
                double cameraZ = 0.5 + (facing == 2 ? -3 : facing == 3 ? 3 : 0);
                float yaw = facing == 2 ? 0 : facing == 3 ? 180 : facing == 4 ? -90 : 90;
                cameraAt(cameraX, 70.5, cameraZ, yaw);
                advance(3);
            } else {
                require(ClientWebScreens.getTexture(activeTile) != null,
                    "dynamic browser texture remains available on facing " + FACINGS[facingIndex]);
                screenshot("web-screen-facing-" + FACINGS[facingIndex] + ".png");
                ChromiumSession session = ClientWebScreens.getSession(activeTile);
                ClientWebScreens.stop(activeTile);
                require(ClientWebScreens.getSession(activeTile) == null, "stop releases the display browser session");
                closeAndAwait(session);
                facingIndex++;
                if (facingIndex < FACINGS.length) openNext();
                else advance(6);
            }
        } catch (Throwable error) {
            fail(error);
        }
    }

    private void verifyCrafting(World world) {
        InventoryCrafting grid = new InventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        }, 3, 3);
        ItemStack steel = GTOreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 1);
        require(steel != null, "steel plate ingredient is registered");
        for (int slot : new int[] { 0, 2, 6, 8 }) grid.setInventorySlotContents(slot, steel.copy());
        grid.setInventorySlotContents(1, new ItemStack(Items.glowstone_dust));
        grid.setInventorySlotContents(4, new ItemStack(Blocks.glass_pane));
        ItemStack cable = GTOreDictUnificator.get(OrePrefixes.cableGt01, Materials.Copper, 1);
        require(cable != null, "single copper cable ingredient is registered");
        grid.setInventorySlotContents(3, cable.copy());
        grid.setInventorySlotContents(5, cable.copy());
        ItemStack output = GTNGItemList.WebScreen.get(1);
        for (ItemStack circuit : new ItemStack[] { ItemList.Circuit_Basic.get(1),
            ItemList.Circuit_BasicElectronic.get(1) }) {
            grid.setInventorySlotContents(7, circuit);
            ItemStack result = CraftingManager.getInstance().findMatchingRecipe(grid, world);
            require(result != null && result.isItemEqual(output) && result.stackSize == 1,
                "native workbench crafts one display with LV circuit " + circuit.getDisplayName());
        }
        grid.setInventorySlotContents(7, ItemList.Circuit_Integrated.get(1));
        ItemStack selector = CraftingManager.getInstance().findMatchingRecipe(grid, world);
        require(selector == null || !selector.isItemEqual(output), "a programmed circuit cannot replace an LV circuit");
        grid.setInventorySlotContents(7, null);
        ItemStack missingCircuit = CraftingManager.getInstance().findMatchingRecipe(grid, world);
        require(missingCircuit == null || !missingCircuit.isItemEqual(output),
            "display cannot be crafted without its LV circuit");
        grid.setInventorySlotContents(7, ItemList.Circuit_Basic.get(1));
        grid.setInventorySlotContents(0, new ItemStack(Items.iron_ingot));
        ItemStack wrongPlate = CraftingManager.getInstance().findMatchingRecipe(grid, world);
        require(wrongPlate == null || !wrongPlate.isItemEqual(output),
            "iron ingots cannot replace steel plates in the display recipe");
    }

    private void checkWallClient() {
        Minecraft mc = Minecraft.getMinecraft();
        switch (stage) {
            case 6 -> {
                if (!wallReady(2, 2, 70)) return;
                activeTile = wallTile(13, 70);
                WebScreenShape shape = ClientWebScreens.getShape(activeTile);
                require(shape.anchorX == 12 && shape.anchorY == 70 && shape.anchorZ == 0 && shape.facing == 3,
                    "2x2 wall resolves the lower-left anchor from a different member");
                mc.gameSettings.hideGUI = false;
                mc.theWorld.getBlock(13, 70, 0).onBlockActivated(mc.theWorld, 13, 70, 0, mc.thePlayer, 3, 0.5F, 0.5F,
                    0.5F);
                applyStartupPreset(activeTile);
                wallSession = ClientWebScreens.getSession(activeTile);
                require(wallSession != null, "right-clicking a nonanchor wall member starts its shared browser");
                fixtureNavigationRequested = false;
                advance(7);
            }
            case 7 -> {
                String status = wallSession.getStatus();
                if ("failed".equals(status) || "missingBrowser".equals(status)) {
                    throw new AssertionError("Wall browser status: " + status);
                }
                if (!fixtureNavigationRequested && "ready".equals(status)) {
                    wallSession.navigate(fixtureUrl);
                    fixtureNavigationRequested = true;
                    frames = 0;
                }
                if (!fixtureNavigationRequested || ++frames < 40 || ClientWebScreens.getTexture(activeTile) == null) {
                    return;
                }
                requireGui(mc.currentScreen);
                assertWallSession(2, 2, 70);
                verifyTexture(activeTile);
                textureLocations.add(ClientWebScreens.getTexture(activeTile));
                require(textureLocations.size() == 1, "the large wall reuses the released display texture slot");
                advance(8);
            }
            case 10 -> {
                if (wallChangeCompleted < 1 || !wallReady(2, 3, 70)) return;
                require("ready".equals(wallSession.getStatus()), "expanding the wall keeps its browser running");
                assertWallSession(2, 3, 70);
                cameraAt(13, 71.5, 5, 180);
                advance(11);
            }
            case 12 -> {
                if (
                    wallChangeCompleted < 2 || !mc.theWorld.isAirBlock(12, 71, 0)
                        || !mc.theWorld.isAirBlock(13, 71, 0)
                        || !wallReady(2, 1, 70)
                        || !wallReady(2, 1, 72)
                ) return;
                assertWallSession(2, 1, 70);
                require(
                    ClientWebScreens.getSession(wallTile(12, 72)) == null
                        && ClientWebScreens.getTexture(wallTile(13, 72)) == null,
                    "removing the middle row separates the upper rectangle from the running browser");
                cameraAt(13, 71.5, 5, 180);
                advance(13);
            }
            case 14 -> {
                if (wallChangeCompleted < 3 || !wallReady(4, 2, 70)) return;
                assertWallSession(4, 2, 70);
                require("ready".equals(wallSession.getStatus()), "horizontal expansion preserves the running browser");
                cameraAt(14, 71, 5, 180);
                advance(15);
            }
            case 16 -> {
                long elapsed = System.currentTimeMillis() - videoProbeStarted;
                if (elapsed >= 12000 && !videoProbeClicked) {
                    click(wallSession, 46, 520);
                    videoProbeClicked = true;
                }
                if (elapsed < 20000) return;
                mc.displayGuiScreen(null);
                mc.gameSettings.hideGUI = true;
                cameraAt(14, 71, 5, 180);
                advance(17);
            }
            default -> {}
        }
    }

    private void renderWall() throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        switch (stage) {
            case 8 -> {
                screenshot("web-screen-wall-gui.png");
                mc.displayGuiScreen(null);
                mc.gameSettings.hideGUI = true;
                cameraAt(13, 71, 5, 180);
                advance(9);
            }
            case 9 -> {
                screenshot("web-screen-wall-2x2.png");
                wallChangeRequested = 1;
                advance(10);
            }
            case 11 -> {
                screenshot("web-screen-wall-2x3.png");
                wallChangeRequested = 2;
                advance(12);
            }
            case 13 -> {
                screenshot("web-screen-wall-split.png");
                wallChangeRequested = 3;
                advance(14);
            }
            case 15 -> {
                screenshot("web-screen-wall-4x2.png");
                if (Boolean.getBoolean("gtng.webscreen.qa.bilibili")) {
                    mc.gameSettings.hideGUI = false;
                    ClientWebScreens.open(activeTile);
                    wallSession.navigate("https://www.bilibili.com/video/BV1Mk4y197Vu/");
                    videoProbeStarted = System.currentTimeMillis();
                    advance(16);
                } else startPresentation();
            }
            case 17 -> {
                screenshot("web-screen-wall-4x2-bilibili.png");
                String probe = "UNVERIFIED_PUBLIC_VIDEO_FRAME; url=" + wallSession.getUrl()
                    + "; status="
                    + wallSession.getStatus()
                    + "; muted; inspect screenshot for playback or site errors";
                Files.write(new File(output(), "bilibili-video.txt").toPath(), probe.getBytes(StandardCharsets.UTF_8));
                System.out.println("WEB_SCREEN_QA: " + probe);
                startPresentation();
            }
            default -> throw new AssertionError("Unknown wall render stage " + stage);
        }
    }

    private void changeWall(World world) {
        if (wallChangeRequested <= wallChangeCompleted) return;
        if (wallChangeRequested == 1) {
            Block block = world.getBlock(12, 70, 0);
            world.setBlock(12, 72, 0, block, 3, 3);
            world.setBlock(13, 72, 0, block, 3, 3);
        } else if (wallChangeRequested == 2) {
            world.setBlockToAir(12, 71, 0);
            world.setBlockToAir(13, 71, 0);
        } else {
            Block block = world.getBlock(12, 70, 0);
            world.setBlockToAir(12, 72, 0);
            world.setBlockToAir(13, 72, 0);
            for (int x = 12; x <= 15; x++) {
                for (int y = 70; y <= 71; y++) world.setBlock(x, y, 0, block, 3, 3);
            }
        }
        wallChangeCompleted = wallChangeRequested;
    }

    private void finishWall() throws InterruptedException {
        ClientWebScreens.stop(activeTile);
        require(ClientWebScreens.getSession(wallTile(12, 70)) == null,
            "stopping a nonanchor member stops the whole connected screen");
        closeAndAwait(wallSession);
        advance(4);
    }

    private void startPresentation() throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        mc.displayGuiScreen(null);
        mc.gameSettings.hideGUI = true;
        for (String option : System.getProperty("gtng.webscreen.qa.benchmarkPresets", "").split(",")) {
            if (option.trim().isEmpty()) continue;
            WebScreenResolution resolution = WebScreenResolution.parse(option);
            if (!presentationPresets.contains(resolution)) presentationPresets.add(resolution);
        }
        if (!presentationPresets.isEmpty()) startPresentationPreset();
        else {
            wallSession.navigate(fixtureUrl + "resolution?" + presentationMarker);
            presentationNavigationRequested = true;
            advance(18);
        }
    }

    private void startPresentationPreset() throws Exception {
        WebScreenResolution resolution = presentationPresets.get(presentationPresetIndex);
        Field profile = ChromiumSession.class.getDeclaredField("profile");
        profile.setAccessible(true);
        File previousProfile = (File) profile.get(wallSession);
        ChromiumSession previousSession = wallSession;
        WebScreenSettings draft = ClientWebScreens.getSettings(activeTile);
        draft.setResolution(resolution);
        ClientWebScreens.applySettings(activeTile, draft);
        wallSession = ClientWebScreens.getSession(activeTile);
        if (previousSession != wallSession) closeAndAwait(previousSession);
        require(
            wallSession != null && wallSession.getWidth() == resolution.width
                && wallSession.getHeight() == resolution.height,
            "wall benchmark opens its " + resolution.option + " browser");
        require(previousProfile.equals(profile.get(wallSession)),
            "wall benchmark retains the same display profile across resolutions");
        Minecraft mc = Minecraft.getMinecraft();
        mc.displayGuiScreen(null);
        mc.gameSettings.hideGUI = true;
        presentationMarker = "presentation-" + resolution.option + "-" + System.nanoTime();
        presentationNavigationRequested = false;
        presentationWarmup = 0;
        presentationStarted = 0;
        presentationPreviousFrame = 0;
        presentationMaximumGap = 0;
        presentationSignature = 0;
        presentationFrames = 0;
        presentationRenderCalls = 0;
        advance(18);
    }

    private void measurePresentation() throws Exception {
        String status = wallSession.getStatus();
        if ("failed".equals(status) || "missingBrowser".equals(status)) {
            throw new AssertionError("Wall benchmark browser status: " + status);
        }
        if (!presentationNavigationRequested) {
            if (!"ready".equals(status)) return;
            wallSession.resetZoom();
            wallSession.navigate(fixtureUrl + "resolution?" + presentationMarker);
            presentationNavigationRequested = true;
        }
        ResourceLocation location = ClientWebScreens.getTexture(activeTile);
        if (location == null) return;
        WebScreenTexture texture = (WebScreenTexture) Minecraft.getMinecraft().getTextureManager().getTexture(location);
        int[] pixels = texture.getTextureData();
        int width = wallSession.getWidth();
        int height = wallSession.getHeight();
        if (pixels.length != width * height) return;
        long now = System.nanoTime();
        if (presentationStarted == 0) {
            if (!hasViewportEvent(presentationMarker, width / 960.0)) return;
            int corner = pixels[16 * width + 16];
            if (Math.abs((corner >> 16 & 255) - 215) > 20 || Math.abs((corner >> 8 & 255) - 74) > 20) return;
            if (presentationWarmup == 0) presentationWarmup = now;
            if (now - presentationWarmup < TimeUnit.SECONDS.toNanos(2)) return;
            presentationStarted = now;
        }
        presentationRenderCalls++;
        int signature = 1;
        for (int y = 100; y < 180; y += 2) {
            for (int x = 780; x < 860; x += 2) {
                signature = 31 * signature + pixels[(y * height / 540) * width + x * width / 960];
            }
        }
        if (presentationFrames == 0 || signature != presentationSignature) {
            if (presentationPreviousFrame != 0) {
                presentationMaximumGap = Math.max(presentationMaximumGap, now - presentationPreviousFrame);
            }
            presentationSignature = signature;
            presentationPreviousFrame = now;
            presentationFrames++;
        }
        long elapsed = now - presentationStarted;
        if (elapsed < TimeUnit.SECONDS.toNanos(15)) return;
        presentationMaximumGap = Math.max(presentationMaximumGap, now - presentationPreviousFrame);
        require(presentationFrames > 1,
            "the wall presents changing browser content throughout the live render benchmark");
        String result = "pixels=" + width
            + "x"
            + height
            + "; captureTarget="
            + wallSession.getFrameRate()
            + "; observedMillis="
            + TimeUnit.NANOSECONDS.toMillis(elapsed)
            + "; renderCalls="
            + presentationRenderCalls
            + "; observedGameRenderFps="
            + presentationRenderCalls * (double) TimeUnit.SECONDS.toNanos(1) / elapsed
            + "; presentedContentChanges="
            + presentationFrames
            + "; observedTextureContentFps="
            + presentationFrames * (double) TimeUnit.SECONDS.toNanos(1) / elapsed
            + "; maximumContentGapMillis="
            + presentationMaximumGap / 1000000.0
            + "; texture changes sampled at RenderTick END on the 4x2 wall";
        String suffix = presentationPresets.isEmpty() ? ""
            : "-" + presentationPresets.get(presentationPresetIndex).option;
        Files.write(new File(output(), "presentation-results" + suffix + ".txt").toPath(),
            result.getBytes(StandardCharsets.UTF_8));
        System.out.println("WEB_SCREEN_QA: " + result);
        verifyTexture(activeTile);
        screenshot("web-screen-wall-4x2-resolution" + suffix + ".png");
        if (!presentationPresets.isEmpty() && ++presentationPresetIndex < presentationPresets.size()) {
            startPresentationPreset();
            return;
        }
        finishWall();
    }

    private static TileWebScreen wallTile(int x, int y) {
        return Minecraft.getMinecraft().theWorld.getTileEntity(x, y, 0) instanceof TileWebScreen tile ? tile : null;
    }

    private static boolean wallReady(int width, int height, int bottomY) {
        for (int x = 12; x < 12 + width; x++) {
            for (int y = bottomY; y < bottomY + height; y++) {
                TileWebScreen tile = wallTile(x, y);
                if (tile == null || tile.getScreenId() == null) return false;
                WebScreenShape shape = ClientWebScreens.getShape(tile);
                if (
                    shape.width != width || shape.height != height
                        || shape.anchorX != 12
                        || shape.anchorY != bottomY
                        || shape.anchorZ != 0
                ) return false;
            }
        }
        return true;
    }

    private void assertWallSession(int width, int height, int bottomY) {
        ResourceLocation texture = ClientWebScreens.getTexture(activeTile);
        for (int x = 12; x < 12 + width; x++) {
            for (int y = bottomY; y < bottomY + height; y++) {
                TileWebScreen tile = wallTile(x, y);
                require(
                    ClientWebScreens.getSession(tile) == wallSession && texture != null
                        && texture.equals(ClientWebScreens.getTexture(tile)),
                    "wall member " + x + "," + y + " shares the same browser and dynamic texture");
            }
        }
    }

    private void cameraAt(double x, double eyeY, double z, float yaw) {
        Minecraft mc = Minecraft.getMinecraft();
        if (camera == null)
            camera = new EntityOtherPlayerMP(mc.theWorld, new GameProfile(UUID.randomUUID(), "WebScreenQA"));
        camera.yOffset = 0;
        double y = eyeY - camera.getEyeHeight();
        camera.setPositionAndRotation(x, y, z, yaw, 0);
        camera.prevPosX = camera.lastTickPosX = x;
        camera.prevPosY = camera.lastTickPosY = y;
        camera.prevPosZ = camera.lastTickPosZ = z;
        camera.prevRotationYaw = camera.rotationYawHead = camera.prevRotationYawHead = yaw;
        camera.prevRotationPitch = 0;
        mc.gameSettings.thirdPersonView = 0;
        mc.gameSettings.viewBobbing = false;
        mc.renderViewEntity = camera;
    }

    private void checkGuiInput() throws Exception {
        if (++guiProbeTicks > 1800) throw new AssertionError("GUI input timed out at step " + guiProbeStep);
        GuiScreen gui = Minecraft.getMinecraft().currentScreen;
        if (guiProbeStep >= 10) {
            checkSettingsGui(gui);
            return;
        }
        requireGui(gui);
        switch (guiProbeStep) {
            case 0 -> {
                guiClick(gui, 100, 44);
                for (char character : "GUI-testX".toCharArray()) {
                    int key = character == '-' ? Keyboard.KEY_MINUS
                        : Keyboard.getKeyIndex(String.valueOf(Character.toUpperCase(character)));
                    guiKey(gui, character, key);
                }
                guiProbeStep++;
            }
            case 1 -> {
                if (!events.contains("gui/input:GUI-testX")) return;
                require(true, "GUI mouse mapping focuses the page and forwards typed characters");
                guiKey(gui, '\0', Keyboard.KEY_F6);
                guiClick(gui, 320, 44);
                guiKey(gui, '\b', Keyboard.KEY_BACK);
                guiProbeStep++;
            }
            case 2 -> {
                if (!events.contains("gui/input:GUI-test") || !events.contains("gui/keyup:X")) return;
                require(true, "GUI restores page focus and forwards Backspace");
                require(true, "switching focus to the address bar releases pressed webpage keys");
                guiKey(gui, '\r', Keyboard.KEY_RETURN);
                guiProbeStep++;
            }
            case 3 -> {
                if (!events.contains("gui/submit:GUI-test")) return;
                require(true, "GUI Enter submits the native webpage form");
                guiClick(gui, 100, 105);
                guiProbeStep++;
            }
            case 4 -> {
                if (!events.contains("gui/click:clicked")) return;
                require(true, "GUI mouse press and release activate the webpage button");
                guiClick(gui, 20, 20);
                guiClick(gui, 940, 20);
                guiClick(gui, 20, 520);
                guiClick(gui, 940, 520);
                guiProbeStep++;
            }
            case 5 -> {
                if (
                    !events.contains("gui/corner:tl") || !events.contains("gui/corner:tr")
                        || !events.contains("gui/corner:bl")
                        || !events.contains("gui/corner:br")
                ) return;
                require(true, "GUI maps all four webpage corners independently of its physical raster size");
                ClientWebScreens.getSession(activeTile).navigate(fixtureUrl + "zoom?gui-zoom");
                guiProbeStep++;
            }
            case 6 -> {
                if (!hasZoomEvent("gui-zoom", 960, 540)) return;
                guiControlClick(gui, "zoomOut");
                require(ClientWebScreens.getSession(activeTile).getZoomPercent() == 75,
                    "GUI minus button shrinks webpage content to 75%");
                guiProbeStep++;
            }
            case 7 -> {
                ChromiumSession session = ClientWebScreens.getSession(activeTile);
                if (
                    !hasZoomEvent("gui-zoom", 1280, 720) || session.getViewportWidth() != 1280
                        || session.getViewportHeight() != 720
                        || !textureHasZoom(activeTile, 1280, 720)
                ) return;
                guiClick(gui, 20, 20);
                guiClick(gui, 1260, 20);
                guiClick(gui, 20, 700);
                guiClick(gui, 1260, 700);
                guiProbeStep++;
            }
            case 8 -> {
                for (int[] corner : new int[][] { { 20, 20 }, { 1260, 20 }, { 20, 700 }, { 1260, 700 } }) {
                    if (!hasZoomClickNear("gui-zoom", 1280, 720, corner[0], corner[1])) return;
                }
                require(true, "GUI maps all webpage corners after a layout-changing zoom");
                ChromiumSession session = ClientWebScreens.getSession(activeTile);
                guiControlClick(gui, "zoomIn");
                require(session.getZoomPercent() == 100,
                    "GUI plus button increases webpage size by 25 percentage points");
                guiControlClick(gui, "zoomIn");
                guiControlClick(gui, "zoomReset");
                require(session.getZoomPercent() == 100, "GUI percentage button resets webpage zoom");
                require(guiZoomKey(gui, Keyboard.KEY_EQUALS, true) && session.getZoomPercent() == 125,
                    "GUI Ctrl+plus enlarges webpage content");
                require(guiZoomKey(gui, Keyboard.KEY_MINUS, true) && session.getZoomPercent() == 100,
                    "GUI Ctrl+minus shrinks webpage content");
                session.setZoomPercent(175);
                require(guiZoomKey(gui, Keyboard.KEY_0, true) && session.getZoomPercent() == 100,
                    "GUI Ctrl+0 resets webpage content size");
                require(!guiZoomKey(gui, Keyboard.KEY_EQUALS, false) && session.getZoomPercent() == 100,
                    "plain plus remains webpage input without changing zoom");
                guiWheel(gui, 120, true);
                require(session.getZoomPercent() == 125, "GUI Ctrl+wheel up enlarges the webpage");
                guiWheel(gui, -120, true);
                require(session.getZoomPercent() == 100, "GUI Ctrl+wheel down shrinks the webpage");
                guiKey(gui, '\0', Keyboard.KEY_F6);
                require(!guiZoomKey(gui, Keyboard.KEY_EQUALS, true),
                    "address editing keeps zoom shortcuts out of webpage navigation");
                guiWheel(gui, 120, true);
                require(session.getZoomPercent() == 100, "address editing suppresses Ctrl+wheel zoom");
                guiControlClick(gui, "zoomIn");
                guiKey(gui, '\0', Keyboard.KEY_ESCAPE);
                require(
                    Minecraft.getMinecraft().currentScreen == null
                        && ClientWebScreens.getSession(activeTile) == session,
                    "Esc leaves the zoomed webpage running on the world display");
                ClientWebScreens.open(activeTile);
                GuiScreen reopened = Minecraft.getMinecraft().currentScreen;
                requireGui(reopened);
                require(ClientWebScreens.getSession(activeTile) == session && session.getZoomPercent() == 125,
                    "reopening the display preserves its current webpage zoom and session");
                GuiButton reset = guiButton(reopened, "zoomReset");
                require("125%".equals(reset.displayString), "reopened GUI shows the current zoom percentage");
                guiProbeStep++;
            }
            case 9 -> {
                if (!hasZoomEvent("gui-zoom", 768, 432)) return;
                ChromiumSession session = ClientWebScreens.getSession(activeTile);
                WebScreenTexture texture = (WebScreenTexture) Minecraft.getMinecraft().getTextureManager()
                    .getTexture(ClientWebScreens.getTexture(activeTile));
                int[] pixels = texture.getTextureData();
                if (pixels.length != session.getWidth() * session.getHeight()) return;
                BufferedImage visible = new BufferedImage(session.getWidth(), session.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
                visible.setRGB(0, 0, visible.getWidth(), visible.getHeight(), pixels, 0, visible.getWidth());
                if (Math.abs(sampleWidth(visible, 768, 432) - session.getWidth() * 100.0 / 768) >= 4) return;
                screenshot("web-screen-gui-zoom-125.png");
                guiProbeStep++;
            }
            default -> throw new AssertionError("Unknown GUI probe step " + guiProbeStep);
        }
    }

    private static void requireGui(GuiScreen gui) {
        if (!(gui instanceof GuiWebScreen)) throw new AssertionError("Browser GUI closed during input checks");
    }

    private void guiClick(GuiScreen gui, int browserX, int browserY) throws Exception {
        ChromiumSession browser = ClientWebScreens.getSession(activeTile);
        int x = guiField(gui, "viewX") + browserX * guiField(gui, "viewWidth") / browser.getViewportWidth();
        int y = guiField(gui, "viewY") + browserY * guiField(gui, "viewHeight") / browser.getViewportHeight();
        guiMouseClick(gui, x, y);
    }

    private static void guiMouseClick(GuiScreen gui, int x, int y) throws Exception {
        Method pressed = GuiScreen.class.getDeclaredMethod("mouseClicked", int.class, int.class, int.class);
        Method released = GuiScreen.class.getDeclaredMethod("mouseMovedOrUp", int.class, int.class, int.class);
        pressed.setAccessible(true);
        released.setAccessible(true);
        pressed.invoke(gui, x, y, 0);
        released.invoke(gui, x, y, 0);
    }

    private static GuiButton guiButton(GuiScreen gui, String name) throws Exception {
        Field field = GuiWebScreen.class.getDeclaredField(name);
        field.setAccessible(true);
        return (GuiButton) field.get(gui);
    }

    private static void guiControlClick(GuiScreen gui, String name) throws Exception {
        GuiButton button = guiButton(gui, name);
        guiMouseClick(gui, button.xPosition + button.width / 2, button.yPosition + button.height / 2);
    }

    private static boolean guiZoomKey(GuiScreen gui, int key, boolean control) throws Exception {
        Method method = GuiWebScreen.class.getDeclaredMethod("handleZoomKey", int.class, boolean.class);
        method.setAccessible(true);
        return (boolean) method.invoke(gui, key, control);
    }

    private static void guiWheel(GuiScreen gui, int wheel, boolean control) throws Exception {
        Method method = GuiWebScreen.class.getDeclaredMethod("handleWheel", int.class, boolean.class, int.class,
            int.class);
        method.setAccessible(true);
        method.invoke(gui, wheel, control, guiField(gui, "viewX") + guiField(gui, "viewWidth") / 2,
            guiField(gui, "viewY") + guiField(gui, "viewHeight") / 2);
    }

    private static void guiKey(GuiScreen gui, char character, int key) throws Exception {
        Method typed = GuiScreen.class.getDeclaredMethod("keyTyped", char.class, int.class);
        typed.setAccessible(true);
        typed.invoke(gui, character, key);
    }

    private void checkSettingsGui(GuiScreen gui) throws Exception {
        switch (guiProbeStep) {
            case 10 -> {
                requireGui(gui);
                ChromiumSession original = ClientWebScreens.getSession(activeTile);
                settingsProfile = browserProfile(original);
                settingsUrl = original.getUrl();
                settingsPeerTile = (TileWebScreen) Minecraft.getMinecraft().theWorld.getTileEntity(x(1), 70, 0);
                ClientWebScreens.open(settingsPeerTile);
                ClientWebScreens.getSession(settingsPeerTile).setZoomPercent(75);
                peerExpected = ClientWebScreens.getSettings(settingsPeerTile);
                peerExpected.setResolution(WebScreenResolution.FullHd);
                peerExpected.setFrameRate(85);
                peerExpected.setJpegQuality(88);
                peerExpected.setMuted(false);
                peerExpected.setZoomPercent(75);
                ClientWebScreens.applySettings(settingsPeerTile, peerExpected);
                settingsPeerSession = ClientWebScreens.getSession(settingsPeerTile);
                settingsPeerSession.navigate(fixtureUrl + "zoom?peer-settings");
                ClientWebScreens.open(activeTile);
                gui = Minecraft.getMinecraft().currentScreen;
                WebScreenSettings unchanged = ClientWebScreens.getSettings(activeTile);
                WebScreenSettings detached = ClientWebScreens.getSettings(activeTile);
                detached.setFrameRate(17);
                requireSettings(unchanged, ClientWebScreens.getSettings(activeTile),
                    "settings accessor returns an independent draft");
                guiButtonClick(gui, 9);
                GuiScreen panel = Minecraft.getMinecraft().currentScreen;
                require(panel instanceof GuiWebScreenSettings, "browser toolbar opens the per-screen settings panel");
                guiButtonClick(panel, 0);
                guiSlider(panel, 1, 35);
                guiSlider(panel, 2, 70);
                guiButtonClick(panel, 3);
                require(ClientWebScreens.getSession(activeTile) == original,
                    "editing settings leaves its browser running");
                requireSettings(unchanged, ClientWebScreens.getSettings(activeTile),
                    "unapplied GUI changes do not mutate live settings");
                guiButtonClick(panel, 5);
                requireGui(Minecraft.getMinecraft().currentScreen);
                requireSettings(unchanged, ClientWebScreens.getSettings(activeTile),
                    "cancel discards every changed setting");
                guiButtonClick(Minecraft.getMinecraft().currentScreen, 9);
                panel = Minecraft.getMinecraft().currentScreen;
                WebScreenSettings draft = guiSettingsDraft(panel);
                for (int index = 0; index < 3 && draft.getResolution() != WebScreenResolution.QuadHd; index++) {
                    guiButtonClick(panel, 0);
                }
                require(draft.getResolution() == WebScreenResolution.QuadHd,
                    "resolution button selects the native 2K preset");
                require(guiSlider(panel, 1, 60) == 60,
                    "refresh slider selects 60 FPS through its actual mouse control");
                require(guiSlider(panel, 2, 75) == 75, "quality slider selects 75 through its actual mouse control");
                if (!draft.isMuted()) guiButtonClick(panel, 3);
                require(draft.isMuted(), "mute button changes only the current screen draft");
                settingsExpected = draft.copy();
                guiProbeStep++;
            }
            case 11 -> {
                require(gui instanceof GuiWebScreenSettings, "settings panel stays open until applied");
                screenshot("web-screen-settings.png");
                settingsPreviousSession = ClientWebScreens.getSession(activeTile);
                settingsPreviousSession.setZoomPercent(settingsPreviousSession.getZoomPercent());
                guiButtonClick(gui, 4);
                requireGui(Minecraft.getMinecraft().currentScreen);
                require(ClientWebScreens.getSession(activeTile) != settingsPreviousSession,
                    "applying changed browser settings replaces the current screen session");
                guiProbeStep++;
            }
            case 12 -> {
                ChromiumSession current = ClientWebScreens.getSession(activeTile);
                if (
                    !readySession(current) || !readySession(settingsPeerSession)
                        || !textureHasZoom(activeTile, 768, 432)
                ) return;
                requireSettings(settingsExpected, ClientWebScreens.getSettings(activeTile),
                    "applying settings keeps the complete current-screen model");
                require(current.getWidth() == 2560 && current.getHeight() == 1440
                    && current.getFrameRate() == 60
                    && current.getQuality() == 75, "applied settings reach the new native browser session");
                require(
                    settingsUrl.equals(current.getUrl()) && settingsProfile.equals(browserProfile(current))
                        && current.getZoomPercent() == 125,
                    "changing settings preserves the webpage URL, profile and 125% zoom");
                require(ClientWebScreens.getSession(settingsPeerTile) == settingsPeerSession,
                    "applying settings leaves a different screen session running");
                requireSettings(peerExpected, ClientWebScreens.getSettings(settingsPeerTile),
                    "two screens keep independent display and audio settings");
                requireSettings(peerExpected, WebScreenSettings.load(browserProfile(settingsPeerSession)),
                    "the second screen persists its own independent settings");
                requireSettings(settingsExpected, WebScreenSettings.load(settingsProfile),
                    "an old pending zoom write cannot overwrite the replacement screen settings");
                verifyTexture(activeTile);
                screenshot("web-screen-settings-applied.png");
                ClientWebScreens.stop(activeTile);
                closeAndAwait(current);
                ClientWebScreens.open(activeTile);
                guiProbeStep++;
            }
            case 13 -> {
                ChromiumSession current = ClientWebScreens.getSession(activeTile);
                if (!readySession(current)) return;
                requireSettings(settingsExpected, ClientWebScreens.getSettings(activeTile),
                    "stopping and reopening a screen restores resolution, refresh, quality, mute and zoom");
                require(
                    current.getWidth() == 2560 && current.getFrameRate() == 60
                        && current.getQuality() == 75
                        && settingsProfile.equals(browserProfile(current)),
                    "reopened browser uses its saved screen profile and settings");
                current.resetZoom();
                WebScreenSettings restored = ClientWebScreens.getSettings(activeTile);
                restored.setResolution(startupPreset());
                restored.setFrameRate(100);
                restored.setJpegQuality(90);
                restored.setMuted(false);
                restored.setZoomPercent(100);
                ClientWebScreens.applySettings(activeTile, restored);
                ClientWebScreens.getSession(activeTile).navigate(settingsUrl);
                ClientWebScreens.stop(settingsPeerTile);
                closeAndAwait(settingsPeerSession);
                settingsPeerTile = null;
                settingsPeerSession = null;
                settingsExpected = restored;
                guiProbeStep++;
            }
            case 14 -> {
                ChromiumSession current = ClientWebScreens.getSession(activeTile);
                if (!readySession(current) || !textureHasZoom(activeTile, 960, 540)) return;
                requireSettings(settingsExpected, ClientWebScreens.getSettings(activeTile),
                    "settings checks restore the requested QA resolution and default presentation settings");
                verifyTexture(activeTile);
                advance(2);
            }
            default -> throw new AssertionError("Unknown settings GUI step " + guiProbeStep);
        }
    }

    private static boolean readySession(ChromiumSession session) {
        String status = session.getStatus();
        if ("failed".equals(status) || "missingBrowser".equals(status)) {
            throw new AssertionError("Settings browser status: " + status);
        }
        return "ready".equals(status);
    }

    private static File browserProfile(ChromiumSession session) throws Exception {
        Field field = ChromiumSession.class.getDeclaredField("profile");
        field.setAccessible(true);
        return (File) field.get(session);
    }

    private static GuiButton guiButtonById(GuiScreen gui, int id) throws Exception {
        Field field = GuiScreen.class.getDeclaredField("buttonList");
        field.setAccessible(true);
        for (Object value : (List<?>) field.get(gui)) {
            GuiButton button = (GuiButton) value;
            if (button.id == id) return button;
        }
        throw new AssertionError("No GUI button " + id);
    }

    private static void guiButtonClick(GuiScreen gui, int id) throws Exception {
        GuiButton button = guiButtonById(gui, id);
        guiMouseClick(gui, button.xPosition + button.width / 2, button.yPosition + button.height / 2);
    }

    private static int guiSlider(GuiScreen gui, int id, int value) throws Exception {
        GuiSlider slider = (GuiSlider) guiButtonById(gui, id);
        double minimum = id == 1 ? 15 : 60;
        int x = slider.xPosition + 4 + (int) Math.round((value - minimum) * (slider.width - 8) / (100 - minimum));
        guiMouseClick(gui, x, slider.yPosition + slider.height / 2);
        return slider.getValueInt();
    }

    private static WebScreenSettings guiSettingsDraft(GuiScreen gui) throws Exception {
        Field field = GuiWebScreenSettings.class.getDeclaredField("draft");
        field.setAccessible(true);
        return (WebScreenSettings) field.get(gui);
    }

    private static boolean textureHasZoom(TileWebScreen tile, int viewportWidth, int viewportHeight) {
        ChromiumSession session = ClientWebScreens.getSession(tile);
        ResourceLocation location = ClientWebScreens.getTexture(tile);
        if (
            location == null || session.getViewportWidth() != viewportWidth
                || session.getViewportHeight() != viewportHeight
        ) return false;
        WebScreenTexture texture = (WebScreenTexture) Minecraft.getMinecraft().getTextureManager().getTexture(location);
        int[] pixels = texture.getTextureData();
        if (pixels.length != session.getWidth() * session.getHeight()) return false;
        int y = (int) Math.round(120.0 * session.getHeight() / viewportHeight);
        int count = 0;
        for (int x = 0; x < session.getWidth(); x++) {
            int color = pixels[y * session.getWidth() + x];
            if ((color >> 16 & 255) < 65 && (color >> 8 & 255) > 180 && (color & 255) > 200) count++;
        }
        return Math.abs(count - session.getWidth() * 100.0 / viewportWidth) < 4;
    }

    private static int guiField(GuiScreen gui, String name) throws Exception {
        Field field = GuiWebScreen.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(gui);
    }

    private void openNext() {
        Minecraft mc = Minecraft.getMinecraft();
        activeTile = (TileWebScreen) mc.theWorld.getTileEntity(x(facingIndex), 70, 0);
        mc.gameSettings.hideGUI = false;
        mc.theWorld.getBlock(x(facingIndex), 70, 0).onBlockActivated(mc.theWorld, x(facingIndex), 70, 0, mc.thePlayer,
            FACINGS[facingIndex], 0.5F, 0.5F, 0.5F);
        applyStartupPreset(activeTile);
        ChromiumSession session = ClientWebScreens.getSession(activeTile);
        require(session != null, "display opens its independent browser session");
        fixtureNavigationRequested = false;
        advance(1);
    }

    private static WebScreenResolution startupPreset() {
        return WebScreenResolution.parse(System.getProperty("gtng.webscreen.qa.preset", "1080p"));
    }

    private static void applyStartupPreset(TileWebScreen tile) {
        WebScreenSettings draft = ClientWebScreens.getSettings(tile);
        if (draft.getResolution() == startupPreset()) return;
        draft.setResolution(startupPreset());
        ClientWebScreens.applySettings(tile, draft);
    }

    private void checkBrowser() {
        ChromiumSession browser = null;
        try {
            if (baselineOnly) {
                checkBaseline();
                browserReady = true;
                return;
            }
            if (resolutionOnly) {
                verifySettingsModel();
                checkResolutions();
                browserReady = true;
                return;
            }
            verifySettingsModel();
            File profile = new File(output(), "browser-profile-" + System.currentTimeMillis());
            browser = new ChromiumSession(profile, "", true);
            ownedBrowser = browser;
            browser.start(fixtureUrl + "?backend");
            awaitBrowser(browser);
            BufferedImage first = awaitFrame(browser);
            require(first.getWidth() == 1920 && first.getHeight() == 1080, "default browser streams exactly 1920x1080");
            require(
                browser.getWidth() == 1920 && browser.getHeight() == 1080
                    && browser.getQuality() == 90
                    && browser.getFrameRate() == 100,
                "default browser uses 1080p, JPEG quality 90 and a 100 FPS capture target");
            awaitAnimation(browser, first);
            ImageIO.write(first, "png", new File(output(), "browser-fixture.png"));
            ChromiumSession active = browser;
            await(() -> events.contains("cookie:new"), "fixture receives its fresh browser profile");
            await(() -> events.stream().anyMatch(value -> value.startsWith("codec:")), "browser reports codecs");
            String codecs = events.stream().filter(value -> value.startsWith("codec:")).findFirst().orElse("");
            require(!codecs.contains("h264=,") && !codecs.endsWith("aac="),
                "installed browser advertises H.264 and AAC playback: " + codecs);
            await(() -> events.contains("video:playing"), "muted generated video advances in the actual browser");
            click(active, 100, 44);
            active.insertText("GTNG-browser-input");
            await(() -> events.contains("input:GTNG-browser-input"), "keyboard text reaches the webpage");
            active.key("rawKeyDown", "Backspace", 8, 0);
            active.key("keyUp", "Backspace", 8, 0);
            await(() -> events.contains("input:GTNG-browser-inpu"), "Backspace edits the focused webpage input");
            click(active, 100, 105);
            await(() -> events.contains("click:clicked"), "mouse click activates the webpage button");
            active.mouse("mouseWheel", 500, 300, "none", 0, 650, 0);
            await(() -> events.contains("scroll:moved"), "mouse wheel scrolls the webpage");
            closeAndAwait(browser);
            browser = new ChromiumSession(profile, "", true);
            ownedBrowser = browser;
            browser.start(fixtureUrl + "?backend");
            awaitBrowser(browser);
            awaitFrame(browser);
            await(() -> events.contains("cookie:persisted"), "reopening the same profile retains its local cookie");
            Files.write(new File(output(), "browser-events.txt").toPath(), events, StandardCharsets.UTF_8);
            probeBilibili(browser);
        } catch (Throwable error) {
            failure = error;
        } finally {
            try {
                closeAndAwait(browser);
            } catch (Throwable error) {
                failure = error;
            }
            ownedBrowser = null;
        }
        if (failure == null) browserReady = true;
    }

    private void checkResolutions() throws Exception {
        verifyFrameConversion();
        verifyFramePublication();
        StringBuilder report = new StringBuilder();
        for (int[] size : new int[][] { { 1920, 1080 }, { 2560, 1440 }, { 3840, 2160 } }) {
            int width = size[0];
            int height = size[1];
            String preset = height + "p";
            File profile = new File(output(), "resolution-profile-" + preset + "-" + System.currentTimeMillis());
            ChromiumSession browser = new ChromiumSession(profile, "", true, width, height, 90, 100);
            ownedBrowser = browser;
            try {
                browser.start(fixtureUrl + "resolution?" + preset);
                awaitBrowser(browser);
                require(
                    browser.getWidth() == width && browser.getHeight() == height
                        && browser.getQuality() == 90
                        && browser.getFrameRate() == 100,
                    preset + " session retains its resolution and capture settings");
                require(browser.getViewportWidth() == 960 && browser.getViewportHeight() == 540,
                    preset + " preserves the 960x540 CSS viewport for readable page layout");
                await(() -> hasViewportEvent(preset, width / 960.0),
                    preset + " browser reports the fixed CSS viewport and expected pixel density");
                BufferedImage frame = awaitResolutionFrame(browser);
                require(frame.getWidth() == width && frame.getHeight() == height,
                    preset + " decoded frame has exactly " + width + "x" + height + " pixels");
                require(stripeContrast(frame) > 100, preset + " JPEG stream preserves alternating one-pixel detail");
                ImageIO.write(frame, "png", new File(output(), "browser-resolution-" + preset + ".png"));
                int[][] corners = { { 20, 20 }, { 940, 20 }, { 20, 520 }, { 940, 520 } };
                String[] names = { "tl", "tr", "bl", "br" };
                for (int index = 0; index < corners.length; index++) {
                    int x = corners[index][0];
                    int y = corners[index][1];
                    String expected = preset + "/corner:" + names[index] + "@" + x + "," + y;
                    click(browser, x, y);
                    await(() -> events.contains(expected), preset + " CSS mouse mapping reaches " + names[index]);
                }
                click(browser, -100, -100);
                await(() -> events.contains(preset + "/corner:tl@0,0"),
                    preset + " mouse coordinates clamp at top left");
                click(browser, 1060, 640);
                await(() -> events.contains(preset + "/corner:br@959,539"),
                    preset + " mouse coordinates clamp at bottom right");
                report.append(preset).append("; pixels=").append(width).append('x').append(height)
                    .append("; jpegQuality=90; captureLimit=100");
                benchmarkFrames(browser, preset, report);
                checkZoom(browser, preset);
                browser.setZoomPercent(125);
                closeAndAwait(browser);
                browser = new ChromiumSession(profile, "", true, width, height, 90, 100);
                ownedBrowser = browser;
                browser.start(fixtureUrl + "zoom?" + preset + "-reopened");
                awaitBrowser(browser);
                require(
                    browser.getZoomPercent() == 125 && browser.getViewportWidth() == 768
                        && browser.getViewportHeight() == 432,
                    preset + " reopening its saved profile restores 125% webpage zoom");
                BufferedImage restored = awaitZoomFrame(browser);
                require(restored.getWidth() == width && restored.getHeight() == height,
                    preset + " persisted zoom restores readable content at the original physical resolution");
                report.append('\n');
            } finally {
                closeAndAwait(browser);
                ownedBrowser = null;
            }
        }
        Files.write(new File(output(), "resolution-results.txt").toPath(),
            report.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void checkBaseline() throws Exception {
        StringBuilder report = new StringBuilder();
        for (int[] size : new int[][] { { 2560, 1440 }, { 3840, 2160 } }) {
            String preset = size[1] + "p";
            ChromiumSession browser = new ChromiumSession(
                new File(output(), "baseline-profile-" + preset + "-" + System.currentTimeMillis()), "", true, size[0],
                size[1], 90, 100);
            ownedBrowser = browser;
            try {
                browser.start(fixtureUrl + "resolution?" + preset);
                awaitBrowser(browser);
                BufferedImage frame = awaitResolutionFrame(browser);
                require(frame.getWidth() == size[0] && frame.getHeight() == size[1],
                    preset + " baseline retains its native capture dimensions");
                report.append(preset).append("; pixels=").append(size[0]).append('x').append(size[1])
                    .append("; jpegQuality=90; captureLimit=100");
                benchmarkFrames(browser, preset, report);
                report.append('\n');
            } finally {
                closeAndAwait(browser);
                ownedBrowser = null;
            }
        }
        Files.write(new File(output(), "baseline-resolution-results.txt").toPath(),
            report.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void verifySettingsModel() throws Exception {
        WebScreenSettings defaults = new WebScreenSettings();
        require(
            defaults.getResolution() == WebScreenResolution.FullHd && defaults.getFrameRate() == 100
                && defaults.getJpegQuality() == 90
                && !defaults.isMuted()
                && defaults.getZoomPercent() == 100,
            "screen settings default to 1080p, 100 FPS, quality 90, audio enabled and 100% zoom");
        WebScreenSettings bounded = defaults.copy();
        bounded.setResolution(null);
        bounded.setFrameRate(-500);
        bounded.setJpegQuality(500);
        bounded.setZoomPercent(500);
        require(bounded.getResolution() == WebScreenResolution.FullHd && bounded.getFrameRate() == 15
            && bounded.getJpegQuality() == 100
            && bounded.getZoomPercent() == 300, "settings setters enforce supported bounds");
        require(defaults.getFrameRate() == 100 && defaults.getJpegQuality() == 90 && defaults.getZoomPercent() == 100,
            "editing a copied settings model leaves its source unchanged");
        File profile = new File(output(), "settings-model-" + System.currentTimeMillis());
        WebScreenSettings saved = defaults.copy();
        saved.setResolution(WebScreenResolution.QuadHd);
        saved.setFrameRate(73);
        saved.setJpegQuality(81);
        saved.setMuted(true);
        saved.setZoomPercent(125);
        saved.save(profile);
        requireSettings(saved, WebScreenSettings.load(profile),
            "atomic settings persistence retains every screen option");
        File json = new File(profile, "webscreen-settings.json");
        Files.write(json.toPath(), "{\"zoomPercent\":125}".getBytes(StandardCharsets.UTF_8));
        WebScreenSettings legacy = defaults.copy();
        legacy.setZoomPercent(125);
        requireSettings(legacy, WebScreenSettings.load(profile),
            "legacy zoom-only files retain the new option defaults");
        Files.write(json.toPath(), ("{\"resolution\":\"4k\",\"frameRate\":1e99,\"jpegQuality\":-999,"
            + "\"zoomPercent\":\"-Infinity\",\"muted\":true}").getBytes(StandardCharsets.UTF_8));
        WebScreenSettings parsed = WebScreenSettings.load(profile);
        require(parsed.getResolution() == WebScreenResolution.UltraHd && parsed.getFrameRate() == 100
            && parsed.getJpegQuality() == 60
            && parsed.getZoomPercent() == 100
            && parsed.isMuted(), "settings JSON bounds large numbers and rejects nonfinite values");
        Files.write(json.toPath(), "{".getBytes(StandardCharsets.UTF_8));
        requireSettings(defaults, WebScreenSettings.load(profile), "malformed settings fall back to usable defaults");
        Files.write(json.toPath(), new byte[16385]);
        requireSettings(defaults, WebScreenSettings.load(profile),
            "oversized settings files are bounded before parsing");
        WebScreenSettings audible = defaults.copy();
        ChromiumSession forced = new ChromiumSession(new File(profile, "qa-forced-mute"), "", true, audible);
        try {
            Field muted = ChromiumSession.class.getDeclaredField("muted");
            muted.setAccessible(true);
            require(muted.getBoolean(forced) && !forced.getSettings().isMuted(),
                "QA browser audio is forced off without changing the player's saved mute preference");
        } finally {
            closeAndAwait(forced);
        }
        verifySettingsPersistenceTiming(profile);
    }

    private void verifySettingsPersistenceTiming(File parent) throws Exception {
        File profile = new File(parent, "close-before-profile-lock");
        Files.createDirectories(profile.toPath());
        WebScreenSettings draft = new WebScreenSettings();
        draft.setResolution(WebScreenResolution.QuadHd);
        draft.setFrameRate(65);
        draft.setJpegQuality(79);
        draft.setMuted(true);
        draft.setZoomPercent(150);
        ChromiumSession closing = new ChromiumSession(profile, "", true, draft.copy());
        try {
            try (RandomAccessFile held = new RandomAccessFile(new File(profile, "webscreen.lock"), "rw");
                FileLock lock = held.getChannel().lock()) {
                closing.start("about:blank");
                closing.close();
                require(!closing.awaitClosed(100),
                    "closing a pending screen retains its latest save intent while the profile is locked");
                requireSettings(draft, WebScreenSettings.load(profile),
                    "reopening a locked profile reads its latest pending draft before disk persistence");
            }
            closeAndAwait(closing);
            require(new File(profile, "webscreen-settings.json").isFile(),
                "closing a pending screen finishes an actual settings file write");
            requireSettings(draft, WebScreenSettings.load(profile),
                "closing before browser startup still saves every requested setting");
        } finally {
            closeAndAwait(closing);
        }
        File replacementProfile = new File(parent, "latest-pending-settings");
        Files.createDirectories(replacementProfile.toPath());
        WebScreenSettings olderDraft = draft.copy();
        olderDraft.setResolution(WebScreenResolution.FullHd);
        olderDraft.setFrameRate(40);
        WebScreenSettings latestDraft = draft.copy();
        latestDraft.setResolution(WebScreenResolution.UltraHd);
        latestDraft.setFrameRate(83);
        latestDraft.setJpegQuality(92);
        ChromiumSession older = null;
        ChromiumSession latest = null;
        try {
            try (RandomAccessFile held = new RandomAccessFile(new File(replacementProfile, "webscreen.lock"), "rw");
                FileLock lock = held.getChannel().lock()) {
                older = new ChromiumSession(replacementProfile, "", true, olderDraft);
                older.persistSettings();
                older.close();
                latest = new ChromiumSession(replacementProfile, "", true, latestDraft);
                latest.persistSettings();
                latest.close();
                require(!latest.awaitClosed(100),
                    "the latest save-only request remains pending until its profile lock is released");
                requireSettings(latestDraft, WebScreenSettings.load(replacementProfile),
                    "pending settings reads select the newest replacement draft");
            }
            closeAndAwait(older);
            closeAndAwait(latest);
            requireSettings(latestDraft, WebScreenSettings.load(replacementProfile),
                "rapid replacement cancels an older pending settings intent and persists the newest complete draft");
        } finally {
            closeAndAwait(older);
            closeAndAwait(latest);
        }
        verifyLiveZoomPending(parent);
    }

    private void verifyLiveZoomPending(File parent) throws Exception {
        File profile = new File(parent, "live-zoom-before-close");
        Files.createDirectories(profile.toPath());
        ChromiumSession running = new ChromiumSession(profile, "", true, new WebScreenSettings());
        ChromiumSession replacement = null;
        CountDownLatch workerEntered = new CountDownLatch(1);
        CountDownLatch releaseWorker = new CountDownLatch(1);
        try (RandomAccessFile held = new RandomAccessFile(new File(profile, "webscreen.lock"), "rw")) {
            FileLock lock = held.getChannel().lock();
            try {
                Field profileLock = ChromiumSession.class.getDeclaredField("profileLock");
                profileLock.setAccessible(true);
                profileLock.set(running, lock);
                Method save = ChromiumSession.class.getDeclaredMethod("savePendingSettings");
                save.setAccessible(true);
                save.invoke(running);
                require(diskZoom(profile) == 100 && WebScreenSettings.load(profile).getZoomPercent() == 100,
                    "live zoom fixture starts with its initial settings intent already saved");
                Field workerField = ChromiumSession.class.getDeclaredField("worker");
                workerField.setAccessible(true);
                ExecutorService worker = (ExecutorService) workerField.get(running);
                worker.execute(() -> {
                    workerEntered.countDown();
                    try {
                        releaseWorker.await();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                });
                require(workerEntered.await(5, TimeUnit.SECONDS),
                    "live zoom fixture blocks its delayed disk save deterministically");
                running.setZoomPercent(150);
                require(diskZoom(profile) == 100, "live zoom remains unsaved on disk while its worker is blocked");
                require(WebScreenSettings.load(profile).getZoomPercent() == 150,
                    "an immediate settings load observes live zoom before the delayed save");
                running.close();
                WebScreenSettings reopened = WebScreenSettings.load(profile);
                require(reopened.getZoomPercent() == 150, "immediate stop and reopen retain a live 150% zoom change");
                reopened.setResolution(WebScreenResolution.QuadHd);
                reopened.setFrameRate(70);
                reopened.setJpegQuality(77);
                reopened.setMuted(true);
                replacement = new ChromiumSession(profile, "", true, reopened.copy());
                replacement.persistSettings();
                replacement.close();
                requireSettings(reopened, WebScreenSettings.load(profile),
                    "the replacement settings intent supersedes the closing session's pending zoom save");
                releaseWorker.countDown();
                closeAndAwait(running);
                closeAndAwait(replacement);
                require(diskZoom(profile) == 150, "the replacement writes the newly inherited zoom to disk");
                requireSettings(reopened, WebScreenSettings.load(profile),
                    "closing the older live session cannot overwrite the replacement's newest complete settings");
            } finally {
                releaseWorker.countDown();
                closeAndAwait(running);
                closeAndAwait(replacement);
                if (lock.isValid()) lock.release();
            }
        }
    }

    private static int diskZoom(File profile) throws Exception {
        String json = new String(Files.readAllBytes(new File(profile, "webscreen-settings.json").toPath()),
            StandardCharsets.UTF_8);
        return new JsonParser().parse(json).getAsJsonObject().get("zoomPercent").getAsInt();
    }

    private void requireSettings(WebScreenSettings expected, WebScreenSettings actual, String label) {
        require(expected.getResolution() == actual.getResolution() && expected.getFrameRate() == actual.getFrameRate()
            && expected.getJpegQuality() == actual.getJpegQuality()
            && expected.isMuted() == actual.isMuted()
            && expected.getZoomPercent() == actual.getZoomPercent(), label);
    }

    private void checkZoom(ChromiumSession browser, String preset) throws Exception {
        browser.navigate(fixtureUrl + "zoom?" + preset);
        for (int zoom : new int[] { 100, 75, 150, 100 }) {
            browser.setZoomPercent(zoom);
            int viewportWidth = (int) Math.round(96000.0 / zoom);
            int viewportHeight = (int) Math.round(54000.0 / zoom);
            require(browser.getZoomPercent() == zoom, preset + " " + zoom + "% updates the requested zoom");
            await(() -> browser.getViewportWidth() == viewportWidth && browser.getViewportHeight() == viewportHeight,
                preset + " " + zoom + "% applies the CSS layout dimensions");
            await(() -> hasZoomEvent(preset, viewportWidth, viewportHeight),
                preset + " " + zoom + "% reaches the actual browser layout");
            BufferedImage frame = awaitZoomFrame(browser);
            require(frame.getWidth() == browser.getWidth() && frame.getHeight() == browser.getHeight(),
                preset + " " + zoom + "% preserves the native physical frame dimensions");
            require(
                Math.abs(sampleWidth(frame, viewportWidth, viewportHeight) - browser.getWidth() * 100.0 / viewportWidth)
                    < 4,
                preset + " " + zoom + "% changes the rendered size of a 100 CSS pixel element");
            for (int[] corner : new int[][] { { 20, 20 }, { viewportWidth - 20, 20 }, { 20, viewportHeight - 20 },
                { viewportWidth - 20, viewportHeight - 20 } }) {
                String expected = preset + "/zoomclick:"
                    + viewportWidth
                    + "x"
                    + viewportHeight
                    + "@"
                    + corner[0]
                    + ","
                    + corner[1];
                click(browser, corner[0], corner[1]);
                await(() -> events.contains(expected),
                    preset + " " + zoom + "% maps CSS corner " + corner[0] + "," + corner[1]);
            }
            if (zoom != 100)
                ImageIO.write(frame, "png", new File(output(), "browser-zoom-" + preset + "-" + zoom + ".png"));
        }
        browser.setZoomPercent(-100);
        require(browser.getZoomPercent() == 25, preset + " zoom clamps at 25%");
        browser.setZoomPercent(500);
        require(browser.getZoomPercent() == 300, preset + " zoom clamps at 300%");
        browser.resetZoom();
        require(browser.getZoomPercent() == 100, preset + " reset returns to 100%");
    }

    private boolean hasZoomEvent(String preset, int width, int height) {
        LayoutState latest = latestLayouts.get(preset);
        return latest != null && latest.dimensions.equals(width + "x" + height);
    }

    private void recordEvent(String event) {
        events.add(event);
        int marker = event.indexOf("/layout:");
        if (marker < 0) return;
        String[] fields = event.substring(marker + 8).split("@");
        if (fields.length != 3) return;
        LayoutState reported = new LayoutState(fields[0], Long.parseLong(fields[1]), Long.parseLong(fields[2]));
        latestLayouts.compute(event.substring(0, marker),
            (key,
                current) -> current == null || reported.document > current.document
                    || reported.document == current.document && reported.sequence > current.sequence ? reported
                        : current);
    }

    private static final class LayoutState {

        private final String dimensions;
        private final long document;
        private final long sequence;

        private LayoutState(String dimensions, long document, long sequence) {
            this.dimensions = dimensions;
            this.document = document;
            this.sequence = sequence;
        }
    }

    private boolean hasZoomClickNear(String preset, int width, int height, int x, int y) {
        String prefix = preset + "/zoomclick:" + width + "x" + height + "@";
        return events.stream().filter(value -> value.startsWith(prefix)).anyMatch(value -> {
            String[] point = value.substring(prefix.length()).split(",");
            return Math.abs(Integer.parseInt(point[0]) - x) <= 3 && Math.abs(Integer.parseInt(point[1]) - y) <= 3;
        });
    }

    private BufferedImage awaitZoomFrame(ChromiumSession browser) throws Exception {
        long deadline = System.currentTimeMillis() + 45000;
        while (!finished && System.currentTimeMillis() < deadline) {
            BufferedImage frame = browser.pollFrame();
            if (
                frame != null && frame.getWidth() == browser.getWidth()
                    && frame.getHeight() == browser.getHeight()
                    && Math.abs(sampleWidth(frame, browser.getViewportWidth(), browser.getViewportHeight())
                        - browser.getWidth() * 100.0 / browser.getViewportWidth()) < 4
            ) return frame;
            Thread.sleep(30);
        }
        throw new AssertionError("No native zoom frame at " + browser.getZoomPercent() + "%");
    }

    private static int sampleWidth(BufferedImage frame, int viewportWidth, int viewportHeight) {
        int y = Math.min(frame.getHeight() - 1, (int) Math.round(120.0 * frame.getHeight() / viewportHeight));
        int count = 0;
        for (int x = 0; x < frame.getWidth(); x++) {
            int color = frame.getRGB(x, y);
            if ((color >> 16 & 255) < 65 && (color >> 8 & 255) > 180 && (color & 255) > 200) count++;
        }
        return count;
    }

    private void verifyFramePublication() throws Exception {
        ChromiumSession browser = new ChromiumSession(new File(output(), "publication-unused-profile"), "", true, 3840,
            2160, 90, 100);
        Method publish = ChromiumSession.class.getDeclaredMethod("publishFrame", long.class, BufferedImage.class);
        publish.setAccessible(true);
        BufferedImage newer = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        BufferedImage older = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        newer.setRGB(0, 0, 0xFF19D8ED);
        older.setRGB(0, 0, 0xFFFF0000);
        try {
            publish.invoke(browser, 20L, newer);
            publish.invoke(browser, 10L, older);
            require(browser.pollFrame() == newer,
                "parallel decode completion cannot replace a newer frame with an older frame");
            publish.invoke(browser, 15L, older);
            require(browser.pollFrame() == null,
                "consuming the newest frame does not permit a late older completion to reappear");
            publish.invoke(browser, 21L, older);
            require(browser.pollFrame() == older, "a subsequent newer frame remains deliverable after polling");
            browser.close();
            publish.invoke(browser, 22L, newer);
            require(browser.pollFrame() == null, "closed sessions reject decoder publications");
        } finally {
            closeAndAwait(browser);
        }
    }

    private boolean hasViewportEvent(String preset, double density) {
        String prefix = preset + "/viewport:960x540@";
        return events.stream().filter(value -> value.startsWith(prefix))
            .anyMatch(value -> Math.abs(Double.parseDouble(value.substring(prefix.length())) - density) < 0.0001);
    }

    private BufferedImage awaitResolutionFrame(ChromiumSession browser) throws Exception {
        long deadline = System.currentTimeMillis() + 45000;
        while (!finished && System.currentTimeMillis() < deadline) {
            BufferedImage frame = browser.pollFrame();
            if (
                frame != null && frame.getWidth() == browser.getWidth()
                    && frame.getHeight() == browser.getHeight()
                    && stripeContrast(frame) > 100
            ) return frame;
            Thread.sleep(50);
        }
        throw new AssertionError(
            "No native-resolution frame with one-pixel chart detail: " + browser.getHeight() + "p");
    }

    private static double stripeContrast(BufferedImage frame) {
        double density = frame.getWidth() / 960.0;
        int start = (int) Math.round(96 * density) + 100;
        int y = (int) Math.round(420 * density);
        int contrast = 0;
        for (int x = start; x < start + 64; x++) {
            contrast += Math.abs((frame.getRGB(x, y) & 255) - (frame.getRGB(x + 1, y) & 255));
        }
        return contrast / 64.0;
    }

    private void benchmarkFrames(ChromiumSession browser, String preset, StringBuilder report) throws Exception {
        long started = System.nanoTime();
        long previousFrame = 0;
        long maximumGap = 0;
        int captured = 0;
        int changed = 0;
        int previousSignature = 0;
        while (!finished && System.nanoTime() - started < TimeUnit.SECONDS.toNanos(15)) {
            BufferedImage frame = browser.pollFrame();
            if (frame != null) {
                long now = System.nanoTime();
                captured++;
                int signature = animationSignature(frame);
                if (changed == 0 || signature != previousSignature) {
                    if (previousFrame != 0) maximumGap = Math.max(maximumGap, now - previousFrame);
                    previousSignature = signature;
                    previousFrame = now;
                    changed++;
                }
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
        }
        long elapsed = System.nanoTime() - started;
        require(changed >= 4,
            preset + " continuously delivers distinct animated native-resolution content for 15 seconds");
        report.append("; consumedFrames=").append(captured).append("; contentChanges=").append(changed)
            .append("; observedMillis=").append(TimeUnit.NANOSECONDS.toMillis(elapsed)).append("; observedDecodedFps=")
            .append(changed * (double) TimeUnit.SECONDS.toNanos(1) / elapsed).append("; maximumFrameGapMillis=")
            .append(maximumGap / 1000000.0).append("; this measures decoded frame delivery, not Minecraft FPS");
    }

    private static int animationSignature(BufferedImage frame) {
        int signature = 1;
        for (int y = 100; y < 180; y += 2) {
            for (int x = 780; x < 860; x += 2) {
                signature = 31 * signature + frame.getRGB(x * frame.getWidth() / 960, y * frame.getHeight() / 540);
            }
        }
        return signature;
    }

    private void verifyTexture(TileWebScreen tile) {
        Minecraft mc = Minecraft.getMinecraft();
        ChromiumSession browser = ClientWebScreens.getSession(tile);
        int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        mc.getTextureManager().bindTexture(ClientWebScreens.getTexture(tile));
        int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        int minification = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER);
        int magnification = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, previous);
        require(width == browser.getWidth() && height == browser.getHeight(),
            "Minecraft uploads the full " + width + "x" + height + " browser frame without downscaling");
        require(minification == GL11.GL_LINEAR && magnification == GL11.GL_LINEAR,
            "browser texture retains linear minification and magnification filters after frame upload");
    }

    private void verifyRenderPresentation(TileWebScreen tile) throws Exception {
        ChromiumSession browser = ClientWebScreens.getSession(tile);
        Field latest = ChromiumSession.class.getDeclaredField("latestImage");
        latest.setAccessible(true);
        @SuppressWarnings("unchecked")
        AtomicReference<BufferedImage> pending = (AtomicReference<BufferedImage>) latest.get(browser);
        Field instance = ClientWebScreens.class.getDeclaredField("INSTANCE");
        instance.setAccessible(true);
        Method render = ClientWebScreens.class.getDeclaredMethod("render", TickEvent.RenderTickEvent.class);
        render.setAccessible(true);
        WebScreenTexture texture = (WebScreenTexture) Minecraft.getMinecraft().getTextureManager()
            .getTexture(ClientWebScreens.getTexture(tile));
        for (int color : new int[] { 0xFF27A4E3, 0xFFDB7352 }) {
            BufferedImage frame = new BufferedImage(browser.getWidth(), browser.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
            frame.setRGB(0, 0, color);
            pending.set(frame);
            render.invoke(instance.get(null), new TickEvent.RenderTickEvent(TickEvent.Phase.START, 0F));
            require(texture.getTextureData()[0] == color,
                "render event presents a new browser frame without waiting for another game tick");
            verifyTexture(tile);
        }
    }

    private void verifyLargeTextureUploads() {
        int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        for (int[] size : new int[][] { { 2560, 1440 }, { 3840, 2160 } }) {
            DynamicTexture texture = null;
            try {
                GL11.glGetError();
                texture = new DynamicTexture(size[0], size[1]);
                int[] pixels = texture.getTextureData();
                pixels[0] = 0xFFFF0000;
                pixels[size[0] - 1] = 0xFF00FF00;
                pixels[pixels.length - size[0]] = 0xFF0000FF;
                pixels[pixels.length - 1] = 0xFFFFFFFF;
                texture.updateDynamicTexture();
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getGlTextureId());
                int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
                int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
                require(width == size[0] && height == size[1] && GL11.glGetError() == GL11.GL_NO_ERROR,
                    "native OpenGL allocates and uploads " + size[0] + "x" + size[1] + " display pixels");
            } finally {
                if (texture != null) texture.deleteGlTexture();
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, previous);
            }
        }
    }

    private void verifyFrameConversion() throws Exception {
        BufferedImage color = new BufferedImage(9, 7, BufferedImage.TYPE_3BYTE_BGR);
        BufferedImage grayscale = new BufferedImage(9, 7, BufferedImage.TYPE_BYTE_GRAY);
        int[] colors = { 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFF000000, 0xFFFFFFFF, 0xFF713DC5, 0xFF00D4A1 };
        for (int y = 0; y < color.getHeight(); y++) {
            for (int x = 0; x < color.getWidth(); x++) {
                int pixel = colors[(x + y * 3) % colors.length];
                color.setRGB(x, y, pixel);
                grayscale.setRGB(x, y, pixel);
            }
        }
        Method convert = ChromiumSession.class.getDeclaredMethod("convertFrame", BufferedImage.class);
        convert.setAccessible(true);
        BufferedImage[] sources = { color, grayscale, color.getSubimage(2, 1, 5, 4) };
        String[] names = { "contiguous BGR", "grayscale", "translated BGR subimage" };
        for (int index = 0; index < sources.length; index++) {
            BufferedImage source = sources[index];
            BufferedImage converted = (BufferedImage) convert.invoke(null, source);
            require(
                converted.getType() == BufferedImage.TYPE_INT_ARGB && converted.getWidth() == source.getWidth()
                    && converted.getHeight() == source.getHeight(),
                names[index] + " converts to matching ARGB dimensions");
            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    if (source.getRGB(x, y) != converted.getRGB(x, y)) {
                        throw new AssertionError(names[index] + " conversion changes RGB or alpha at " + x + "," + y);
                    }
                }
            }
            require(true, names[index] + " conversion preserves every RGB channel and alpha value");
        }
    }

    private void probeBilibili(ChromiumSession browser) throws Exception {
        String result;
        try {
            browser.navigate("https://www.bilibili.com/");
            await(() -> browser.getUrl().contains("bilibili.com"), "public Bilibili URL is reached");
            BufferedImage frame = awaitFrame(browser);
            long deadline = System.currentTimeMillis() + 45000;
            while (isFixtureBackground(frame.getRGB(10, 10)) && System.currentTimeMillis() < deadline) {
                Thread.sleep(100);
                frame = awaitFrame(browser);
            }
            require(!isFixtureBackground(frame.getRGB(10, 10)), "public website replaces the local fixture frame");
            ImageIO.write(frame, "png", new File(output(), "browser-bilibili.png"));
            result = "PUBLIC_PAGE_FRAME; url=" + browser.getUrl()
                + "; status="
                + browser.getStatus()
                + "; no account login or protected video playback attempted";
        } catch (Exception | AssertionError error) {
            result = "UNAVAILABLE; " + error;
        }
        Files.write(new File(output(), "bilibili.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
        System.out.println("WEB_SCREEN_QA: " + result);
    }

    private void startFixture() throws Exception {
        fixture = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        fixture.createContext("/event", exchange -> {
            String query = exchange.getRequestURI().getRawQuery();
            if (query != null) recordEvent(URLDecoder.decode(query, "UTF-8"));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        fixture.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            byte[] body = (path.equals("/resolution") ? resolutionPage()
                : path.equals("/zoom") ? zoomPage(layoutDocuments.incrementAndGet()) : fixturePage())
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        fixture.start();
        fixtureUrl = "http://127.0.0.1:" + fixture.getAddress().getPort() + "/";
    }

    private static String fixturePage() {
        return "<!doctype html><meta charset=utf-8><title>GTNG Browser QA</title>"
            + "<style>body{margin:0;background:#17283c;color:white;font:24px sans-serif;height:2000px}"
            + "input{position:absolute;left:24px;top:24px;width:320px;height:40px;font:24px sans-serif;box-sizing:border-box}"
            + "button{position:absolute;left:24px;top:86px;width:160px;height:40px;font:20px sans-serif}"
            + "canvas{position:absolute;left:400px;top:24px}video{position:absolute;left:24px;top:180px;width:320px}"
            + "h1{position:absolute;top:360px;left:24px}footer{position:absolute;top:1500px;background:#2682d8;width:100%}</style>"
            + "<form id=form><input id=input aria-label=Input><button id=button type=button>Test click</button></form>"
            + "<canvas id=canvas width=160 height=90></canvas>"
            + "<video id=video muted autoplay playsinline></video><h1>GTNG Web Display</h1><footer>Scroll target</footer>"
            + "<script>const qaPrefix=location.search==='?backend'?'':location.search==='?gui'?'gui/':null;"
            + "const report=(k,v)=>qaPrefix===null?null:fetch('/event?'+encodeURIComponent(qaPrefix+k+':'+v));"
            + "document.addEventListener('pointerdown',e=>{const row=e.clientY<40?'t':e.clientY>innerHeight-40?'b':'';"
            + "const column=e.clientX<40?'l':e.clientX>innerWidth-40?'r':'';if(row&&column)report('corner',row+column)});"
            + "report('cookie',document.cookie.includes('gtng_webscreen_qa=1')?'persisted':'new');"
            + "document.cookie='gtng_webscreen_qa=1; Max-Age=3600; Path=/; SameSite=Lax';"
            + "input.oninput=()=>report('input',input.value);button.onclick=()=>{button.textContent='Clicked';report('click','clicked')};"
            + "input.onkeyup=e=>report('keyup',e.key);"
            + "form.onsubmit=e=>{e.preventDefault();report('submit',input.value)};"
            + "onscroll=()=>{if(scrollY>100)report('scroll','moved')};"
            + "const probe=document.createElement('video');report('codec','h264='+probe.canPlayType('video/mp4; codecs=\"avc1.42E01E\"')"
            + "+',aac='+probe.canPlayType('audio/mp4; codecs=\"mp4a.40.2\"'));"
            + "const ctx=canvas.getContext('2d');let n=0;setInterval(()=>{ctx.fillStyle=n++%2?'#28cf8a':'#d659a7';"
            + "ctx.fillRect(0,0,160,90);ctx.fillStyle='white';ctx.fillText('LIVE '+n,15,45)},100);"
            + "const chunks=[],rec=new MediaRecorder(canvas.captureStream(10));rec.ondataavailable=e=>chunks.push(e.data);"
            + "rec.onstop=()=>{video.src=URL.createObjectURL(new Blob(chunks,{type:rec.mimeType}));"
            + "video.play().then(()=>setTimeout(()=>{if(video.currentTime>0)report('video','playing')},350))};"
            + "rec.start();setTimeout(()=>rec.stop(),1200);</script>";
    }

    private static String resolutionPage() {
        return "<!doctype html><meta charset=utf-8><title>Web Display Resolution QA</title>"
            + "<style>body{margin:0;overflow:hidden;background:#17283c;color:white;font:24px sans-serif}"
            + "button{position:fixed;width:64px;height:64px;padding:0;border:0;z-index:2;color:white;font:16px sans-serif}"
            + "#tl{left:0;top:0;background:#d74a4a}#tr{right:0;top:0;background:#35a45f}"
            + "#bl{left:0;bottom:0;background:#406bd2}#br{right:0;bottom:0;background:#ab4dcc}"
            + "canvas{position:absolute;left:96px;top:100px}#live{position:absolute;right:100px;top:100px;width:80px;height:80px}"
            + "h1{position:absolute;left:96px;top:0}</style><h1 id=title></h1>"
            + "<button id=tl>TL</button><button id=tr>TR</button><button id=bl>BL</button><button id=br>BR</button>"
            + "<canvas id=chart></canvas><div id=live></div><script>"
            + "const preset=location.search.slice(1),report=(k,v)=>fetch('/event?'+encodeURIComponent(preset+'/'+k+':'+v));"
            + "title.textContent=preset+' | '+innerWidth+' x '+innerHeight+' | Native detail';"
            + "report('viewport',innerWidth+'x'+innerHeight+'@'+devicePixelRatio);"
            + "document.querySelectorAll('button').forEach(b=>b.onclick=e=>report('corner',b.id+'@'+e.clientX+','+e.clientY));"
            + "const dpr=devicePixelRatio;chart.width=Math.round((innerWidth-192)*dpr);chart.height=Math.round(360*dpr);"
            + "chart.style.width=(innerWidth-192)+'px';chart.style.height='360px';const ctx=chart.getContext('2d');"
            + "ctx.fillStyle='white';ctx.fillRect(0,0,chart.width,chart.height);ctx.scale(dpr,dpr);"
            + "ctx.fillStyle='black';[12,16,24,32,48].forEach((size,i)=>{ctx.font=size+'px sans-serif';"
            + "ctx.fillText(size+'px: Browser text ABCDEFG 0123456789',16,30+i*50)});"
            + "ctx.resetTransform();for(let x=0;x<chart.width;x++){ctx.fillStyle=x%2?'white':'black';"
            + "ctx.fillRect(x,Math.round(300*dpr),1,Math.round(60*dpr))}"
            + "let n=0;function animate(){live.style.background=n++%2?'#28cf8a':'#d659a7';"
            + "live.textContent=n;requestAnimationFrame(animate)}animate();</script>";
    }

    private static String zoomPage(long document) {
        return "<!doctype html><meta charset=utf-8><title>Web Display Zoom QA</title>"
            + "<style>body{margin:0;overflow:hidden;background:#17283c;color:white;font:20px sans-serif}"
            + "button{position:fixed;width:64px;height:64px;border:0;background:#d74a4a;color:white}"
            + "#tl{left:0;top:0}#tr{right:0;top:0}#bl{left:0;bottom:0}#br{right:0;bottom:0}"
            + "#sample{position:absolute;left:100px;top:100px;width:100px;height:40px;background:#19d8ed}"
            + "#live{position:absolute;left:220px;top:100px}h1{position:absolute;left:100px;top:170px;font-size:24px}</style>"
            + "<button id=tl>TL</button><button id=tr>TR</button><button id=bl>BL</button><button id=br>BR</button>"
            + "<div id=sample></div><div id=live></div><h1>100 CSS pixel sample | Readable browser text</h1><script>"
            + "const preset=location.search.slice(1),report=(k,v)=>fetch('/event?'+encodeURIComponent(preset+'/'+k+':'+v));"
            + "const layoutDocument="
            + document
            + ";let layoutSequence=0;"
            + "const layout=()=>report('layout',innerWidth+'x'+innerHeight+'@'+layoutDocument+'@'+(++layoutSequence));onresize=layout;layout();"
            + "document.querySelectorAll('button').forEach(b=>b.onclick=e=>report('zoomclick',innerWidth+'x'+innerHeight+'@'+e.clientX+','+e.clientY));"
            + "let n=0;function animate(){live.textContent='LIVE '+n++;requestAnimationFrame(animate)}animate();</script>";
    }

    private void awaitBrowser(ChromiumSession browser) throws Exception {
        await(() -> {
            String status = browser.getStatus();
            if ("failed".equals(status) || "missingBrowser".equals(status)) {
                throw new AssertionError("Browser startup status: " + status);
            }
            return "ready".equals(status);
        }, "browser starts with an independent muted profile");
    }

    private BufferedImage awaitFrame(ChromiumSession browser) throws Exception {
        long deadline = System.currentTimeMillis() + 45000;
        while (!finished && System.currentTimeMillis() < deadline) {
            BufferedImage frame = browser.pollFrame();
            if (frame != null) {
                int background = frame.getRGB(0, 0);
                for (int y = 1; y < 12; y++) {
                    for (int x = 1; x < 16; x++) {
                        if (frame.getRGB(x * frame.getWidth() / 16, y * frame.getHeight() / 12) != background) {
                            return frame;
                        }
                    }
                }
            }
            Thread.sleep(100);
        }
        throw new AssertionError("No nonuniform browser frame; status=" + browser.getStatus());
    }

    private void awaitAnimation(ChromiumSession browser, BufferedImage first) throws Exception {
        long deadline = System.currentTimeMillis() + 10000;
        int x = 450 * browser.getWidth() / browser.getViewportWidth();
        int y = 50 * browser.getHeight() / browser.getViewportHeight();
        while (!finished && System.currentTimeMillis() < deadline) {
            BufferedImage frame = browser.pollFrame();
            if (frame != null && frame.getRGB(x, y) != first.getRGB(x, y)) {
                require(true, "browser continuously streams the animated canvas");
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Browser frame did not advance with the animated canvas");
    }

    private void await(BooleanSupplier condition, String label) throws Exception {
        long deadline = System.currentTimeMillis() + 45000;
        while (!finished && System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                require(true, label);
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Timed out: " + label);
    }

    private static void click(ChromiumSession browser, int x, int y) {
        browser.mouse("mousePressed", x, y, "left", 1, 0, 0);
        browser.mouse("mouseReleased", x, y, "left", 0, 0, 0);
    }

    private static boolean isFixtureBackground(int color) {
        return Math.abs((color >> 16 & 255) - 23) < 12 && Math.abs((color >> 8 & 255) - 40) < 12
            && Math.abs((color & 255) - 60) < 12;
    }

    private void closeAndAwait(ChromiumSession browser) throws InterruptedException {
        if (browser == null) return;
        browser.close();
        require(browser.awaitClosed(10000), "owned browser process and worker finish bounded cleanup");
    }

    private void advance(int next) {
        stage = next;
        frames = 0;
    }

    private static int x(int index) {
        return index * 3;
    }

    private static File output() {
        return new File(System.getProperty("gtng.webscreen.qa.output", "."));
    }

    private static void screenshot(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private synchronized void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
        System.out.println("WEB_SCREEN_QA: " + message);
    }

    private void fail(Throwable error) {
        error.printStackTrace();
        try {
            Files.write(new File(output(), "failure-events.txt").toPath(), events, StandardCharsets.UTF_8);
            StringBuilder diagnostics = new StringBuilder("stage=").append(stage).append("; guiStep=")
                .append(guiProbeStep);
            ChromiumSession session = activeTile == null ? null : ClientWebScreens.getSession(activeTile);
            if (session != null) {
                diagnostics.append("; status=").append(session.getStatus()).append("; url=").append(session.getUrl())
                    .append("; requestedZoom=").append(session.getZoomPercent()).append("; appliedViewport=")
                    .append(session.getViewportWidth()).append('x').append(session.getViewportHeight());
            }
            GuiScreen gui = Minecraft.getMinecraft().currentScreen;
            diagnostics.append("; gui=").append(gui == null ? "null" : gui.getClass().getName());
            if (gui instanceof GuiWebScreen) {
                diagnostics.append("; view=").append(guiField(gui, "viewX")).append(',').append(guiField(gui, "viewY"))
                    .append(',').append(guiField(gui, "viewWidth")).append(',').append(guiField(gui, "viewHeight"));
            }
            ResourceLocation location = activeTile == null ? null : ClientWebScreens.getTexture(activeTile);
            diagnostics.append("; texturePixels=");
            if (location != null) {
                WebScreenTexture texture = (WebScreenTexture) Minecraft.getMinecraft().getTextureManager()
                    .getTexture(location);
                diagnostics.append(texture.getTextureData().length);
            } else diagnostics.append("none");
            Files.write(new File(output(), "failure-diagnostics.txt").toPath(),
                diagnostics.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("WEB_SCREEN_QA: " + diagnostics);
            screenshot("web-screen-failure.png");
        } catch (Exception diagnosticError) {
            diagnosticError.printStackTrace();
        }
        finish("FAIL; stage " + stage + "; " + error);
    }

    private void finish(String result) {
        if (finished) return;
        finished = true;
        if (camera != null) Minecraft.getMinecraft().renderViewEntity = Minecraft.getMinecraft().thePlayer;
        try {
            closeAndAwait(ownedBrowser);
            if (settingsPeerTile != null) {
                ClientWebScreens.stop(settingsPeerTile);
                closeAndAwait(settingsPeerSession);
            }
            if (activeTile != null) {
                ChromiumSession session = ClientWebScreens.getSession(activeTile);
                ClientWebScreens.stop(activeTile);
                closeAndAwait(session);
            }
        } catch (InterruptedException | AssertionError error) {
            result = "FAIL; cleanup; " + error;
        }
        if (fixture != null) fixture.stop(0);
        try {
            Files.write(new File(output(), "result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
        } catch (Exception error) {
            error.printStackTrace();
        }
        System.out.println("WEB_SCREEN_QA: " + result);
    }
}
