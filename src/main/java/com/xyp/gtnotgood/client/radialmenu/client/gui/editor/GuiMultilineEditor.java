package com.xyp.gtnotgood.client.radialmenu.client.gui.editor;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

import org.lwjgl.input.Keyboard;

import com.xyp.gtnotgood.client.radialmenu.client.gui.GuiStack;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.Ui;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.UiDragReorder;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.UiIconButton;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.UiScreen;

public class GuiMultilineEditor extends UiScreen {

    public interface Result {

        /** @param text the lines joined by newlines, in the order shown */
        void onLinesEdited(String text);
    }

    private static final int ID_ADD = 1;

    /** Numbered far apart, like the chain's step controls: a long list must not reach into the next range. */
    private static final int ID_REMOVE_BASE = 1000;
    private static final int ID_UP_BASE = 2000;
    private static final int ID_DOWN_BASE = 3000;

    private static final int NUMBER_WIDTH = 16;
    private static final int REMOVE_WIDTH = 20;
    private static final int ARROW = 18;

    private final String titleKey;
    private final Result result;

    private final List<String> lines = new ArrayList<>();

    private final List<GuiTextField> fields = new ArrayList<>();

    /** Which line each built field belongs to - only visible rows have fields, so the two are not parallel. */
    private final List<Integer> fieldLineIndex = new ArrayList<>();

    /** Focused after the next rebuild, so a line added by Enter is the one you carry on typing into. */
    private int focusLine = -1;

    private int rowTop;

    private final UiDragReorder drag = new UiDragReorder();

    public GuiMultilineEditor(String titleKey, String initialText, Result result) {
        this.titleKey = titleKey;
        this.result = result;

        if (initialText != null) {
            for (String line : initialText.split("\\r?\\n")) {
                lines.add(line);
            }
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
    }

    @Override
    protected String titleKey() {
        return titleKey;
    }

    @Override
    protected int panelWidth() {
        return 360;
    }

    @Override
    protected boolean framedViewport() {
        return true;
    }

    /** The hint and the add button, both of which stay put rather than scrolling away with the lines. */
    @Override
    protected int footerHeight() {
        return 10 + Ui.GAP + Ui.ROW;
    }

    @Override
    protected void buildControls() {
        fields.clear();
        fieldLineIndex.clear();

        int left = contentLeft();
        int fieldLeft = left + UiDragReorder.GRIP + NUMBER_WIDTH;
        int removeLeft = contentRight() - REMOVE_WIDTH;
        int downLeft = removeLeft - ARROW - 2;
        int upLeft = downLeft - ARROW - 2;
        int fieldWidth = upLeft - Ui.GAP - fieldLeft;
        int y = scrolledTop();
        rowTop = y;

        for (int i = 0; i < lines.size(); i++) {
            // Rows that do not fit are not built at all. Creating them and hiding them afterwards left buttons and
            // text fields disagreeing about where the edge was.
            if (!isVisibleRow(y)) {
                y += Ui.STEP;
                continue;
            }

            GuiTextField field = new GuiTextField(this.fontRendererObj, fieldLeft + 1, y + 3, fieldWidth - 2, 14);
            field.setMaxStringLength(256);
            field.setText(lines.get(i));
            field.setCursorPositionEnd();
            if (i == focusLine) {
                field.setFocused(true);
            }
            fields.add(field);
            fieldLineIndex.add(Integer.valueOf(i));

            GuiButton up = new UiIconButton(ID_UP_BASE + i, upLeft, y, ARROW, Ui.ROW, UiIconButton.Icon.Up);
            GuiButton down = new UiIconButton(ID_DOWN_BASE + i, downLeft, y, ARROW, Ui.ROW, UiIconButton.Icon.Down);
            up.enabled = i > 0;
            down.enabled = i < lines.size() - 1;
            this.buttonList.add(up);
            this.buttonList.add(down);

            this.buttonList.add(
                new UiIconButton(ID_REMOVE_BASE + i, removeLeft, y, REMOVE_WIDTH, Ui.ROW, UiIconButton.Icon.Cross));
            y += Ui.STEP;
        }
        focusLine = -1;

        setContentHeight(lines.size() * Ui.STEP);

        GuiButton add = new GuiButton(ID_ADD, left, panelBottom - Ui.PAD - Ui.ROW, contentWidth(), Ui.ROW,
            // #tr radialmenu.lines.add
            // # Add line
            // # zh_CN 添加行
            I18n.format("radialmenu.lines.add"));
        this.buttonList.add(add);
        markFooter(ID_ADD);
        // #tr radialmenu.lines.add.tip
        // # Append a line. Enter inserts a line after the current one
        // # zh_CN 在末尾添加一行；Enter 在当前行后插入新行
        tooltip(ID_ADD, I18n.format("radialmenu.lines.add.tip"));

        addBottomBar("gui.done", null, "gui.cancel");
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == ID_PRIMARY) {
            captureFields();
            result.onLinesEdited(join());
            GuiStack.pop();
            return;
        }
        if (button.id == ID_SECONDARY) {
            onCancel();
            return;
        }
        if (button.id == ID_ADD) {
            addLine(lines.size());
            return;
        }

        // Highest base first: an id belongs to the last range it is above, the same order the chain's steps use.
        if (button.id >= ID_DOWN_BASE) {
            moveLine(button.id - ID_DOWN_BASE, 1);
            return;
        }
        if (button.id >= ID_UP_BASE) {
            moveLine(button.id - ID_UP_BASE, -1);
            return;
        }

        int index = button.id - ID_REMOVE_BASE;
        if (index >= 0 && index < lines.size()) {
            captureFields();
            lines.remove(index);
            if (lines.isEmpty()) {
                lines.add("");
            }
            requestRebuild();
        }
    }

