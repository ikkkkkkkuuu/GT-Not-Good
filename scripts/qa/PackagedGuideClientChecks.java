package com.xyp.gtnotgood.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.compiler.AnchorIndexer;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytVisitor;
import com.hfstudio.guidenh.guide.document.flow.LytFlowAnchor;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.indices.ItemIndex;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.GuideScreen;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.style.Styleable;
import com.xyp.gtnotgood.common.blocks.flux.BlockFluxConnector;
import com.xyp.gtnotgood.common.blocks.flux.TileFluxLogistics;
import com.xyp.gtnotgood.common.blocks.flux.TileFluxPlug;
import com.xyp.gtnotgood.common.blocks.flux.TileFluxPoint;
import com.xyp.gtnotgood.common.blocks.largeinterface.TileLargeInterface;
import com.xyp.gtnotgood.common.blocks.mebridge.TileMEBridgeReceiver;
import com.xyp.gtnotgood.common.blocks.mebridge.TileMEBridgeSender;
import com.xyp.gtnotgood.common.blocks.mecontainer.TileMEContainer;
import com.xyp.gtnotgood.common.blocks.mestock.TileMERequester;
import com.xyp.gtnotgood.common.blocks.packaged.TilePackagedProvider;
import com.xyp.gtnotgood.common.blocks.stockio.TileStockIOInterface;
import com.xyp.gtnotgood.common.parts.advancedio.PartAdvancedIOBus;
import com.xyp.gtnotgood.common.parts.largeinterface.PartLargeInterface;
import com.xyp.gtnotgood.common.parts.mestock.PartRequesterTerminal;
import com.xyp.gtnotgood.common.parts.mestock.PartThresholdExportBus;
import com.xyp.gtnotgood.common.parts.mestock.PartThresholdLevelEmitter;
import com.xyp.gtnotgood.common.parts.stockio.PartStockIOInterface;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.parts.IPartHost;
import appeng.parts.networking.PartCable;
import appeng.tile.networking.TileCableBus;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import thaumcraft.common.tiles.TileArcaneWorkbench;

