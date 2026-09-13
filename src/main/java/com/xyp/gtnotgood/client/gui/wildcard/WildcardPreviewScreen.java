package com.xyp.gtnotgood.client.gui.wildcard;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.xyp.gtnotgood.common.items.wildcard.model.IWildcardFilterComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.IWildcardIOComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.WildcardExpansion;
import com.xyp.gtnotgood.common.items.wildcard.model.WildcardMigration;
import com.xyp.gtnotgood.common.items.wildcard.model.WildcardModelState;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.ldlib.gui.texture.TextTexture;
import com.xyp.ldlib.gui.ui.UIElement;
import com.xyp.ldlib.gui.ui.elements.Button;

/**
 * Read-only integration probe for the reduced LDLib port and existing wildcard model.
 * Migration and expansion operate once on an owned copy; no inventory writes or packets occur.
 * Pagination bounds rendering work independently of the number of expanded materials.
 */
public final class WildcardPreviewScreen extends GuiScreen {

    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 220;
    private static final int ROWS = 7;
    private static final com.xyp.ldlib.gui.ui.style.ModernTheme THEME = new com.xyp.ldlib.gui.ui.style.ModernTheme(
        path -> ModList.GTNotGood.getResourceLocation("textures/gui/ldlib/" + path));

    private final List<List<String>> rows = new ArrayList<>();
    private UIElement root;
    private int selectedTab;
    private int page;
    private int left;
    private int top;

    public WildcardPreviewScreen(ItemStack stack) {
        ItemStack snapshot = stack.copy();
        WildcardMigration.migrateIfNeeded(snapshot);
        List<IWildcardIOComponent> inputs = WildcardModelState.getInputs(snapshot);
        List<IWildcardIOComponent> outputs = WildcardModelState.getOutputs(snapshot);
        List<IWildcardFilterComponent> filters = WildcardModelState.getFilters(snapshot);
        List<String> preview = new ArrayList<>();
        for (WildcardExpansion.Expanded entry : WildcardExpansion.expand(inputs, outputs, filters)) {
            preview.add(
                entry.material.mName + ": " + describeStacks(entry.inputs) + " > " + describeStacks(entry.outputs));
        }
        rows.add(preview);
        rows.add(describeIO(inputs));
        rows.add(describeIO(outputs));
        List<String> filterRows = new ArrayList<>();
        for (IWildcardFilterComponent filter : filters) filterRows.add(filter.describe());
        rows.add(filterRows);
    }

    private static String describeStacks(List<ItemStack> stacks) {
        StringBuilder result = new StringBuilder();
        for (ItemStack stack : stacks) {
            if (result.length() > 0) result.append(", ");
            result.append(stack.stackSize)
                .append("x ")
                .append(stack.getDisplayName());
        }
        return result.toString();
    }

    private static List<String> describeIO(List<IWildcardIOComponent> components) {
        List<String> result = new ArrayList<>();
        for (IWildcardIOComponent component : components) {
            ItemStack display = component.getDisplayStack();
            result.add(
                component.typeKey() + ": "
                    + (display == null ? "-" : display.stackSize + "x " + display.getDisplayName()));
        }
        return result;
    }

    @Override
    public void initGui() {
        left = (width - PANEL_WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;
        rebuild();
    }

    private void rebuild() {
        root = new UIElement(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        root.setBackground(THEME.panel);
        // #tr gui.wildcardprototype.title
        // # Wildcard Pattern - Preview
        // # zh_CN 通配样板符 - 预览
        root.addChild(label(8, 7, 284, 16, StatCollector.translateToLocal("gui.wildcardprototype.title")));
        // #tr gui.wildcardprototype.preview
        // # Preview
        // # zh_CN 预览
        String preview = StatCollector.translateToLocal("gui.wildcardprototype.preview");
        // #tr gui.wildcardprototype.inputs
        // # Inputs
        // # zh_CN 输入
        String inputs = StatCollector.translateToLocal("gui.wildcardprototype.inputs");
        // #tr gui.wildcardprototype.outputs
        // # Outputs
        // # zh_CN 输出
        String outputs = StatCollector.translateToLocal("gui.wildcardprototype.outputs");
        // #tr gui.wildcardprototype.filters
        // # Filters
        // # zh_CN 过滤
        String filters = StatCollector.translateToLocal("gui.wildcardprototype.filters");
        String[] titles = { preview, inputs, outputs, filters };
        UIElement tabs = new UIElement(8, 29, 62, 116);
        for (int i = 0; i < titles.length; i++) {
            final int tab = i;
            tabs.addChild(button(0, i * 27, 62, 23, titles[i], i == selectedTab, () -> {
                selectedTab = tab;
                page = 0;
                rebuild();
            }));
        }
        root.addChild(tabs);
        UIElement content = new UIElement(77, 29, 215, 150);
        content.setBackground(THEME.panel);
        List<String> current = rows.get(selectedTab);
        if (current.isEmpty()) {
            // #tr gui.wildcardprototype.empty
            // # No entries
            // # zh_CN 暂无内容
            content.addChild(label(4, 4, 207, 20, StatCollector.translateToLocal("gui.wildcardprototype.empty")));
        } else {
            for (int i = 0; i < ROWS && page * ROWS + i < current.size(); i++) {
                content.addChild(label(4, 4 + i * 20, 207, 19, current.get(page * ROWS + i)));
            }
        }
        root.addChild(content);
        root.addChild(button(77, 183, 30, 18, "<", false, () -> {
            if (page > 0) {
                page--;
                rebuild();
            }
        }));
        root.addChild(label(110, 183, 148, 18, (page + 1) + " / " + Math.max(1, (current.size() + ROWS - 1) / ROWS)));
        root.addChild(button(262, 183, 30, 18, ">", false, () -> {
            if ((page + 1) * ROWS < current.size()) {
                page++;
                rebuild();
            }
        }));
        // #tr gui.wildcardprototype.readonly
        // # Read-only snapshot - Esc to close
        // # zh_CN 只读快照 - Esc 关闭
        root.addChild(label(8, 203, 284, 12, StatCollector.translateToLocal("gui.wildcardprototype.readonly")));
    }

    private static UIElement label(int x, int y, int width, int height, String text) {
        return new UIElement(x, y, width, height).setBackground(new TextTexture(() -> text, 0xFF202830));
    }

    private static Button button(int x, int y, int w, int h, String text, boolean selected, Runnable action) {
        return new Button(x, y, w, h, THEME.text(selected ? THEME.accent : THEME.button, () -> text), action)
            .setHoverTexture(THEME.text(THEME.hover, () -> text));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        root.draw(mouseX, mouseY, left, top);
        List<String> current = rows.get(selectedTab);
        int row = (mouseY - top - 33) / 20;
        int index = page * ROWS + row;
        if (mouseX >= left + 81 && mouseX < left + 288
            && mouseY >= top + 33
            && mouseY < top + 33 + ROWS * 20
            && index < current.size()) {
            drawHoveringText(
                fontRendererObj.listFormattedStringToWidth(current.get(index), Math.min(260, width - 24)),
                mouseX,
                mouseY,
                fontRendererObj);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (!root.mouseClicked(mouseX, mouseY, button, left, top)) super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
