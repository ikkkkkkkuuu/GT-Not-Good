package com.xyp.ldlib.demo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.xyp.ldlib.gui.texture.ColorRectTexture;
import com.xyp.ldlib.gui.ui.UIElement;
import com.xyp.ldlib.gui.ui.elements.ColorSelector;
import com.xyp.ldlib.gui.ui.elements.Dialog;
import com.xyp.ldlib.gui.ui.elements.GraphView;
import com.xyp.ldlib.gui.ui.elements.Label;
import com.xyp.ldlib.gui.ui.elements.Menu;
import com.xyp.ldlib.gui.ui.elements.ProgressBar;
import com.xyp.ldlib.gui.ui.elements.SearchComponent;
import com.xyp.ldlib.gui.ui.elements.Slider;
import com.xyp.ldlib.gui.ui.elements.SplitView;
import com.xyp.ldlib.gui.ui.elements.TreeList;
import com.xyp.ldlib.gui.ui.elements.VirtualScrollerView;
import com.xyp.ldlib.gui.ui.event.UIEvents;
import com.xyp.ldlib.gui.ui.style.ModernTheme;

/** Interactive showcase of the additional 1.21 controls; all values are disposable local demonstration data. */
public final class ExtendedDemoPages {

    private ExtendedDemoPages() {}

    public static UIElement create(int index, ModernTheme theme) {
        UIElement page = new UIElement(0, 0, 225, 161);
        switch (index) {
            case 3:
                values(page, theme);
                break;
            case 4:
                search(page, theme);
                break;
            case 5:
                tree(page, theme);
                break;
            case 6:
                virtual(page, theme);
                break;
            case 7:
                page.addChild(new ColorSelector(4, 2, 217, 157, theme).setColor(0xFF55AAEE));
                break;
            case 8:
                graph(page, theme);
                break;
            default:
                throw new IllegalArgumentException("Unknown demo page");
        }
        return page;
    }

    private static void values(UIElement page, ModernTheme theme) {
        // #tr gui.ldlibdemo.dragvalue
        // # Drag / arrows: adjust value
        // # zh_CN 拖动滑块或方向键调整数值
        page.addChild(new Label(0, 0, 225, 18, StatCollector.translateToLocal("gui.ldlibdemo.dragvalue")));
        Slider slider = new Slider(8, 26, 202, 16, 0, 100, 1, false, theme.input, theme.accent).setValue(40);
        page.addChild(slider);
        page.addChild(new Label(8, 45, 202, 18, () -> Math.round(slider.getValue()) + "%"));
        page.addChild(
            new ProgressBar(8, 70, 164, 18, theme.input, theme.accent).setSupplier(() -> slider.getValue() / 100));
        page.addChild(
            new ProgressBar(8, 97, 164, 18, theme.input, theme.accent)
                .setDirection(ProgressBar.FillDirection.RIGHT_TO_LEFT)
                .setInterpolation(.2)
                .setSupplier(() -> slider.getValue() / 100));
        page.addChild(
            new ProgressBar(181, 70, 14, 72, theme.input, theme.accent)
                .setDirection(ProgressBar.FillDirection.BOTTOM_TO_TOP)
                .setSupplier(() -> slider.getValue() / 100));
        page.addChild(
            new ProgressBar(203, 70, 14, 72, theme.input, theme.accent)
                .setDirection(ProgressBar.FillDirection.TOP_TO_BOTTOM)
                .setSupplier(() -> slider.getValue() / 100));
    }

    private static void search(UIElement page, ModernTheme theme) {
        List<String> candidates = Arrays.asList(
            new ItemStack(Items.iron_ingot).getDisplayName(),
            new ItemStack(Items.gold_ingot).getDisplayName(),
            new ItemStack(Items.diamond).getDisplayName(),
            new ItemStack(Items.emerald).getDisplayName(),
            new ItemStack(Items.redstone).getDisplayName(),
            new ItemStack(Items.coal).getDisplayName(),
            new ItemStack(Items.quartz).getDisplayName());
        // #tr gui.ldlibdemo.searchhint
        // # Open, type, then Enter to select
        // # zh_CN 打开后输入关键词，回车选择
        page.addChild(new Label(0, 0, 225, 18, StatCollector.translateToLocal("gui.ldlibdemo.searchhint")));
        SearchComponent<String> search = new SearchComponent<String>(8, 25, 209, 22, theme, value -> value)
            .setCandidates(candidates)
            .setValue(candidates.get(0));
        page.addChild(search);
        page.addChild(new Label(8, 52, 209, 18, () -> search.getValue()));
        // #tr gui.ldlibdemo.confirm
        // # Apply this choice?
        // # zh_CN 应用此选择？
        String confirm = StatCollector.translateToLocal("gui.ldlibdemo.confirm");
        // #tr gui.ldlibdemo.accept
        // # Apply
        // # zh_CN 应用
        String accept = StatCollector.translateToLocal("gui.ldlibdemo.accept");
        // #tr gui.ldlibdemo.cancel
        // # Cancel
        // # zh_CN 取消
        String cancel = StatCollector.translateToLocal("gui.ldlibdemo.cancel");
        String[] status = { "" };
        // #tr gui.ldlibdemo.dialog
        // # Open dialog
        // # zh_CN 打开确认弹窗
        String dialog = StatCollector.translateToLocal("gui.ldlibdemo.dialog");
        page.addChild(
            theme.button(
                8,
                78,
                209,
                22,
                () -> dialog,
                () -> Dialog
                    .confirm(page, theme, confirm, accept, cancel, result -> status[0] = result ? accept : cancel)));
        // #tr gui.ldlibdemo.menuhint
        // # Menu (or right-click this page)
        // # zh_CN 打开菜单（或右键本页）
        String menuLabel = StatCollector.translateToLocal("gui.ldlibdemo.menuhint");
        List<Menu.Entry> entries = Arrays.asList(
            new Menu.Entry(accept, true, () -> status[0] = accept),
            new Menu.Entry(
                dialog,
                new Menu.Entry(
                    confirm,
                    true,
                    () -> Dialog.confirm(
                        page,
                        theme,
                        confirm,
                        accept,
                        cancel,
                        result -> status[0] = result ? accept : cancel))),
            new Menu.Entry(cancel, false, () -> {}));
        page.addChild(
            theme.button(
                8,
                106,
                209,
                22,
                () -> menuLabel,
                () -> new Menu(page, theme).openAt(page.getScreenX() + 8, page.getScreenY() + 106, entries)));
        page.addEventListener(
            UIEvents.MOUSE_DOWN,
            e -> { if (e.button == 1) new Menu(page, theme).openAt(e.x, e.y, entries); });
        page.addChild(new Label(8, 135, 209, 18, () -> status[0]));
    }

