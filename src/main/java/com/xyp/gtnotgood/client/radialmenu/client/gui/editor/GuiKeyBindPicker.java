package com.xyp.gtnotgood.client.radialmenu.client.gui.editor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Mouse;

import com.xyp.gtnotgood.client.radialmenu.client.gui.GuiStack;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.Ui;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.UiList;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.UiScreen;
import com.xyp.gtnotgood.client.radialmenu.client.input.KeyBindingLookup;

public class GuiKeyBindPicker extends UiScreen {

    public interface Callback {

        void onKeyBindPicked(String description, String category);
    }

    private static final class Row {

        final String heading;
        final KeyBinding binding;

        Row(String heading, KeyBinding binding) {
            this.heading = heading;
            this.binding = binding;
        }
    }

    private final Callback callback;

    private final List<Row> rows = new ArrayList<>();

    private GuiTextField searchField;
    private String query = "";

    private UiList list;
    private int listScroll;

    public GuiKeyBindPicker(Callback callback) {
        this.callback = callback;
    }

    @Override
    protected String titleKey() {
        return "radialmenu.editor.pickKeybind";
    }

    @Override
    protected int panelWidth() {
        return 360;
    }

    @Override
    protected void buildControls() {
        searchField = new GuiTextField(this.fontRendererObj, contentLeft() + 1, contentTop() + 3, contentWidth() - 2,
            14);
        searchField.setMaxStringLength(64);
        searchField.setText(query);
        searchField.setCursorPositionEnd();
        searchField.setFocused(true);

        // The search sits above the frame; the frame itself takes the shared edges like every other box.
        list = new UiList(viewportLeft(), contentTop() + Ui.STEP, viewportRight(), viewportBottom(), 12);
        rebuildRows();
        list.scrollTo(listScroll, rows.size());

        addBottomBar("gui.cancel", null, null);
    }

    private void rebuildRows() {
        rows.clear();

        String needle = query.trim().toLowerCase();
        Map<String, List<KeyBinding>> byCategory = new LinkedHashMap<>();

        for (KeyBinding binding : KeyBindingLookup.all()) {
            if (!needle.isEmpty() && !matches(binding, needle)) {
                continue;
            }
            String category = binding.getKeyCategory();
            List<KeyBinding> bucket = byCategory.get(category);
            if (bucket == null) {
                bucket = new ArrayList<>();
                byCategory.put(category, bucket);
            }
            bucket.add(binding);
        }

        for (Map.Entry<String, List<KeyBinding>> entry : byCategory.entrySet()) {
            rows.add(new Row(I18n.format(entry.getKey()), null));
            for (KeyBinding binding : entry.getValue()) {
                rows.add(new Row(null, binding));
            }
        }
    }

    private static boolean matches(KeyBinding binding, String needle) {
        return contains(I18n.format(binding.getKeyDescription()), needle)
            || contains(binding.getKeyDescription(), needle)
            || contains(I18n.format(binding.getKeyCategory()), needle)
            || contains(binding.getKeyCategory(), needle);
    }

    private static boolean contains(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase().contains(needle);
    }

    @Override
    protected void drawContent(int mouseX, int mouseY, float partialTicks) {
        searchField.drawTextBox();
        list.drawFrame();

        int hovered = list.itemAt(mouseX, mouseY, rows.size());

        for (int i = 0; i < list.rowsVisible() && i + list.firstRow() < rows.size(); i++) {
            Row row = rows.get(i + list.firstRow());
            int y = list.rowTop(i);

            if (row.heading != null) {
                this.fontRendererObj.drawString(row.heading, list.textLeft(), y + 2, Ui.TEXT_HEADER);
                continue;
            }

            list.drawRowBackground(i, false, i + list.firstRow() == hovered);

            String name = I18n.format(row.binding.getKeyDescription());
            this.fontRendererObj.drawString(Ui.fit(name, list.textWidth() - 80), list.textLeft() + 6, y + 2, Ui.TEXT);

            boolean unbound = KeyBindingLookup.isUnbound(row.binding);
            // #tr radialmenu.editor.unbound
            // # not bound
            // # zh_CN 未绑定
            String key = unbound ? I18n.format("radialmenu.editor.unbound")
                : GameSettings.getKeyDisplayString(row.binding.getKeyCode());
            this.fontRendererObj.drawString(key, list.right - 8 - this.fontRendererObj.getStringWidth(key), y + 2,
                unbound ? Ui.TEXT_MUTED : Ui.TEXT);
        }

        list.drawScrollbar(rows.size());
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        list.scroll(Mouse.getEventDWheel(), rows.size());
        listScroll = list.firstRow();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (!isShowing()) {
            return;
        }
        searchField.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton != 0) {
            return;
        }
        int index = list.itemAt(mouseX, mouseY, rows.size());
        if (index < 0) {
            return;
        }
        Row row = rows.get(index);
        // Headings are labels, not choices.
        if (row.binding == null) {
            return;
        }
        callback.onKeyBindPicked(row.binding.getKeyDescription(), row.binding.getKeyCategory());
        GuiStack.pop();
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == ID_PRIMARY) {
            onCancel();
        }
    }

    @Override
    protected boolean handleKey(char typedChar, int keyCode) {
        if (searchField.textboxKeyTyped(typedChar, keyCode)) {
            query = searchField.getText();
            listScroll = 0;
            rebuildRows();
            list.scrollTo(0, rows.size());
            return true;
        }
        return false;
    }
}
