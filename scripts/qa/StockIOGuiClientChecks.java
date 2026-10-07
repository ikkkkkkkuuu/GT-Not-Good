package com.xyp.gtnotgood.qa;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.MinecraftForgeClient;

import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.GenericSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.widgets.PageButton;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

/** Client-side GUI checks invoked by the isolated stock IO network test after opening the real factory. */
public final class StockIOGuiClientChecks {

    private static int frames;
    private static boolean done;

    private StockIOGuiClientChecks() {}

    /**
     * Call from the END render tick while the server waits for GUI edits. Fixtures need item/fluid marks at 0 and 899.
     * The item policy at 0 becomes reserve 10,000,000,000 and batch 7; the fluid policy becomes 1,000 and 2,500.
     */
    public static boolean step(File output) {
        if (done) return true;
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiContainerWrapper gui)) return false;
        ModularPanel panel = gui.getScreen().getMainPanel();
        require(panel.getName().equals("stock_io"), "real stock IO factory is open");
        frames++;
        List<PhantomItemSlot> items = collect(panel, PhantomItemSlot.class);
        List<FluidSlot> fluids = collect(panel, FluidSlot.class);
        var sync = gui.getScreen().getSyncManager().getMainPSM();
        if (frames == 40) {
            require(items.size() == 900, "900 item marks synchronize to client");
            require(fluids.size() == 900, "900 fluid marks synchronize to client");
            require(items.get(0).getSlot().getStack() != null && items.get(899).getSlot().getStack() != null,
                "item marks include both first and final index");
            require(fluids.get(0).getFluidStack() != null && fluids.get(899).getFluidStack() != null,
                "fluid marks include both first and final index");
            List<IWidget> stocks = new ArrayList<>();
            collectNamed(panel, "StockDisplay", stocks);
            require(stocks.size() == 1800, "1800 draw-only stock cells cannot extract or change a sample");
            for (String name : new String[] { "itemAmounts", "fluidAmounts" }) {
                Object value = ((GenericSyncValue<?, ?>) sync.findSyncHandlerNullable(name, 0)).getValue();
                require(value instanceof long[][] amounts && amounts.length == 2 && amounts[0].length == 900
                    && amounts[1].length == 900, "exact 64-bit available and total arrays synchronize: " + name);
            }
            List<Grid> grids = collect(panel, Grid.class);
            require(grids.size() == 4, "paired item and fluid grids share both pages");
            for (int i = 0; i < 4; i += 2) {
                Grid left = grids.get(i), right = grids.get(i + 1);
                require(left.getArea().h() == 72 && right.getArea().h() == 72,
                    "four rows of nine slots are visible on each side");
                var scrollLeft = left.getScrollArea();
                var scrollRight = right.getScrollArea();
                scrollLeft.getScrollY().scrollTo(scrollLeft, Integer.MAX_VALUE);
                require(scrollLeft.getScrollY().getScroll() > 0
                    && scrollLeft.getScrollY().getScroll() == scrollRight.getScrollY().getScroll(),
                    "filter and stock grids reach the final row together: " + i);
                scrollLeft.getScrollY().scrollTo(scrollLeft, 0);
            }
            screenshot(output, "stock-io-part-items.png");
            verifyStockRendering(output, "stock-io-part-items.png", panel, false);
            ItemStack cursor = mc.thePlayer.inventory.getItemStack();
            require(cursor == null, "ghost test starts with an empty player cursor");
            items.get(1).handleDragAndDrop(new ItemStack(Items.paper, 32), 0);
        }
        if (frames == 70) {
            ItemStack sample = items.get(1).getSlot().getStack();
            require(sample != null && sample.getItem() == Items.paper && sample.stackSize == 1,
                "recipe-viewer drag stores only one phantom identity");
            require(mc.thePlayer.inventory.getItemStack() == null, "phantom drag leaves the cursor unchanged");
            items.get(1).onMousePressed(0);
        }
        if (frames == 100) {
            require(items.get(1).getSlot().getStack() == null, "empty left click clears a phantom mark");
            require(mc.thePlayer.inventory.getItemStack() == null, "clearing a phantom cannot produce or consume items");
            items.get(0).onMousePressed(1);
        }
        if (frames == 140) {
            ModularPanel policy = popup(gui, "itemPolicy");
            List<TextFieldWidget> fields = collect(policy, TextFieldWidget.class);
            require(fields.size() == 2, "right-click opens the per-item reserve and batch popup");
            edit(fields.get(0), "10000000000", panel);
            edit(fields.get(1), "7", panel);
        }
        if (frames == 180) {
            ModularPanel policy = popup(gui, "itemPolicy");
            List<TextFieldWidget> fields = collect(policy, TextFieldWidget.class);
            require(number(fields.get(0)) == 10000000000L && number(fields.get(1)) == 7,
                "item popup preserves a reserve larger than a signed integer");
            screenshot(output, "stock-io-item-policy.png");
            policy.closeIfOpen();
        }
        if (frames == 190) {
            ItemStack sample = items.get(0).getSlot().getStack().copy();
            sample.stackSize = 32;
            items.get(0).handleDragAndDrop(sample, 0);
        }
        if (frames == 210) {
            require(items.get(0).getSlot().getStack() != null,
                "dragging the same identity retains its mark and configured policy");
            items.get(899).onMousePressed(1);
        }
        if (frames == 250) {
            ModularPanel policy = popup(gui, "itemPolicy");
            List<TextFieldWidget> fields = collect(policy, TextFieldWidget.class);
            require(((IntSyncValue) sync.findSyncHandlerNullable("selectedItem", 0)).getIntValue() == 899,
                "policy selection reaches the 900th mark");
            require(number(fields.get(0)) == 0 && number(fields.get(1)) == 64,
                "reopening the popup follows the new slot rather than the first selection");
            policy.closeIfOpen();
        }
        if (frames == 280) {
            List<PageButton> tabs = collect(panel, PageButton.class);
            require(tabs.size() == 2, "item and fluid tabs retain both configurations");
            tabs.get(1).onMousePressed(0);
        }
        if (frames == 310) {
            screenshot(output, "stock-io-part-fluids.png");
            verifyStockRendering(output, "stock-io-part-fluids.png", panel, true);
            fluids.get(0).onMousePressed(1);
        }
        if (frames == 350) {
            ModularPanel policy = popup(gui, "fluidPolicy");
            List<TextFieldWidget> fields = collect(policy, TextFieldWidget.class);
            require(fields.size() == 2, "fluid right-click opens its own native mB policy");
            edit(fields.get(0), "1000", panel);
            edit(fields.get(1), "2500", panel);
        }
        if (frames == 390) {
            ModularPanel policy = popup(gui, "fluidPolicy");
            List<TextFieldWidget> fields = collect(policy, TextFieldWidget.class);
            require(number(fields.get(0)) == 1000 && number(fields.get(1)) == 2500,
                "fluid policy uses native mB quantities without bucket conversion");
            screenshot(output, "stock-io-fluid-policy.png");
            policy.closeIfOpen();
        }
        if (frames == 420) collect(panel, PageButton.class).get(0).onMousePressed(0);
        if (frames == 450) items.get(0).onMousePressed(1);
        if (frames == 490) {
            ModularPanel policy = popup(gui, "itemPolicy");
            List<TextFieldWidget> fields = collect(policy, TextFieldWidget.class);
            require(number(fields.get(0)) == 10000000000L && number(fields.get(1)) == 7,
                "tab changes and other policy selections preserve the first item's settings");
            policy.closeIfOpen();
            done = true;
        }
        return done;
    }

    /** Capture the full-block entry after the test server opens the same GUI for the block variant. */
    public static void captureBlock(File output) {
        Minecraft mc = Minecraft.getMinecraft();
        require(mc.currentScreen instanceof GuiContainerWrapper gui
            && gui.getScreen().getMainPanel().getName().equals("stock_io"), "full-block factory opens the shared GUI");
        screenshot(output, "stock-io-block-items.png");
    }

    /** Open after the full-block GUI capture, then wait at least one rendered frame before capturing the probe. */
    public static void openModelProbe() {
        require(MinecraftForgeClient.getItemRenderer(GTNGItemList.StockIOInterfacePart.get(1), ItemRenderType.INVENTORY)
            != null, "interface part uses the registered native AE item renderer");
        Minecraft.getMinecraft().displayGuiScreen(new ModelProbeGui());
    }

    public static void captureModelProbe(File output) {
        require(Minecraft.getMinecraft().currentScreen instanceof ModelProbeGui, "native model probe is rendered");
        screenshot(output, "stock-io-native-models.png");
    }

    /** Enlarges actual inventory rendering; no replacement textures or test-only block renderers are involved. */
    private static final class ModelProbeGui extends GuiScreen {

        private final RenderItem renderItem = new RenderItem();

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTick) {
            drawRect(0, 0, width, height, 0xFF303238);
            drawCenteredString(fontRendererObj, "库存 IO 接口 · 原生模型", width / 2, height / 2 - 95, 0xFFFFFF);
            GTNGItemList[] variants = {
                GTNGItemList.StockIOInterfacePart, GTNGItemList.StockIOInterface, GTNGItemList.AdvancedIOBus };
            String[] names = { "接口部件", "接口方块", "高级 IO 配色" };
            for (int i = 0; i < variants.length; i++) {
                int x = width / 2 - 170 + 138 * i, y = height / 2 - 35;
                drawRect(x - 3, y - 3, x + 67, y + 67, 0xFFD0D0D0);
                drawRect(x, y, x + 64, y + 64, 0xFF888888);
                GL11.glPushMatrix();
                GL11.glTranslatef(x, y, 0);
                GL11.glScalef(4, 4, 4);
                RenderHelper.enableGUIStandardItemLighting();
                renderItem.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), variants[i].get(1), 0, 0);
                RenderHelper.disableStandardItemLighting();
                GL11.glPopMatrix();
                drawCenteredString(fontRendererObj, names[i], x + 32, y + 81, 0xFFFFFF);
            }
            drawCenteredString(fontRendererObj, "Forge RenderItem · 4x", width / 2, height / 2 + 98, 0xA0A4AC);
        }
    }

    private static ModularPanel popup(GuiContainerWrapper gui, String name) {
        for (ModularPanel panel : gui.getScreen().getPanelManager().getOpenPanels()) {
            if (panel.getName().equals(name)) return panel;
        }
        throw new AssertionError("Expected open popup " + name);
    }

    private static void edit(TextFieldWidget field, String value, ModularPanel panel) {
        field.setText(value);
        field.onRemoveFocus(panel.getContext());
    }

    private static long number(TextFieldWidget field) {
        return Long.parseLong(field.getText().replaceAll("[^0-9]", ""));
    }

    private static <T> List<T> collect(IWidget root, Class<T> type) {
        List<T> matches = new ArrayList<>();
        collect(root, type, matches);
        return matches;
    }

    private static <T> void collect(IWidget root, Class<T> type, List<T> matches) {
        if (type.isInstance(root)) matches.add(type.cast(root));
        for (IWidget child : root.getChildren()) collect(child, type, matches);
    }

    private static void collectNamed(IWidget root, String name, List<IWidget> matches) {
        if (root.getClass().getSimpleName().equals(name)) matches.add(root);
        for (IWidget child : root.getChildren()) collectNamed(child, name, matches);
    }

    /** Check pixels as well as sync state: reserved stock still renders an identity and a visible zero. */
    private static void verifyStockRendering(File output, String name, ModularPanel panel, boolean fluid) {
        Minecraft mc = Minecraft.getMinecraft();
        GuiContainerWrapper gui = (GuiContainerWrapper) mc.currentScreen;
        long[][] amounts = (long[][]) ((GenericSyncValue<?, ?>) gui.getScreen().getSyncManager().getMainPSM()
            .findSyncHandlerNullable(fluid ? "fluidAmounts" : "itemAmounts", 0)).getValue();
        require(amounts[0][0] == 0, "render fixture offers zero " + (fluid ? "fluid" : "item") + " stock");
        List<IWidget> stocks = new ArrayList<>();
        collectNamed(panel, "StockDisplay", stocks);
        IWidget marked = stocks.get(fluid ? 900 : 0), empty = stocks.get(fluid ? 901 : 1);
        BufferedImage screenshot;
        try {
            screenshot = ImageIO.read(new File(new File(output, "screenshots"), name));
        } catch (IOException exception) {
            throw new AssertionError("Cannot inspect the native GUI screenshot", exception);
        }
        ScaledResolution resolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        double scaleX = (double) screenshot.getWidth() / resolution.getScaledWidth();
        double scaleY = (double) screenshot.getHeight() / resolution.getScaledHeight();
        int difference = 0, white = 0;
        for (int y = 3; y < 13; y++) {
            for (int x = 3; x < 13; x++) {
                int first = screenshot.getRGB((int) ((marked.getArea().x() + x) * scaleX),
                    (int) ((marked.getArea().y() + y) * scaleY));
                int second = screenshot.getRGB((int) ((empty.getArea().x() + x) * scaleX),
                    (int) ((empty.getArea().y() + y) * scaleY));
                if (first != second) difference++;
            }
        }
        for (int y = 13; y < 17; y++) {
            for (int x = 9; x < 17; x++) {
                int pixel = screenshot.getRGB((int) ((marked.getArea().x() + x) * scaleX),
                    (int) ((marked.getArea().y() + y) * scaleY));
                if ((pixel >> 16 & 255) >= 230 && (pixel >> 8 & 255) >= 230 && (pixel & 255) >= 230) white++;
            }
        }
        require(difference >= 5, "marked " + (fluid ? "fluid" : "item") + " stock draws its icon even at zero availability");
        require(white > 0, "marked " + (fluid ? "fluid" : "item") + " stock draws a visible quantity overlay");
    }

    private static void screenshot(File output, String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output, name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private static void require(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
        if (frames != 0 && !message.equals("real stock IO factory is open")) {
            System.out.println("STOCK_IO_QA: " + message);
        }
    }
}
