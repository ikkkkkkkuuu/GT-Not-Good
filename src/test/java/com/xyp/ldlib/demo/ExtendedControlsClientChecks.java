package com.xyp.ldlib.demo;

import java.io.File;
import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ScreenShotHelper;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.ldlib.gui.holder.ModularUIScreen;
import com.xyp.ldlib.gui.ui.UIElement;
import com.xyp.ldlib.gui.ui.elements.ColorSelector;
import com.xyp.ldlib.gui.ui.elements.Dialog;
import com.xyp.ldlib.gui.ui.elements.GraphView;
import com.xyp.ldlib.gui.ui.elements.Menu;
import com.xyp.ldlib.gui.ui.elements.SearchComponent;
import com.xyp.ldlib.gui.ui.elements.Slider;
import com.xyp.ldlib.gui.ui.elements.SplitView;
import com.xyp.ldlib.gui.ui.elements.TextField;
import com.xyp.ldlib.gui.ui.elements.TreeList;
import com.xyp.ldlib.gui.ui.elements.VirtualScrollerView;
import com.xyp.ldlib.gui.ui.style.ModernTheme;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Opt-in real-client render/input checks. No world is opened; screenshots are written before automatic shutdown. */
@Mod(
    modid = "ldlibcontrolsqa",
    name = "LDLib Controls QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public class ExtendedControlsClientChecks {

    private Probe screen;
    private int frames, stage;
    private static final String[] NAMES = { "values", "search", "search-popup", "menu", "dialog", "tree-split",
        "virtual-last-page", "color", "graph", "graph-zoom", "search-filtered", "nested-menu", "split-dragged",
        "color-edited", "graph-dragged", "full-demo" };

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.ldlib.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || screen != null) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld != null || mc.currentScreen == null) return;
        mc.gameSettings.pauseOnLostFocus = false;
        screen = new Probe();
        mc.displayGuiScreen(screen);
        screen.stage(0);
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || screen == null || ++frames < 25) return;
        if (GL11.glGetError() != GL11.GL_NO_ERROR) throw new AssertionError("LDLib render GL error at " + stage);
        Minecraft mc = Minecraft.getMinecraft();
        File output = new File(System.getProperty("gtng.ldlib.qa.output"));
        output.mkdirs();
        ScreenShotHelper
            .saveScreenshot(output, NAMES[stage] + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        System.out.println("LDLIB_CONTROLS_QA_STAGE " + NAMES[stage]);
        if (++stage == NAMES.length) {
            System.out.println("LDLIB_CONTROLS_QA_PASS");
            mc.shutdown();
        } else {
            frames = 0;
            screen.stage(stage);
        }
    }

    /** Uses the same input dispatcher as ModularUIScreen and the production demonstration pages. */
    private static final class Probe extends ModularUIScreen {

        private final ModernTheme theme;
        private UIElement page;
        private int menuActions;

        Probe() {
            super(new UIElement(0, 0, 245, 181));
            theme = new ModernTheme(path -> ModList.GTNotGood.getResourceLocation("textures/gui/ldlib/" + path));
            root.setBackground(theme.panel);
        }

        private void page(int index) {
            input.clear();
            root.clearAllChildren();
            page = ExtendedDemoPages.create(index, theme);
            page.setPosition(10, 10);
            root.addChild(page);
            root.layout();
        }

        void stage(int stage) {
            switch (stage) {
                case 0:
                    page(3);
                    Slider slider = find(page, Slider.class);
                    click(slider.getScreenX() + 60, slider.getScreenY() + 7);
                    require(slider.getValue() > 0 && slider.getValue() < 100, "slider input");
                    break;
                case 1:
                    page(4);
                    break;
                case 2:
                    SearchComponent<?> search = find(page, SearchComponent.class);
                    search.show();
                    root.layout();
                    input.validate();
                    require(input.getFocused() != null, "popup focus");
                    break;
                case 3:
                    input.keyTyped('\0', Keyboard.KEY_DOWN, false, false, false);
                    input.keyTyped('\0', Keyboard.KEY_RETURN, false, false, false);
                    require(
                        root.getChildren()
                            .size() == 1,
                        "search closes after selection");
                    new Menu(page, theme).openAt(
                        page.getScreenX() + 20,
                        page.getScreenY() + 30,
                        Arrays.asList(
                            new Menu.Entry("Apply", true, () -> {}),
                            new Menu.Entry("More", new Menu.Entry("Rename", true, () -> {})),
                            new Menu.Entry("Disabled", false, () -> {})));
                    break;
                case 4:
                    input.keyTyped('\0', Keyboard.KEY_ESCAPE, false, false, false);
                    Dialog.confirm(page, theme, "Apply this choice?", "Apply", "Cancel", result -> {});
                    break;
                case 5:
                    page(5);
                    break;
                case 6:
                    page(6);
                    VirtualScrollerView<?> list = find(page, VirtualScrollerView.class);
                    list.setScroll(list.getMaxScroll());
                    root.layout();
                    require(list.getInstantiatedRowCount() < 12, "virtual row bound");
                    break;
                case 7:
                    page(7);
                    ColorSelector color = find(page, ColorSelector.class);
                    color.setColor(0x8055AAEE);
                    require(color.getColor() == 0x8055AAEE, "ARGB color");
                    break;
                case 8:
                    page(8);
                    break;
                case 9:
                    GraphView graph = find(page, GraphView.class);
                    int x = graph.getScreenX() + 80, y = graph.getScreenY() + 50;
                    double before = graph.worldX(x);
                    input.mouseWheel(x, y, 120);
                    require(Math.abs(before - graph.worldX(x)) < .00001, "zoom anchor");
                    require(graph.getScale() > 1, "wheel zoom");
                    break;
                case 10:
                    page(4);
                    SearchComponent<?> selector = find(page, SearchComponent.class);
                    selector.show();
                    root.layout();
                    input.validate();
                    type("zzzz-no-match");
                    require(
                        find(root, VirtualScrollerView.class).getItems()
                            .isEmpty(),
                        "empty search");
                    TextField query = find(root, TextField.class);
                    input.mouseDown(query.getScreenX() + 5, query.getScreenY() + 5, 1);
                    input.mouseUp(query.getScreenX() + 5, query.getScreenY() + 5, 1);
                    type(
                        selector.getValue()
                            .toString());
                    require(
                        find(root, VirtualScrollerView.class).getItems()
                            .size() == 1,
                        "localized typed search");
                    break;
                case 11:
                    input.keyTyped('\0', Keyboard.KEY_RETURN, false, false, false);
                    require(
                        root.getChildren()
                            .size() == 1,
                        "filtered selection closes popup");
                    Menu nested = new Menu(page, theme);
                    nested.openAt(
                        page.getScreenX() + 80,
                        page.getScreenY() + 35,
                        Arrays.asList(new Menu.Entry("More", new Menu.Entry("Run action", true, () -> menuActions++))));
                    root.layout();
                    input.validate();
                    UIElement parentPanel = nested.getChildren()
                        .get(0);
                    click(parentPanel.getScreenX() + 20, parentPanel.getScreenY() + 10);
                    require(
                        nested.getChildren()
                            .size() == 2,
                        "submenu opens");
                    break;
                case 12:
                    Menu openMenu = find(root, Menu.class);
                    UIElement submenu = openMenu.getChildren()
                        .get(1);
                    click(submenu.getScreenX() + 20, submenu.getScreenY() + 10);
                    require(menuActions == 1 && openMenu.getParent() == null, "submenu action exactly once");
                    boolean[] accepted = { false };
                    Dialog confirmation = Dialog
                        .confirm(page, theme, "Confirm", "Apply", "Cancel", result -> accepted[0] = result);
                    root.layout();
                    input.validate();
                    click(confirmation.content.getScreenX() + 25, confirmation.content.getScreenY() + 62);
                    require(accepted[0] && confirmation.getParent() == null, "confirmation action");
                    page(5);
                    TreeList<?> tree = find(page, TreeList.class);
                    click(tree.getScreenX() + 10, tree.getScreenY() + 30);
                    require(tree.getSelected() != null, "tree pointer selection");
                    SplitView split = find(page, SplitView.class);
                    int dividerX = split.getScreenX() + split.first.getWidth() + 2;
                    input.mouseDown(dividerX, split.getScreenY() + 50, 0);
                    input.mouseMove(dividerX + 30, split.getScreenY() + 50);
                    input.mouseUp(dividerX + 30, split.getScreenY() + 50, 0);
                    require(split.getPercentage() > .6, "split divider drag");
                    break;
                case 13:
                    page(7);
                    ColorSelector edited = find(page, ColorSelector.class);
                    int priorColor = edited.getColor();
                    click(edited.getScreenX() + 50, edited.getScreenY() + 45);
                    require(edited.getColor() != priorColor, "HSV pointer input");
                    TextField argb = find(edited, TextField.class);
                    input.mouseDown(argb.getScreenX() + 5, argb.getScreenY() + 5, 1);
                    input.mouseUp(argb.getScreenX() + 5, argb.getScreenY() + 5, 1);
                    type("7FCC8844");
                    require(edited.getColor() == 0x7FCC8844, "ARGB typed input");
                    break;
                case 14:
                    page(8);
                    GraphView moved = find(page, GraphView.class);
                    GraphView.Node first = moved.getNodes()
                        .get(0);
                    int nx = moved.getScreenX() + 25, ny = moved.getScreenY() + 45;
                    double originalX = first.getX();
                    input.mouseDown(nx, ny, 0);
                    input.mouseMove(nx + 15, ny + 20);
                    input.mouseUp(nx + 15, ny + 20, 0);
                    require(first.getX() == originalX + 15 && moved.getSelected() == first, "node drag");
                    input.mouseDown(nx, ny, 2);
                    input.mouseMove(nx + 20, ny);
                    input.mouseUp(nx + 20, ny, 2);
                    input.keyTyped('\0', Keyboard.KEY_HOME, false, false, false);
                    require(moved.getScale() > 0, "fit after pan");
                    break;
                case 15:
                    Minecraft.getMinecraft()
                        .displayGuiScreen(
                            new LibraryDemoScreen(
                                path -> ModList.GTNotGood.getResourceLocation("textures/gui/ldlib/" + path)));
                    break;
                default:
                    throw new AssertionError(stage);
            }
            root.layout();
            input.validate();
        }

        private void click(int x, int y) {
            input.mouseDown(x, y, 0);
            input.mouseUp(x, y, 0);
        }

        private void type(String text) {
            for (char character : text.toCharArray()) input.keyTyped(character, 0, false, false, false);
            root.layout();
        }

        private static void require(boolean condition, String message) {
            if (!condition) throw new AssertionError(message);
        }

        private static <T> T find(UIElement element, Class<T> type) {
            if (type.isInstance(element)) return type.cast(element);
            for (UIElement child : element.getChildren()) {
                T result = find(child, type);
                if (result != null) return result;
            }
            return null;
        }
    }
}