    private void moveLine(int index, int direction) {
        int target = index + direction;
        if (index < 0 || index >= lines.size() || target < 0 || target >= lines.size()) {
            return;
        }
        captureFields();
        lines.set(index, lines.set(target, lines.get(index)));
        // The moved line keeps the focus, so a line can be walked several places without clicking it again.
        focusLine = target;
        requestRebuild();
    }

    /** Adds a line at a position and scrolls it into view, so Enter at the bottom of a long list is not a dead end. */
    private void addLine(int index) {
        captureFields();
        lines.add(Math.min(index, lines.size()), "");
        focusLine = Math.min(index, lines.size() - 1);
        scrollOffset = Math.min(maxScrollAfterAdd(), Math.max(0, (focusLine + 1) * Ui.STEP - viewportHeight()));
        requestRebuild();
    }

    private int maxScrollAfterAdd() {
        return Math.max(0, lines.size() * Ui.STEP - viewportHeight());
    }

    /** Text fields are the source of truth while the screen is open, so they are read back before any reshuffle. */
    private void captureFields() {
        for (int i = 0; i < fields.size(); i++) {
            int index = fieldLineIndex.get(i).intValue();
            if (index < lines.size()) {
                lines.set(index, fields.get(i).getText());
            }
        }
    }

    private String join() {
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line.trim());
        }
        return builder.toString();
    }

    @Override
    protected void beforeScroll() {
        captureFields();
    }

    @Override
    protected void drawContent(int mouseX, int mouseY, float partialTicks) {
        int left = contentLeft();
        for (int i = 0; i < fields.size(); i++) {
            GuiTextField field = fields.get(i);
            int line = fieldLineIndex.get(i).intValue();
            boolean held = drag.isDragging() && drag.grabbedRow() == line;
            if (held) {
                Gui.drawRect(left, field.yPosition - 3, contentRight(), field.yPosition + Ui.ROW - 3, Ui.ROW_SELECTED);
            }
            UiDragReorder.drawGrip(left, field.yPosition - 3, Ui.ROW, held);

            String number = (line + 1) + ".";
            this.fontRendererObj.drawString(number,
                left + UiDragReorder.GRIP + NUMBER_WIDTH - 4 - this.fontRendererObj.getStringWidth(number),
                field.yPosition + 3, Ui.TEXT_MUTED);
            field.drawTextBox();
        }

        drag.drawInsertion(rows(), contentLeft(), contentRight());
        scrollTowardsDrag();
    }

    private UiDragReorder.Rows rows() {
        return new UiDragReorder.Rows() {

            @Override
            public int count() {
                return lines.size();
            }

            @Override
            public int top(int row) {
                return rowTop + row * Ui.STEP;
            }

            @Override
            public int height() {
                return Ui.ROW;
            }
        };
    }

    private void scrollTowardsDrag() {
        if (!drag.isDragging()) {
            return;
        }
        int overshoot = drag.overshoot(rows());
        if (overshoot == 0) {
            return;
        }
        int next = Math.max(0, Math.min(maxScroll(), scrollOffset + Math.max(-6, Math.min(6, overshoot))));
        if (next != scrollOffset) {
            beforeScroll();
            scrollOffset = next;
            requestRebuild();
        }
    }

    @Override
    protected void drawOverlay(int mouseX, int mouseY, float partialTicks) {
        this.fontRendererObj
            // #tr radialmenu.lines.hint
            // # Placeholders: {player} {dim} {x} {y} {z}
            // # zh_CN 占位符：{player} {dim} {x} {y} {z}
            .drawString(I18n.format("radialmenu.lines.hint"), contentLeft(), viewportBottom() + Ui.GAP, Ui.TEXT_MUTED);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        // Before super, and before the fields: a press on a grip is a grip, not a click into the line beside it.
        if (button == 0 && isInsideViewport(mouseY) && drag.press(mouseX, mouseY, contentLeft(), rows())) {
            captureFields();
            return;
        }
        super.mouseClicked(mouseX, mouseY, button);
        // A field clipped at the panel edge still answers to clicks on the part that was cut away.
        if (!isShowing() || !isInsideViewport(mouseY)) {
            return;
        }
        for (GuiTextField field : fields) {
            field.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int mouseButton, long timeSinceClick) {
        drag.moveTo(mouseY);
        super.mouseClickMove(mouseX, mouseY, mouseButton, timeSinceClick);
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int which) {
        if (which == 0) {
            int from = drag.grabbedRow();
            int to = drag.release(rows());
            if (to >= 0 && to != from && from >= 0 && from < lines.size()) {
                // Captured on the way in, so what was typed into the dragged line travels with it.
                lines.add(to, lines.remove(from));
                focusLine = to;
                requestRebuild();
            }
        }
        super.mouseMovedOrUp(mouseX, mouseY, which);
    }

    @Override
    protected boolean handleKey(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            addLine(focusedLine() + 1);
            return true;
        }
        for (GuiTextField field : fields) {
            if (field.textboxKeyTyped(typedChar, keyCode)) {
                return true;
            }
        }
        return false;
    }

    /** @return the line being typed into, or the last one when nothing has focus */
    private int focusedLine() {
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).isFocused()) {
                return fieldLineIndex.get(i).intValue();
            }
        }
        return lines.size() - 1;
    }
}
