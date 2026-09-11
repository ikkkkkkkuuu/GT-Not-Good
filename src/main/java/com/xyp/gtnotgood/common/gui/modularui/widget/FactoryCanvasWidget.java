package com.xyp.gtnotgood.common.gui.modularui.widget;

import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.IntegratedProductionFactoryGui;
import com.xyp.gtnotgood.utils.machine.factory.FactoryGraph;
import com.xyp.gtnotgood.utils.machine.factory.FactoryRecipeCatalog;
import com.xyp.gtnotgood.utils.machine.factory.FactoryText;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** MUI2 graph canvas with server-committed dragging and directed routes; no old ModularUI classes are used. */
public final class FactoryCanvasWidget extends Widget<FactoryCanvasWidget> implements Interactable {

    private static final int WIDTH = 100;
    private static final int HEIGHT = 42;
    private final FactoryGraph graph;
    private final IntSupplier selected;
    private final IntConsumer select;
    private final IntConsumer open;
    private final IntegratedProductionFactoryGui.FactoryActions actions;
    private final IntFunction<String> status;
    private int panX;
    private int panY;
    private int anchorX;
    private int anchorY;
    private int originX;
    private int originY;
    private int dragging = -1;
    private int linking = -1;
    private int dragX;
    private int dragY;

    public FactoryCanvasWidget(FactoryGraph graph, IntSupplier selected, IntConsumer select, IntConsumer open,
        IntegratedProductionFactoryGui.FactoryActions actions, IntFunction<String> status) {
        this.graph = graph;
        this.selected = selected;
        this.select = select;
        this.open = open;
        this.actions = actions;
        this.status = status;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
        Gui.drawRect(0, 0, getArea().width, getArea().height, 0xff111d2c);
        for (int x = Math.floorMod(panX, 16); x < getArea().width; x += 16)
            Gui.drawRect(x, 0, x + 1, getArea().height, 0xff17283b);
        for (int y = Math.floorMod(panY, 16); y < getArea().height; y += 16)
            Gui.drawRect(0, y, getArea().width, y + 1, 0xff17283b);
        for (FactoryGraph.Node node : graph.nodes) for (int source : node.sources) {
            FactoryGraph.Node from = graph.find(source);
            if (from == null) continue;
            int x1 = positionX(from) + WIDTH;
            int y1 = positionY(from) + HEIGHT / 2;
            int x2 = positionX(node);
            int y2 = positionY(node) + HEIGHT / 2;
            int middle = (x1 + x2) / 2;
            line(x1, y1, middle, y1);
            line(middle, y1, middle, y2);
            line(middle, y2, x2, y2);
            if (x2 > 4 && x2 < getArea().width && y2 > 4 && y2 < getArea().height - 4) {
                Minecraft.getMinecraft().fontRenderer.drawString(">", x2 - 5, y2 - 4, 0xff61d8df);
            }
        }
        for (FactoryGraph.Node node : graph.nodes) {
            int x = positionX(node), y = positionY(node);
            if (x < 0 || y < 0 || x + WIDTH > getArea().width || y + HEIGHT > getArea().height) continue;
            Gui.drawRect(
                x,
                y,
                x + WIDTH,
                y + HEIGHT,
                node.id == linking ? 0xffdaa453
                    : node.id == selected.getAsInt() ? 0xff62d8dd : node.target ? 0xffe6c15c : 0xff526780);
            Gui.drawRect(x + 1, y + 1, x + WIDTH - 1, y + HEIGHT - 1, 0xff24384e);
            FactoryRecipeCatalog.Entry entry = FactoryRecipeCatalog.get(node.recipe);
            String title = (node.target ? "★ " : "") + "#"
                + node.id
                + " "
                + (entry == null ? FactoryText.EMPTY.text() : entry.title());
            text(title, x + 4, y + 4, 0xffedf5ff);
            text("P " + node.parallel + " / OC " + node.overclocks, x + 4, y + 16, 0xff9ccce3);
            text(status.apply(node.id), x + 4, y + 28, 0xffb2d2b9);
        }
    }

    @SideOnly(Side.CLIENT)
    private void text(String value, int x, int y, int color) {
        Minecraft.getMinecraft().fontRenderer
            .drawString(Minecraft.getMinecraft().fontRenderer.trimStringToWidth(value, WIDTH - 8), x, y, color);
    }

    @SideOnly(Side.CLIENT)
    private void line(int x1, int y1, int x2, int y2) {
        int left = Math.max(0, Math.min(x1, x2));
        int top = Math.max(0, Math.min(y1, y2));
        int right = Math.min(getArea().width, Math.max(x1, x2) + 1);
        int bottom = Math.min(getArea().height, Math.max(y1, y2) + 1);
        if (right > left && bottom > top) Gui.drawRect(left, top, right, bottom, 0xff61d8df);
    }

    private int positionX(FactoryGraph.Node node) {
        return panX + (node.id == dragging ? dragX : node.x);
    }

    private int positionY(FactoryGraph.Node node) {
        return panY + (node.id == dragging ? dragY : node.y);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public Result onMousePressed(int button) {
        int x = getContext().getMouseX(), y = getContext().getMouseY();
        FactoryGraph.Node hit = null;
        for (FactoryGraph.Node node : graph.nodes) {
            if (x >= positionX(node) && x < positionX(node) + WIDTH
                && y >= positionY(node)
                && y < positionY(node) + HEIGHT) hit = node;
        }
        if (button == 1) {
            if (hit == null) linking = -1;
            else if (linking < 0) linking = hit.id;
            else {
                actions.send(3, hit.id, linking, 0, "");
                linking = -1;
            }
            return Result.SUCCESS;
        }
        if (button != 0) return Result.IGNORE;
        anchorX = x;
        anchorY = y;
        dragging = hit == null ? -1 : hit.id;
        originX = hit == null ? panX : hit.x;
        originY = hit == null ? panY : hit.y;
        dragX = originX;
        dragY = originY;
        if (hit != null) select.accept(hit.id);
        return Result.SUCCESS;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void onMouseDrag(int button, long time) {
        if (button != 0) return;
        int x = originX + getContext().getMouseX() - anchorX;
        int y = originY + getContext().getMouseY() - anchorY;
        if (dragging < 0) {
            panX = Math.max(-2048, Math.min(100, x));
            panY = Math.max(-2048, Math.min(100, y));
        } else {
            dragX = Math.max(0, Math.min(2048, x));
            dragY = Math.max(0, Math.min(2048, y));
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean onMouseRelease(int button) {
        if (button == 0 && dragging >= 0) {
            int id = dragging;
            boolean clicked = Math.abs(getContext().getMouseX() - anchorX) < 3
                && Math.abs(getContext().getMouseY() - anchorY) < 3;
            dragging = -1;
            if (clicked) open.accept(id);
            else actions.send(2, id, dragX, dragY, "");
        }
        return true;
    }
}
