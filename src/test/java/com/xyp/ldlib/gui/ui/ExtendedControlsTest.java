package com.xyp.ldlib.gui.ui;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.util.ResourceLocation;

import org.junit.Test;
import org.lwjgl.input.Keyboard;

import com.xyp.ldlib.gui.texture.ColorRectTexture;
import com.xyp.ldlib.gui.ui.elements.ColorSelector;
import com.xyp.ldlib.gui.ui.elements.Dialog;
import com.xyp.ldlib.gui.ui.elements.GraphView;
import com.xyp.ldlib.gui.ui.elements.ProgressBar;
import com.xyp.ldlib.gui.ui.elements.SearchComponent;
import com.xyp.ldlib.gui.ui.elements.Slider;
import com.xyp.ldlib.gui.ui.elements.SplitView;
import com.xyp.ldlib.gui.ui.elements.TreeList;
import com.xyp.ldlib.gui.ui.elements.VirtualScrollerView;
import com.xyp.ldlib.gui.ui.event.UIEvents;
import com.xyp.ldlib.gui.ui.style.ModernTheme;

/** Behavioral regressions for capture, modal isolation, virtual extent, camera mapping and model updates. */
public class ExtendedControlsTest {

    private static ModernTheme theme() {
        return new ModernTheme(path -> new ResourceLocation("test", path));
    }

    private static ColorRectTexture texture() {
        return new ColorRectTexture(0xFFFFFFFF);
    }

    @Test
    public void sliderCaptureClampsOutsideAndProgrammaticUpdateIsSilent() {
        UIElement root = new UIElement(10, 20, 200, 200);
        Slider slider = new Slider(5, 5, 100, 15, 10, 20, 2, false, texture(), texture());
        int[] notifications = { 0 };
        slider.setOnChange(value -> notifications[0]++)
            .setValue(13);
        assertEquals(14, slider.getValue(), 0);
        assertEquals(0, notifications[0]);
        root.addChild(slider);
        UIInput input = new UIInput(root);
        input.mouseDown(20, 30, 0);
        input.mouseMove(1000, 30);
        input.mouseUp(1000, 30, 0);
        assertEquals(20, slider.getValue(), 0);
        int count = notifications[0];
        input.mouseMove(20, 30);
        assertEquals(count, notifications[0]);
        input.keyTyped('\0', Keyboard.KEY_HOME, false, false, false);
        assertEquals(10, slider.getValue(), 0);
    }

    @Test
    public void modalTrapsFocusAndEscapeRestoresOwnerWithoutClickThrough() {
        UIElement root = new UIElement(0, 0, 300, 200);
        UIElement owner = new UIElement(0, 0, 20, 20).setFocusable(true);
        root.addChild(owner);
        UIInput input = new UIInput(root);
        input.focus(owner);
        int[] clicks = { 0 };
        owner.addEventListener(UIEvents.CLICK, e -> clicks[0]++);
        Dialog dialog = new Dialog(owner, 100, 60, theme());
        UIElement field = new UIElement(2, 2, 20, 20).setFocusable(true);
        dialog.content.addChild(field);
        dialog.open();
        root.layout();
        input.validate();
        assertSame(field, input.getFocused());
        input.cycleFocus(false);
        assertSame(field, input.getFocused());
        input.mouseDown(5, 5, 0);
        input.mouseUp(5, 5, 0);
        assertEquals(0, clicks[0]);
        assertTrue(input.keyTyped('\0', Keyboard.KEY_ESCAPE, false, false, false));
        assertNull(dialog.getParent());
        assertSame(owner, input.getFocused());
    }

    @Test
    public void nestedModalRestoresFocusAndOwnerRemovalClosesDescendants() {
        UIElement root = new UIElement(0, 0, 300, 200);
        UIElement owner = new UIElement(0, 0, 30, 30).setFocusable(true);
        root.addChild(owner);
        UIInput input = new UIInput(root);
        input.focus(owner);
        Dialog first = new Dialog(owner, 100, 80, theme());
        UIElement field = new UIElement(0, 0, 20, 20).setFocusable(true);
        first.content.addChild(field);
        first.open();
        root.layout();
        input.validate();
        Dialog second = new Dialog(field, 80, 50, theme());
        second.open();
        root.layout();
        input.validate();
        second.close();
        input.validate();
        assertSame(field, input.getFocused());
        owner.setVisible(false);
        root.layout();
        input.validate();
        assertNull(first.getParent());
        assertNull(input.getFocused());
    }

    @Test
    public void virtualRowsStayBoundedAtEndAndAfterModelShrink() {
        List<Integer> items = new ArrayList<>();
        for (int i = 0; i < 10000; i++) items.add(i);
        VirtualScrollerView<Integer> list = new VirtualScrollerView<>(
            0,
            0,
            100,
            100,
            20,
            (item, index) -> new UIElement(0, 0, 90, 20));
        list.setItems(items);
        list.layout();
        assertTrue(list.getInstantiatedRowCount() <= 7);
        assertEquals(199900, list.getMaxScroll());
        list.revealIndex(9999);
        list.layout();
        assertEquals(list.getMaxScroll(), list.getScroll());
        assertTrue(list.getInstantiatedRowCount() <= 7);
        assertEquals(
            199980,
            list.getChildren()
                .get(
                    list.getChildren()
                        .size() - 1)
                .getY());
        list.setItems(Arrays.asList(1, 2));
        list.layout();
        assertEquals(0, list.getScroll());
        assertEquals(2, list.getInstantiatedRowCount());
        list.setItems(new ArrayList<>());
        list.layout();
        assertEquals(0, list.getInstantiatedRowCount());
    }