    private static void tree(UIElement page, ModernTheme theme) {
        SplitView split = new SplitView(0, 0, 225, 161, false, theme.accent).setMinimumPaneSize(60);
        TreeList<String> tree = new TreeList<>(0, 0, 105, 150, theme, value -> value);
        // #tr gui.ldlibdemo.materials
        // # Materials
        // # zh_CN 材料
        TreeList.Node<String> root = new TreeList.Node<>(StatCollector.translateToLocal("gui.ldlibdemo.materials"));
        // #tr gui.ldlibdemo.metals
        // # Metals
        // # zh_CN 金属
        TreeList.Node<String> metals = new TreeList.Node<>(StatCollector.translateToLocal("gui.ldlibdemo.metals"));
        metals.add(new TreeList.Node<>(new ItemStack(Items.iron_ingot).getDisplayName()));
        metals.add(new TreeList.Node<>(new ItemStack(Items.gold_ingot).getDisplayName()));
        root.add(metals);
        root.add(new TreeList.Node<>(new ItemStack(Items.diamond).getDisplayName()));
        tree.setRoot(root);
        tree.setExpanded(metals, true);
        split.first.addChild(tree);
        // #tr gui.ldlibdemo.splithint
        // # Drag divider
        // # zh_CN 拖动分隔条
        split.second.addChild(new Label(0, 4, 100, 22, StatCollector.translateToLocal("gui.ldlibdemo.splithint")));
        split.second
            .addChild(new Label(0, 35, 100, 22, () -> tree.getSelected() == null ? "..." : tree.getSelected().value));
        split.setOnChange(value -> {
            split.layout();
            tree.setSize(Math.max(0, split.first.getWidth() - 6), split.first.getHeight());
            for (UIElement child : split.second.getChildren())
                child.setSize(split.second.getWidth(), child.getHeight());
        });
        page.addChild(split);
    }

    private static void virtual(UIElement page, ModernTheme theme) {
        // #tr gui.ldlibdemo.virtualrow
        // # Entry
        // # zh_CN 条目
        String label = StatCollector.translateToLocal("gui.ldlibdemo.virtualrow");
        int[] selected = { -1 };
        VirtualScrollerView<Integer> list = new VirtualScrollerView<>(
            0,
            22,
            225,
            139,
            22,
            (value, index) -> theme.button(0, 0, 219, 22, () -> label + " " + value, () -> selected[0] = value));
        list.setThumbTexture(theme.scrollThumb);
        List<Integer> items = new ArrayList<>();
        for (int i = 0; i < 10000; i++) items.add(i);
        list.setItems(items);
        page.addChild(
            new Label(
                0,
                0,
                225,
                18,
                () -> String.format(Locale.ROOT, "10,000 / %d / %d", list.getInstantiatedRowCount(), selected[0])));
        page.addChild(list);
    }

    private static void graph(UIElement page, ModernTheme theme) {
        GraphView graph = new GraphView(0, 21, 225, 140);
        // #tr gui.ldlibdemo.graphhint
        // # Wheel: zoom / drag: move / Home: fit
        // # zh_CN 滚轮缩放 / 拖动移动 / Home 居中
        page.addChild(new Label(0, 0, 225, 18, StatCollector.translateToLocal("gui.ldlibdemo.graphhint")));
        GraphView.Node a = new GraphView.Node(
            10,
            35,
            66,
            28,
            theme.text(theme.button, () -> new ItemStack(Items.iron_ingot).getDisplayName()));
        GraphView.Node b = new GraphView.Node(
            130,
            10,
            66,
            28,
            theme.text(theme.accent, () -> new ItemStack(Items.gold_ingot).getDisplayName()));
        GraphView.Node c = new GraphView.Node(
            130,
            85,
            66,
            28,
            theme.text(new ColorRectTexture(0xFF99DDDD), () -> new ItemStack(Items.diamond).getDisplayName()));
        graph.addNode(a)
            .addNode(b)
            .addNode(c)
            .connect(a, b, 0xFFFFFF)
            .connect(a, c, 0x66DDFF);
        page.addChild(graph);
    }
}