@Mod(
    modid = "packagedguideqa",
    name = "Packaged Guide QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class PackagedGuideClientChecks {

    private static final ResourceLocation GUIDE = id("guidenh");
    private static final ResourceLocation HOME = id("index.md");
    private static final boolean ME_GUIDE = "me-hatches".equals(System.getProperty("gtng.packagedGuide.qa.page"));
    private static final boolean AE_GUIDE = "ae-devices".equals(System.getProperty("gtng.packagedGuide.qa.page"));
    private static final boolean FLUX_GUIDE = "flux-network".equals(System.getProperty("gtng.packagedGuide.qa.page"));
    private static final ResourceLocation TUTORIAL = id(FLUX_GUIDE ? "flux-network.md"
        : AE_GUIDE ? "ae-devices.md" : ME_GUIDE ? "me-hatches.md" : "packaged-pattern-provider.md");
    private static final String[] ANCHORS = FLUX_GUIDE
        ? new String[] { null, "trio-preview", "energy-preview", "logistics-preview", "troubleshooting" }
        : AE_GUIDE
            ? new String[] { null, "buses-preview", "interfaces-preview", "interface-parts-preview",
                "requester-preview", "network-preview", "troubleshooting" }
            : ME_GUIDE
                ? new String[] { null, "pattern-preview", "mirror-preview", "io-preview", "mold-preview",
                    "troubleshooting" }
                : new String[] { null, "layout", "binding-preview", "record" };
    private static final int SCENE_STAGES = FLUX_GUIDE ? 3 : AE_GUIDE ? 5 : ME_GUIDE ? 4 : 2;
    private final List<String> evidence = new ArrayList<>();
    private boolean started;
    private boolean finished;
    private int ticks;
    private int stage = -1;
    private int frames;
    private boolean positioned;
    private int expectedScroll;
    private int previousScroll = -1;
    private MutableGuide guide;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.packagedGuide.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0F);
        if (finished) {
            mc.shutdown();
            return;
        }
        try {
            if (++ticks > 6000) throw new AssertionError("Client timed out at stage " + stage);
            if (!started && mc.theWorld == null && mc.currentScreen instanceof GuiMainMenu) {
                started = true;
                require(Loader.isModLoaded("guidenh"), "GuideNH is loaded");
                ModConfig.ui.welcomePopupEnabled = false;
                ModConfig.debug.enableDebugMode = false;
                mc.launchIntegratedServer("packaged-guide-qa-" + System.currentTimeMillis(), "Packaged Guide QA",
                    new WorldSettings(83L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (stage < 0 && mc.theWorld != null && mc.thePlayer != null) {
                guide = GuideRegistry.getById(GUIDE);
                require(guide != null, "GuideNH automatically discovers the packaged guide");
                checkParsedPage(HOME);
                checkParsedPage(TUTORIAL);
                if (FLUX_GUIDE) {
                    checkItemIndex(GTNGItemList.FluxPlug.get(1));
                    checkItemIndex(GTNGItemList.FluxPoint.get(1));
                    checkItemIndex(GTNGItemList.FluxLogisticsPlug.get(1));
                } else if (AE_GUIDE) {
                    GTNGItemList[] devices = { GTNGItemList.AdvancedIOBus, GTNGItemList.ThresholdExportBus,
                        GTNGItemList.ThresholdLevelEmitter, GTNGItemList.StockIOInterface,
                        GTNGItemList.StockIOInterfacePart, GTNGItemList.LargeInterface, GTNGItemList.LargeInterfacePart,
                        GTNGItemList.MERequester, GTNGItemList.MERequesterTerminal,
                        GTNGItemList.WirelessDualInterfaceTerminal, GTNGItemList.WildcardPattern,
                        GTNGItemList.PatternSorter, GTNGItemList.MEContainer, GTNGItemList.MEBridgeSender,
                        GTNGItemList.MEBridgeReceiver, GTNGItemList.MEWirelessTransceiver,
                        GTNGItemList.WirelessFrequencyCard };
                    for (GTNGItemList device : devices) checkItemIndex(device.get(1));
                } else if (ME_GUIDE) {
                    GTNGItemList[] machines = { GTNGItemList.SuperMTEHatchCraftingInputBusME,
                        GTNGItemList.SuperMTEHatchCraftingInputME, GTNGItemList.CompactSuperMTEHatchCraftingInputME,
                        GTNGItemList.SuperMTEHatchCraftingInputSlave, GTNGItemList.CircuitMEPatternBuffer,
                        GTNGItemList.CircuitMEPatternMirror, GTNGItemList.SuperAdvancedMEInputHatch,
                        GTNGItemList.SuperAdvancedMEInputBus, GTNGItemList.MaxCapacityMEOutputBus,
                        GTNGItemList.MaxCapacityMEOutputHatch, GTNGItemList.MEOutputAssembly,
                        GTNGItemList.MEDataAccessHatch };
                    for (GTNGItemList machine : machines) checkItemIndex(machine.get(1));
                    ItemStack extruder = ItemList.Machine_LV_Extruder.get(1);
                    require(extruder.getItemDamage() == 281, "the reference LV Extruder uses the expected machine ID");
                    checkItemIndex(extruder);
                } else {
                    checkItemIndex(GTNGItemList.WirelessPackagedPatternProvider.get(1));
                    checkItemIndex(GTNGItemList.ItemWirelessConnector.get(1));
                    checkItemIndex(GTNGItemList.ArcaneWorkbenchCore.get(1));
                }
                open(0);
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || stage < 0) return;
        try {
            if (++frames < (positioned ? 30 : 90)) return;
            Minecraft mc = Minecraft.getMinecraft();
            require(mc.currentScreen instanceof GuideScreen, "the native GuideNH screen is open");
            Field currentPage = GuideScreen.class.getDeclaredField("currentPage");
            currentPage.setAccessible(true);
            GuidePage page = (GuidePage) currentPage.get(mc.currentScreen);
            require(page != null, "the native screen compiles the requested page");
            ResourceLocation expected = stage == 0 ? HOME : TUTORIAL;
            require(page.id().equals(expected), "the native screen displays " + expected);
            require(!guide.isPageFailed(expected), "the page has no parse or compilation failure");
            checkErrors(page);
            if (stage > 0) {
                require(new AnchorIndexer(page.document()).get(ANCHORS[stage]) != null,
                    "the page defines screenshot anchor #" + ANCHORS[stage]);
                checkScenes(page);
            }
            GuideScreen screen = (GuideScreen) mc.currentScreen;
            if (!positioned) {
                positionScreenshot(screen, page);
                positioned = true;
                frames = 0;
                return;
            }
            checkScreenshotPosition(screen, page);
            String name = stage == 0 ? "home" : ANCHORS[stage];
            ScreenShotHelper.saveScreenshot(outputDirectory(), name + ".png", mc.displayWidth, mc.displayHeight,
                mc.getFramebuffer());
            if (stage + 1 == ANCHORS.length) finish("PASS");
            else open(stage + 1);
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void checkParsedPage(ResourceLocation pageId) {
        ParsedGuidePage page = guide.getParsedPage(pageId);
        require(page != null, "the packaged resources contain " + pageId);
        require(page.getSourceResourcePack() instanceof FileResourcePack,
            pageId + " loads from a jar resource pack: " + page.getSourceResourcePack());
        String language = System.getProperty("gtng.packagedGuide.qa.language", "zh_CN").toLowerCase(Locale.ROOT);
        require(page.getLanguage().equalsIgnoreCase(language), pageId + " uses the requested " + language + " page");
        page.getAstRoot();
        require(!page.hasParseFailure(), pageId + " parses without errors");
    }

    private void checkItemIndex(ItemStack stack) {
        PageAnchor anchor = guide.getIndex(ItemIndex.class).findByStack(stack);
        require(anchor != null && TUTORIAL.equals(anchor.pageId()),
            "the item index opens the tutorial for " + stack.getDisplayName());
    }

    private void checkErrors(GuidePage page) {
        page.document().visit(new LytVisitor() {

            @Override
            public Result beforeNode(LytNode node) {
                checkStyle(node);
                return Result.CONTINUE;
            }

            @Override
            public Result beforeFlowContent(LytFlowContent content) {
                checkStyle(content);
                return Result.CONTINUE;
            }
        });
        require(true, page.id() + " has no inline compiler diagnostics or unknown tags/ids");
    }

    private static void checkStyle(Styleable node) {
        var color = node.getStyle().color();
        if (color != null && color.resolve() == ColorUtils.ERROR_TEXT.resolve()) {
            throw new AssertionError("GuideNH inline compiler error in " + node);
        }
    }

    private void checkScenes(GuidePage page) {
        require(page.scenes().size() >= SCENE_STAGES, "the tutorial compiles " + SCENE_STAGES + " 3D scenes");
        boolean provider = false;
        boolean workbench = false;
        Set<Integer> machines = new HashSet<>();
        Set<Class<?>> tiles = new HashSet<>();
        Set<Class<?>> northParts = new HashSet<>();
        for (LytGuidebookScene scene : page.scenes()) {
            require(scene.getLoadFailure() == null, "the scene has no runtime load failure: " + scene.getLoadFailure());
            require(scene.isInteractive(), "the scene supports native rotation and zoom");
            require(!scene.getLevel().getFilledBlocks().isEmpty(), "the scene contains rendered blocks");
            require(!scene.getAnnotations().isEmpty(), "the scene contains operation annotations");
            provider |= scene.getLevel().getTileEntities().stream().anyMatch(TilePackagedProvider.class::isInstance);
            workbench |= scene.getLevel().getTileEntity(5, 1, 1) instanceof TileArcaneWorkbench
                && scene.getLevel().getBlockMetadata(5, 1, 1) == 15;
            for (var tile : scene.getLevel().getTileEntities()) {
                tiles.add(tile.getClass());
                if (AE_GUIDE && tile instanceof IPartHost host) {
                    var part = host.getPart(ForgeDirection.NORTH);
                    if (part != null) {
                        require(tile instanceof TileCableBus, "the scene places real parts in a native AE cable bus");
                        require(host.getPart(ForgeDirection.UNKNOWN) instanceof PartCable,
                            "the preview part is mounted on a real AE cable");
                        northParts.add(part.getClass());
                    }
                }
                if (tile instanceof IGregTechTileEntity machine && machine.getMetaTileEntity() != null) {
                    machines.add((int) machine.getMetaTileID());
                    if (ME_GUIDE || AE_GUIDE) require(machine.getFrontFacing() == ForgeDirection.NORTH,
                        "machine " + machine.getMetaTileID() + " shows its front toward the default preview camera");
                }
            }
        }
        if (FLUX_GUIDE) {
            for (Class<?> type : new Class<?>[] { TileFluxPlug.class, TileFluxPoint.class, TileFluxLogistics.class }) {
                require(tiles.contains(type), "the scenes create the actual tile " + type.getSimpleName());
            }
            checkFluxScenes(page);
        } else if (AE_GUIDE) {
            for (Class<?> type : new Class<?>[] { TileStockIOInterface.class, TileLargeInterface.class,
                TileMERequester.class, TileMEContainer.class, TileMEBridgeSender.class, TileMEBridgeReceiver.class }) {
                require(tiles.contains(type), "the scenes create the actual tile " + type.getSimpleName());
            }
            for (Class<?> type : new Class<?>[] { PartAdvancedIOBus.class, PartThresholdExportBus.class,
                PartThresholdLevelEmitter.class, PartStockIOInterface.class, PartLargeInterface.class,
                PartRequesterTerminal.class }) {
                require(northParts.contains(type),
                    "the scenes create the real north-facing part " + type.getSimpleName());
            }
            require(machines.contains(281), "the scenes create the actual LV Extruder at machine ID 281");
        } else if (ME_GUIDE) {
            for (int id : new int[] { 28504, 28519, 28523, 28505, 28522, 28526, 281 }) {
                require(machines.contains(id), "the scenes create the actual GregTech machine " + id);
            }
        } else {
            require(provider, "the scene creates the real packaged provider tile entity in the preview world");
            require(workbench, "the scene creates the real Thaumcraft arcane workbench at metadata 15");
        }
    }

    private void checkFluxScenes(GuidePage page) {
        List<LytGuidebookScene> scenes = orderedScenes(page);
        require(scenes.size() == 3, "the Flux tutorial contains exactly three scenes");
        LytGuidebookScene trio = scenes.get(0);
        checkFluxTile(trio, 1, GTNGItemList.FluxPlug, TileFluxPlug.class);
        checkFluxTile(trio, 3, GTNGItemList.FluxPoint, TileFluxPoint.class);
        TileFluxLogistics sample = checkFluxTile(trio, 5, GTNGItemList.FluxLogisticsPlug, TileFluxLogistics.class);
        require(sample.outputSide() == ForgeDirection.DOWN, "the introductory logistics plug points down");

        LytGuidebookScene energy = scenes.get(1);
        checkFluxTile(energy, 1, GTNGItemList.FluxPlug, TileFluxPlug.class);
        checkFluxTile(energy, 5, GTNGItemList.FluxPoint, TileFluxPoint.class);
        checkFluxMachine(energy, 0, 161, ForgeDirection.EAST);
        IGregTechTileEntity extruder = checkFluxMachine(energy, 6, 281, ForgeDirection.SOUTH);
        require(
            extruder.getMetaTileEntity() instanceof MTEBasicMachine machine
                && machine.mMainFacing == ForgeDirection.NORTH,
            "the energy preview Extruder shows its main face north and its output south");
        BlockFluxConnector plug = (BlockFluxConnector) energy.getLevel().getBlock(1, 1, 1);
        BlockFluxConnector point = (BlockFluxConnector) energy.getLevel().getBlock(5, 1, 1);
        require(plug.canConnectTo(energy.getLevel(), 1, 1, 1, ForgeDirection.WEST),
            "the Flux Plug connects to the battery buffer's east energy output");
        require(point.canConnectTo(energy.getLevel(), 5, 1, 1, ForgeDirection.EAST),
            "the Flux Point connects to the Extruder's west energy input");

        LytGuidebookScene logistics = scenes.get(2);
        require(logistics.getLevel().getTileEntity(1, 1, 1) instanceof TileMEBridgeSender,
            "the logistics scene creates a real ME Bridge Sender");
        TileFluxLogistics connector = checkFluxTile(logistics, 4, GTNGItemList.FluxLogisticsPlug,
            TileFluxLogistics.class);
        TileEntity chest = logistics.getLevel().getTileEntity(5, 1, 1);
        require(
            chest instanceof TileEntityChest && logistics.getLevel().getBlock(5, 1, 1) == Blocks.chest
                && logistics.getLevel().getBlockMetadata(5, 1, 1) == ForgeDirection.NORTH.ordinal(),
            "the logistics scene creates a real north-facing vanilla chest");
        require(connector.outputSide() == ForgeDirection.EAST, "the logistics plug points east toward the chest");
        if (stage == 3 && positioned) {
            require(connector.destination() == chest,
                "the visible logistics plug targets its adjacent chest; world=" + connector.getWorldObj()
                    + ", destination="
                    + connector.destination());
        }
    }

    private <T extends TileEntity> T checkFluxTile(LytGuidebookScene scene, int x, GTNGItemList item, Class<T> type) {
        TileEntity tile = scene.getLevel().getTileEntity(x, 1, 1);
        require(
            type.isInstance(tile)
                && scene.getLevel().getBlock(x, 1, 1) == Block.getBlockFromItem(item.get(1).getItem()),
            "the scene places " + type.getSimpleName() + " at (" + x + ",1,1) using its registered block");
        return type.cast(tile);
    }

    private IGregTechTileEntity checkFluxMachine(LytGuidebookScene scene, int x, int id, ForgeDirection facing) {
        TileEntity tile = scene.getLevel().getTileEntity(x, 1, 1);
        require(tile instanceof IGregTechTileEntity, "the energy scene places a real GT machine at (" + x + ",1,1)");
        IGregTechTileEntity machine = (IGregTechTileEntity) tile;
        require(
            machine.getMetaTileEntity() != null && machine.getMetaTileID() == id && machine.getFrontFacing() == facing,
            "the energy scene places GT machine " + id + " facing " + facing + " at (" + x + ",1,1)");
        return machine;
    }

    private void open(int nextStage) {
        stage = nextStage;
        frames = 0;
        positioned = false;
        GuideScreen.open(GUIDE, new PageAnchor(stage == 0 ? HOME : TUTORIAL, ANCHORS[stage]));
    }

    private void positionScreenshot(GuideScreen screen, GuidePage page) throws Exception {
        field("pendingRestoreViewState").set(screen, null);
        invoke(screen, "ensureLayout");
        if (stage > 0) {
            field("pendingAnchorScroll").setBoolean(screen, true);
            invoke(screen, "scrollToCurrentAnchor");
        }
        invoke(screen, "clampScroll");
        invoke(screen, "snapVisualScrollToTarget");
        int anchorY = 0;
        if (stage > 0) {
            var target = new AnchorIndexer(page.document()).get(ANCHORS[stage]);
            anchorY = target.flowContent() instanceof LytFlowAnchor anchor && anchor.getLayoutY().isPresent()
                ? anchor.getLayoutY().getAsInt()
                : target.blockNode().getBounds().y();
        }
        expectedScroll = Math.max(0, Math.min(anchorY, (Integer) invoke(screen, "getMaxScroll")));
        require(field("scrollY").getInt(screen) == expectedScroll,
            "native anchor navigation positions #" + ANCHORS[stage] + " at document y=" + expectedScroll);
        if (stage > 0 && stage <= SCENE_STAGES) {
            var bounds = screenshotScene(page).getBounds();
            require(bounds != null, "the screenshot scene has layout bounds");
            field("scrollY").setInt(screen, bounds.y());
            invoke(screen, "clampScroll");
            invoke(screen, "snapVisualScrollToTarget");
            expectedScroll = field("scrollY").getInt(screen);
            require(true, "the screenshot frames its scene: " + describePosition(screen, page));
        }
    }

    private void checkScreenshotPosition(GuideScreen screen, GuidePage page) throws Exception {
        int scroll = field("scrollY").getInt(screen);
        float visualScroll = field("visualScrollY").getFloat(screen);
        require(scroll == expectedScroll && Math.abs(visualScroll - expectedScroll) < 0.1F,
            "the rendered screenshot remains at the requested position: " + describePosition(screen, page));
        if (stage > 0) {
            require(scroll > previousScroll, "each screenshot advances to its distinct tutorial section");
            int viewportHeight = (Integer) invoke(screen, "getDocumentViewportHeight");
            if (stage <= SCENE_STAGES) {
                var bounds = screenshotScene(page).getBounds();
                require(bounds != null && bounds.y() >= scroll && bounds.bottom() <= scroll + viewportHeight,
                    "the complete scene is inside the rendered viewport: " + describePosition(screen, page));
            } else {
                var target = new AnchorIndexer(page.document()).get(ANCHORS[stage]);
                var bounds = target.blockNode().getBounds();
                require(bounds != null && bounds.y() >= scroll - 10 && bounds.y() < scroll + viewportHeight,
                    "the final tutorial section is inside the rendered viewport: " + describePosition(screen, page));
            }
        }
        previousScroll = scroll;
    }

    private String describePosition(GuideScreen screen, GuidePage page) throws Exception {
        String bounds = "none";
        if (page != null && stage > 0 && stage <= SCENE_STAGES && page.scenes().size() >= stage) {
            bounds = String.valueOf(screenshotScene(page).getBounds());
        } else if (page != null && stage > 0) {
            var target = new AnchorIndexer(page.document()).get(ANCHORS[stage]);
            if (target != null) bounds = String.valueOf(target.blockNode().getBounds());
        }
        return "stage=" + stage
            + ", anchor="
            + (stage < 0 ? "none" : ANCHORS[stage])
            + ", bounds="
            + bounds
            + ", scroll="
            + field("scrollY").getInt(screen)
            + ", visualScroll="
            + field("visualScrollY").getFloat(screen)
            + ", expected="
            + expectedScroll
            + ", viewportHeight="
            + invoke(screen, "getDocumentViewportHeight")
            + ", maxScroll="
            + invoke(screen, "getMaxScroll");
    }

    private LytGuidebookScene screenshotScene(GuidePage page) {
        return orderedScenes(page).get(stage - 1);
    }

    private List<LytGuidebookScene> orderedScenes(GuidePage page) {
        List<LytGuidebookScene> scenes = new ArrayList<>(page.scenes());
        require(scenes.stream().allMatch(scene -> scene.getBounds() != null),
            "all screenshot scenes have layout bounds");
        scenes.sort(Comparator.comparingInt(scene -> scene.getBounds().y()));
        return scenes;
    }

    private static Field field(String name) throws Exception {
        Field field = GuideScreen.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Object invoke(GuideScreen screen, String name) throws Exception {
        Method method = GuideScreen.class.getDeclaredMethod(name);
        method.setAccessible(true);
        return method.invoke(screen);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(ModList.GTNotGood.getID(), path);
    }

    private static File outputDirectory() {
        return new File(System.getProperty("gtng.packagedGuide.qa.output", "."));
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        evidence.add(message);
        System.out.println("PACKAGED_GUIDE_QA: " + message);
    }

    private void fail(Throwable failure) {
        failure.printStackTrace();
        evidence.add("FAIL: " + failure);
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen instanceof GuideScreen screen) {
                evidence
                    .add("FAIL POSITION: " + describePosition(screen, (GuidePage) field("currentPage").get(screen)));
            }
            ScreenShotHelper.saveScreenshot(outputDirectory(), "failure.png", mc.displayWidth, mc.displayHeight,
                mc.getFramebuffer());
        } catch (Throwable diagnosticFailure) {
            evidence.add("Failed to capture failure evidence: " + diagnosticFailure);
        }
        finish("FAIL");
    }

    private void finish(String result) {
        finished = true;
        try {
            outputDirectory().mkdirs();
            Files.write(new File(outputDirectory(), "checks.txt").toPath(), evidence, StandardCharsets.UTF_8);
            Files.write(new File(outputDirectory(), "result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
        } catch (Exception failure) {
            failure.printStackTrace();
        }
        System.out.println("PACKAGED_GUIDE_QA: " + result);
    }
}