    @Test
    public void scrollbarDragReachesLastVirtualRowWithoutClickingContent() {
        VirtualScrollerView<Integer> list = new VirtualScrollerView<>(
            0,
            0,
            100,
            100,
            20,
            (item, index) -> new UIElement(0, 0, 100, 20));
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < 1000; i++) values.add(i);
        list.setItems(values);
        list.layout();
        UIInput input = new UIInput(list);
        input.mouseDown(98, 3, 0);
        input.mouseMove(98, 200);
        input.mouseUp(98, 200, 0);
        assertEquals(list.getMaxScroll(), list.getScroll());
        list.layout();
        assertTrue(list.getInstantiatedRowCount() <= 7);
    }

    @Test
    public void splitHonorsMinimumAndShrinkingNeverCreatesNegativeBounds() {
        SplitView split = new SplitView(0, 0, 200, 100, false, texture()).setMinimumPaneSize(40);
        split.setPercentage(0);
        split.layout();
        assertEquals(40, split.first.getWidth());
        assertEquals(156, split.second.getWidth());
        split.setSize(2, 5);
        split.layout();
        assertEquals(0, split.first.getWidth());
        assertEquals(0, split.second.getWidth());
    }

    @Test
    public void graphZoomPreservesCursorWorldPointAndDraggingUsesScale() {
        UIElement root = new UIElement(10, 20, 300, 200);
        GraphView graph = new GraphView(5, 5, 200, 150);
        GraphView.Node node = new GraphView.Node(20, 20, 30, 20, texture());
        graph.addNode(node);
        root.addChild(graph);
        double wx = graph.worldX(80), wy = graph.worldY(60);
        graph.zoomAt(80, 60, 2);
        assertEquals(wx, graph.worldX(80), .00001);
        assertEquals(wy, graph.worldY(60), .00001);
        graph.zoomAt(80, 60, 1);
        UIInput input = new UIInput(root);
        input.mouseDown(40, 50, 0);
        input.mouseMove(60, 70);
        input.mouseUp(60, 70, 0);
        assertEquals(40, node.getX(), .00001);
        assertEquals(40, node.getY(), .00001);
        assertSame(node, graph.nodeAt(60, 70));
        graph.zoomAt(80, 60, 1e4);
        assertEquals(4, graph.getScale(), 0);
    }

    @Test
    public void removingNodeAlsoRemovesEdgesAndSelection() {
        GraphView graph = new GraphView(0, 0, 100, 100);
        GraphView.Node a = new GraphView.Node(0, 0, 20, 20, texture());
        GraphView.Node b = new GraphView.Node(40, 0, 20, 20, texture());
        graph.addNode(a)
            .addNode(b)
            .connect(a, b, 0xFFFFFF);
        graph.removeNode(a);
        assertTrue(
            graph.getConnections()
                .isEmpty());
        assertNull(graph.nodeAt(5, 5));
    }

    @Test
    public void treeKeyboardSelectsAndPrunesDetachedModel() {
        TreeList<String> tree = new TreeList<>(0, 0, 100, 100, theme(), value -> value);
        TreeList.Node<String> root = new TreeList.Node<>("root");
        TreeList.Node<String> child = new TreeList.Node<>("child");
        root.add(child);
        tree.setRoot(root);
        tree.layout();
        UIInput input = new UIInput(tree);
        input.focus(tree);
        input.keyTyped('\0', Keyboard.KEY_DOWN, false, false, false);
        input.keyTyped('\0', Keyboard.KEY_DOWN, false, false, false);
        assertSame(child, tree.getSelected());
        root.remove(child);
        tree.refresh();
        assertNull(tree.getSelected());
        try {
            root.add(root);
            fail("Cycle accepted");
        } catch (IllegalArgumentException expected) {}
    }

    @Test
    public void searchFiltersCaseInsensitivelyAndSetterIsSilent() {
        SearchComponent<String> search = new SearchComponent<String>(0, 0, 100, 20, theme(), value -> value)
            .setCandidates(Arrays.asList("Iron", "Gold", "Iron dust"));
        int[] changes = { 0 };
        search.setOnChange(value -> changes[0]++);
        search.setValue("Gold");
        assertEquals(Arrays.asList("Iron", "Iron dust"), search.search("IRON"));
        assertTrue(
            search.search("no match")
                .isEmpty());
        assertEquals(0, changes[0]);
    }

    @Test
    public void colorArgbRoundTripsAndSetIsSilent() {
        ColorSelector picker = new ColorSelector(0, 0, 200, 160, theme());
        int[] changes = { 0 };
        picker.setOnChange(value -> changes[0]++);
        for (int color : new int[] { 0, 0xFFFFFFFF, 0x8055AAEE, 0xFFFF0000, 0xFF123456 }) {
            picker.setColor(color);
            assertEquals(color, picker.getColor());
        }
        assertEquals(0, changes[0]);
    }

    @Test
    public void progressRejectsNonFiniteAndClampsSuppliedValues() {
        ProgressBar progress = new ProgressBar(0, 0, 100, 10, texture(), texture()).setSupplier(() -> 2);
        progress.tick();
        assertEquals(1, progress.getValue(), 0);
        try {
            progress.setValue(Double.NaN);
            fail("NaN accepted");
        } catch (IllegalArgumentException expected) {}
    }
}
