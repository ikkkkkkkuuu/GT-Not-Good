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
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.UiColorButton;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.UiDragReorder;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.UiIconButton;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.UiScreen;
import com.xyp.gtnotgood.client.radialmenu.client.icon.IconRenderer;
import com.xyp.gtnotgood.client.radialmenu.client.profile.ProfileManager;
import com.xyp.gtnotgood.client.radialmenu.client.toolbelt.ToolBeltMenu;
import com.xyp.gtnotgood.client.radialmenu.config.ColorConfig;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionField;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSpec;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionTypes;
import com.xyp.gtnotgood.client.radialmenu.core.model.IconSpec;
import com.xyp.gtnotgood.client.radialmenu.core.model.MenuNode;
import com.xyp.gtnotgood.client.radialmenu.core.model.SlotLayout;
import com.xyp.gtnotgood.client.radialmenu.core.model.StyleResolver;

import cpw.mods.fml.client.config.GuiSlider;

public class GuiMenuSettings extends UiScreen implements GuiIconPicker.Callback {

    private static final int ID_ICON = 1;
    private static final int ID_FIELD_BASE = 40;
    private static final int ID_CLEAR_BASE = 100;
    private static final int ID_UP_BASE = 200;
    private static final int ID_DOWN_BASE = 300;
    private static final int ID_COPY_BASE = 400;
    private static final int ID_DELETE_BASE = 500;
    private static final int ID_MOVE_BASE = 600;

    private static final int ARROW = 18;

    private static final int GROUP_GAP = 6;

    private final MenuNode menu;
    private final boolean toolBelt;
    private final boolean readOnly;

    private final ActionSpec draft;

    private GuiTextField titleField;

    /** Survives control rebuilds, which recreate the field. */
    private String titleText;
    private IconSpec icon;
    private int iconRowTop;

    private final List<ActionField> fields = new ArrayList<>();

    private final List<Integer> fieldRowTops = new ArrayList<>();
    private final List<GuiTextField> inputs = new ArrayList<>();

    /** Parallel to the fields, like the text boxes: a slider holds its value in the widget rather than in the spec. */
    private final List<GuiSlider> sliders = new ArrayList<>();

    /** The menu's entries, reordered here and written back only on save. */
    private final List<MenuNode> workingChildren;

    /** Where each drawn row sits, so the icon and title can be drawn with the content rather than over a button. */
    private final List<int[]> rowPositions = new ArrayList<>();
    private final List<MenuNode> rowNodes = new ArrayList<>();

    private final List<Integer> rowIndices = new ArrayList<>();

    private final UiDragReorder drag = new UiDragReorder();

    /** Entries on their way to another menu, applied when this screen is saved. */
    private final List<MenuNode[]> pendingMoves = new ArrayList<>();

    private int generalSectionTop;
    private int entriesSectionTop;

    public GuiMenuSettings(MenuNode menu) {
        this.menu = menu;
        this.toolBelt = ToolBeltMenu.isMenu(menu);
        this.readOnly = menu.isBuiltIn() && !toolBelt;
        this.draft = SubmenuFields.toSpec(menu);
        this.titleText = menu.title == null ? "" : menu.title;
        this.icon = menu.icon == null ? null : menu.icon.copy();
        this.workingChildren = new ArrayList<>(menu.childrenOrEmpty());
    }

    @Override
    protected String titleKey() {
        // #tr radialmenu.menu.title
        // # Menu settings
        // # zh_CN 菜单设置
        return "radialmenu.menu.title";
    }

    @Override
    protected int panelWidth() {
        return 330;
    }

    @Override
    protected boolean framedViewport() {
        return true;
    }

    @Override
    protected void buildControls() {
        if (readOnly) {
            setContentHeight(Ui.STEP);
            addBottomBar("gui.done", null, null);
            return;
        }
        fields.clear();
        inputs.clear();
        sliders.clear();
        fieldRowTops.clear();

        int left = contentLeft();
        int controlLeft = left + Ui.LABEL_WIDTH + Ui.GAP;
        int controlWidth = contentRight() - controlLeft;

        generalSectionTop = scrolledTop();
        int y = generalSectionTop + 14;

        // Built at its real size: GuiTextField works out its horizontal scroll from the width it has when the
        // text is set, so a placeholder-sized field resized afterwards renders blank until it is clicked.
        titleField = new GuiTextField(this.fontRendererObj, controlLeft + 1, y + 3, controlWidth - 2, 14);
        titleField.setMaxStringLength(48);
        titleField.setText(titleText);
        titleField.setCursorPositionZero();
        y += Ui.STEP;

        if (toolBelt) {
            iconRowTop = y;
            String iconLabel = icon == null || icon.id == null ? I18n.format("radialmenu.editor.pickIcon")
                : Ui.fit(icon.id, controlWidth - Ui.ROW - 16);
            this.buttonList.add(new GuiButton(ID_ICON, controlLeft + Ui.ROW + Ui.GAP, y, controlWidth - Ui.ROW - Ui.GAP,
                Ui.ROW, iconLabel));
            tooltip(ID_ICON, describe("radialmenu.editor.icon.tip"));
            y += Ui.STEP;
        }

        List<ActionField> available = SubmenuFields.fields(menu);
        for (int fieldIndex = 0; fieldIndex < available.size(); fieldIndex++) {
            ActionField field = available.get(fieldIndex);
            if (!FieldControls.isRelevant(field, draft)) {
                continue;
            }
            ActionField nextField = fieldIndex + 1 < available.size() ? available.get(fieldIndex + 1) : null;
            int index = fields.size();
            fields.add(field);
            tooltip(ID_FIELD_BASE + index,
                toolBelt && ActionTypes.PARAM_SLOT_COUNT.equals(field.key) ? EditorRestrictions.resizeTip()
                    : describe(field.tooltipKey()));

            String current = draft.getString(field.key, field.defaultValue);
            sliders.add(null);

            if (FieldControls.standsApart(field)) {
                y += Ui.GAP;
            }
            fieldRowTops.add(Integer.valueOf(y));

            if (FieldControls.isButton(field)) {
                int mainWidth = FieldControls.hasClear(field) ? controlWidth - Ui.GAP - FieldControls.CLEAR_WIDTH
                    : controlWidth;

                boolean editable = field.kind != ActionField.Kind.Color || FieldControls.colorEnabled(field.key);

                GuiButton main = field.kind == ActionField.Kind.Color
                    ? colorButton(ID_FIELD_BASE + index, field, controlLeft, y, mainWidth)
                    : new GuiButton(ID_FIELD_BASE + index, controlLeft, y, mainWidth, Ui.ROW,
                        FieldControls.buttonLabel(field, current));
                main.enabled = editable;
                this.buttonList.add(main);
                inputs.add(null);

                if (FieldControls.hasClear(field)) {
                    GuiButton clear = new UiIconButton(ID_CLEAR_BASE + index, controlLeft + mainWidth + Ui.GAP, y,
                        FieldControls.CLEAR_WIDTH, Ui.ROW, UiIconButton.Icon.Cross);
                    clear.enabled = current != null && !current.trim().isEmpty();
                    this.buttonList.add(clear);
                    tooltip(ID_CLEAR_BASE + index, describe("radialmenu.profileColors.clear.tip"));
                }
                if (!editable) {
                    tooltip(ID_FIELD_BASE + index, FieldControls.colorOffTip(field.key));
                }
            } else if (field.hasRange()) {
                GuiSlider slider = FieldControls.slider(ID_FIELD_BASE + index, controlLeft, y, controlWidth, Ui.ROW,
                    field, current);
                this.buttonList.add(slider);
                sliders.set(index, slider);
                inputs.add(null);
            } else {
                GuiTextField input = new GuiTextField(this.fontRendererObj, controlLeft + 1, y + 3, controlWidth - 2,
                    14);
                input.setMaxStringLength(8);
                input.setText(current == null ? "" : current);
                inputs.add(input);
            }
            y += FieldControls.stepAfter(field, nextField);
            if (FieldControls.standsApart(field)) {
                y += Ui.GAP;
            }
        }

        y += Ui.GAP;
        entriesSectionTop = y;
        y += 14;

        rowPositions.clear();
        rowNodes.clear();
        rowIndices.clear();

        for (int i = 0; i < workingChildren.size(); i++) {
            MenuNode child = workingChildren.get(i);
            if (child == null) {
                continue;
            }

            GuiButton move = new UiIconButton(ID_MOVE_BASE + i, rowButtonsLeft(), y, ARROW, Ui.ROW,
                UiIconButton.Icon.Right);
            GuiButton copy = new UiIconButton(ID_COPY_BASE + i, rowButtonsLeft() + ARROW + 2, y, ARROW, Ui.ROW,
                UiIconButton.Icon.Copy);
            GuiButton delete = new UiIconButton(ID_DELETE_BASE + i, rowButtonsLeft() + (ARROW + 2) * 2, y, ARROW,
                Ui.ROW, UiIconButton.Icon.Cross);
            GuiButton up = new UiIconButton(ID_UP_BASE + i, contentRight() - ARROW * 2 - 2, y, ARROW, Ui.ROW,
                UiIconButton.Icon.Up);
            GuiButton down = new UiIconButton(ID_DOWN_BASE + i, contentRight() - ARROW, y, ARROW, Ui.ROW,
                UiIconButton.Icon.Down);
            copy.enabled = !toolBelt && !child.isBuiltIn();
            delete.enabled = !toolBelt && !child.isBuiltIn();
            move.enabled = !toolBelt && !child.isBuiltIn();
            up.enabled = canMove(i, -1);
            down.enabled = canMove(i, 1);
            this.buttonList.add(move);
            this.buttonList.add(copy);
            this.buttonList.add(delete);
            this.buttonList.add(up);
            this.buttonList.add(down);
            // #tr radialmenu.menu.move.tip
            // # Move this entry into another submenu
            // # zh_CN 将此条目移动到另一个子菜单
            tooltip(ID_MOVE_BASE + i, describe("radialmenu.menu.move.tip"));
            // #tr radialmenu.menu.copy.tip
            // # Duplicate this entry
            // # zh_CN 复制此条目
            tooltip(ID_COPY_BASE + i, describe("radialmenu.menu.copy.tip"));
            // #tr radialmenu.menu.remove.tip
            // # Remove this entry. Changes take effect when saved
            // # zh_CN 移除此条目，保存后生效
            tooltip(ID_DELETE_BASE + i, describe("radialmenu.menu.remove.tip"));
            if (toolBelt || child.isBuiltIn()) {
                tooltip(ID_COPY_BASE + i, EditorRestrictions.builtInTip());
                tooltip(ID_DELETE_BASE + i, EditorRestrictions.builtInTip());
                tooltip(ID_MOVE_BASE + i, EditorRestrictions.builtInTip());
                if (toolBelt) {
                    tooltip(ID_UP_BASE + i, EditorRestrictions.builtInTip());
                    tooltip(ID_DOWN_BASE + i, EditorRestrictions.builtInTip());
                }
            }

            rowPositions.add(new int[] { left, y });
            rowNodes.add(child);
            rowIndices.add(Integer.valueOf(i));
            y += Ui.STEP;
        }

        // The queued moves are drawn under the list, so they are part of what scrolls - left out of the height
        // they would sit outside the clip and be cut off by the frame.
        y += pendingMoves.size() * 10;

        setContentHeight(y - scrolledTop() + Ui.PAD);
        addBottomBar("radialmenu.editor.save", null, "gui.cancel");
        if (toolBelt) tooltip(ID_PRIMARY, EditorRestrictions.resizeTip());
    }

    private int rowButtonsLeft() {
        return contentRight() - ARROW * 5 - 4 - GROUP_GAP;
    }

    /** Whether a move would change anything, so a button that cannot act is visibly disabled. */
    private boolean canMove(int index, int direction) {
        if (toolBelt) return false;
        List<MenuNode> probe = new ArrayList<>(workingChildren);
        return MenuNode.moveInList(probe, index, direction, isFixedLayout());
    }

    /** The layout as chosen on screen, which may not yet be the one saved on the node. */
    private boolean isFixedLayout() {
        return draft.getEnum(ActionTypes.PARAM_SLOT_MODE, SlotLayout.Mode.class, SlotLayout.Mode.Fixed)
            == SlotLayout.Mode.Fixed;
    }

    private static String accentStart() {
        int highlight = StyleResolver.resolve(ColorConfig.defaultColors(), ProfileManager.active().style,
            null).highlight;
        return String.format("#%06X", Integer.valueOf(highlight & 0x00FFFFFF));
    }

    private UiColorButton colorButton(int id, final ActionField field, int x, int y, int width) {
        return new UiColorButton(id, x, y, width, new UiColorButton.Value() {

            @Override
            public String get() {
                return draft.getString(field.key, "");
            }
        }, FieldControls.inheritedColor(field.key), I18n.format("radialmenu.editor.inherit"));
    }

    private static String describe(String key) {
        String translated = I18n.format(key);
        return translated.equals(key) ? null : translated;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (readOnly) {
            onCancel();
            return;
        }
        if (button.id == ID_PRIMARY) {
            save();
            return;
        }
        if (button.id == ID_SECONDARY) {
            onCancel();
            return;
        }
        if (button.id == ID_ICON && toolBelt) {
            capture();
            GuiStack.push(new GuiIconPicker(this, icon == null ? null : icon.color));
            return;
        }

        int clearIndex = button.id - ID_CLEAR_BASE;
        if (clearIndex >= 0 && clearIndex < fields.size()) {
            capture();
            draft.set(fields.get(clearIndex).key, "");
            requestRebuild();
            return;
        }

        int upIndex = button.id - ID_UP_BASE;
        if (!toolBelt && upIndex >= 0 && upIndex < workingChildren.size()) {
            capture();
            MenuNode.moveInList(workingChildren, upIndex, -1, isFixedLayout());
            requestRebuild();
            return;
        }
        int downIndex = button.id - ID_DOWN_BASE;
        if (!toolBelt && downIndex >= 0 && downIndex < workingChildren.size()) {
            capture();
            MenuNode.moveInList(workingChildren, downIndex, 1, isFixedLayout());
            requestRebuild();
            return;
        }

        int copyIndex = button.id - ID_COPY_BASE;
        if (!toolBelt && copyIndex >= 0 && copyIndex < workingChildren.size()) {
            capture();
            duplicate(copyIndex);
            requestRebuild();
            return;
        }
        int deleteIndex = button.id - ID_DELETE_BASE;
        if (!toolBelt && deleteIndex >= 0 && deleteIndex < workingChildren.size()) {
            capture();
            remove(deleteIndex);
            requestRebuild();
            return;
        }
        int moveIndex = button.id - ID_MOVE_BASE;
        if (!toolBelt && moveIndex >= 0 && moveIndex < workingChildren.size()) {
            capture();
            askWhereToMove(moveIndex);
            return;
        }

        int index = button.id - ID_FIELD_BASE;
        if (index < 0 || index >= fields.size()) {
            return;
        }
        final ActionField field = fields.get(index);
        String current = draft.getString(field.key, field.defaultValue);

        if (field.kind == ActionField.Kind.Color) {
            capture();
            GuiStack.push(new GuiColorPicker(current, true, new GuiColorPicker.Result() {

                @Override
                public void onColorPicked(String hex) {
                    draft.set(field.key, hex);
                    requestRebuild();
                }
            }));
            return;
        }

        if (field.kind == ActionField.Kind.Accent) {
            capture();
            GuiStack.push(new GuiColorPicker(accentStart(), false, new GuiColorPicker.Result() {

                @Override
                public void onColorPicked(String hex) {
                    FieldControls.applyAccent(draft, field, hex);
                    requestRebuild();
                }
            }));
            return;
        }

        if (field.kind == ActionField.Kind.Enum) {
            // Rebuilding throws away anything typed but not yet read back, so it is read back first.
            capture();
            int at = field.options.indexOf(current);
            String next = field.options.get((at + 1) % field.options.size());
            draft.set(field.key, next);
            requestRebuild();
        }
    }

    private void duplicate(int index) {
        MenuNode original = workingChildren.get(index);
        if (original == null || original.isBuiltIn()) {
            return;
        }
        MenuNode copy = original.copy();

        if (!isFixedLayout()) {
            workingChildren.add(index + 1, copy);
            return;
        }
        for (int i = index + 1; i < workingChildren.size(); i++) {
            if (workingChildren.get(i) == null) {
                workingChildren.set(i, copy);
                return;
            }
        }
        workingChildren.add(copy);
    }

    private void remove(int index) {
        MenuNode removed = workingChildren.get(index);
        if (removed != null && removed.isBuiltIn()) {
            return;
        }
        workingChildren.set(index, null);
        // An entry queued to move into a submenu that is itself being deleted would arrive nowhere.
        dropMovesInto(removed);
    }

    private void dropMovesInto(MenuNode gone) {
        if (gone == null || !gone.isCategory()) {
            return;
        }
        for (int i = pendingMoves.size() - 1; i >= 0; i--) {
            if (pendingMoves.get(i)[1] == gone) {
                pendingMoves.remove(i);
            }
        }
    }

    private void askWhereToMove(final int index) {
        final MenuNode moved = workingChildren.get(index);
        if (moved == null || moved.isBuiltIn()) {
            return;
        }
        GuiStack.push(new GuiMenuPicker(menu, moved, new GuiMenuPicker.Result() {

            @Override
            public void onMenuPicked(MenuNode target) {
                if (target == null || target == menu || target.isBuiltIn()) {
                    return;
                }
                workingChildren.set(index, null);
                pendingMoves.add(new MenuNode[] { moved, target });
                requestRebuild();
            }
        }));
    }

    private void capture() {
        if (titleField != null) {
            titleText = titleField.getText();
        }
        for (int i = 0; i < fields.size() && i < inputs.size(); i++) {
            GuiTextField input = inputs.get(i);
            if (input != null) {
                draft.set(fields.get(i).key, input.getText());
            }
            GuiSlider slider = sliders.get(i);
            if (slider != null) {
                draft.set(fields.get(i).key, Integer.toString(slider.getValueInt()));
            }
        }
    }

    private void save() {
        if (readOnly) {
            return;
        }
        capture();

        String title = titleText.trim();
        menu.title = title.isEmpty() ? null : title;

        if (toolBelt) {
            menu.icon = icon;
        } else {
            menu.children = new ArrayList<>(workingChildren);
        }

        for (MenuNode[] move : pendingMoves) {
            MenuNode node = move[0];
            MenuNode target = move[1];
            target.setChildAt(target.appendIndex(), node);
            target.ensureSlotCapacity();
        }

        // applyToNode reads the spec off the node, the same way the slot editor hands it over, and clears it
        // afterwards - a category carries no action at any depth.
        menu.action = draft;
        SubmenuFields.applyToNode(menu);
        if (toolBelt) {
            int requestedSlots = menu.layout.slots;
            ToolBeltMenu.requestResize(menu, requestedSlots);
        }

        ProfileManager.active().normalize();
        ProfileManager.saveActive();
        IconRenderer.clearCache();
        GuiStack.closeAll();
    }

    @Override
    public void onIconPicked(IconSpec icon) {
        this.icon = icon;
        requestRebuild();
    }

    @Override
    protected void onCancel() {
        GuiStack.closeAll();
    }

    @Override
    protected void beforeScroll() {
        capture();
    }

    @Override
    protected void drawContent(int mouseX, int mouseY, float partialTicks) {
        if (readOnly) {
            this.fontRendererObj.drawString(EditorRestrictions.builtInTip(), contentLeft(), contentTop(), Ui.TEXT);
            return;
        }
        int left = contentLeft();

        if (isVisibleRow(generalSectionTop)) {
            // #tr radialmenu.menu.sectionGeneral
            // # Appearance
            // # zh_CN 外观
            Ui.sectionHeader(I18n.format("radialmenu.menu.sectionGeneral"), left, generalSectionTop, contentRight());
        }

        int y = generalSectionTop + 14;

        if (isVisibleRow(y)) {
            // #tr radialmenu.menu.name
            // # Name
            // # zh_CN 名称
            Ui.rowLabel(I18n.format("radialmenu.menu.name"), left, y);
            titleField.drawTextBox();
        }
        y += Ui.STEP;

        if (toolBelt && isVisibleRow(iconRowTop)) {
            Ui.rowLabel(I18n.format("radialmenu.editor.icon"), left, iconRowTop);
            int iconLeft = left + Ui.LABEL_WIDTH + Ui.GAP;
            Ui.frame(iconLeft, iconRowTop, iconLeft + Ui.ROW, iconRowTop + Ui.ROW, 0xFF101010, Ui.PANEL_BORDER);
            if (icon != null) IconRenderer.draw(icon, iconLeft + 2, iconRowTop + 2);
        }

        for (int i = 0; i < fields.size() && i < fieldRowTops.size(); i++) {
            int rowTop = fieldRowTops.get(i).intValue();
            if (isVisibleRow(rowTop)) {
                Ui.rowLabel(Ui.fit(I18n.format(fields.get(i).labelKey), Ui.LABEL_WIDTH), left, rowTop);
            }
        }

        for (GuiTextField input : inputs) {
            if (input != null && isVisibleRow(input.yPosition - 3)) {
                input.drawTextBox();
            }
        }

        if (isVisibleRow(entriesSectionTop)) {
            // #tr radialmenu.menu.entries
            // # Entries
            // # zh_CN 条目
            Ui.sectionHeader(I18n.format("radialmenu.menu.entries"), left, entriesSectionTop, contentRight());
        }

        if (rowNodes.isEmpty() && isVisibleRow(entriesSectionTop + 14)) {
            this.fontRendererObj
                // #tr radialmenu.menu.noEntries
                // # This menu has no entries yet
                // # zh_CN 此菜单还没有任何条目
                .drawString(I18n.format("radialmenu.menu.noEntries"), left, entriesSectionTop + 18, Ui.TEXT_MUTED);
        }

        for (int i = 0; i < rowNodes.size(); i++) {
            int[] position = rowPositions.get(i);
            if (!isVisibleRow(position[1])) {
                continue;
            }
            MenuNode child = rowNodes.get(i);
            int iconLeft = position[0] + UiDragReorder.GRIP + 2;

            if (drag.isDragging() && drag.grabbedRow() == i) {
                // The row being carried, marked where it came from: without it, a drag of one row in a list of
                // identical-looking rows gives no clue what is moving.
                Gui.drawRect(position[0], position[1], contentRight(), position[1] + Ui.ROW, Ui.ROW_SELECTED);
            }
            if (!toolBelt) {
                UiDragReorder.drawGrip(position[0], position[1], Ui.ROW, drag.isDragging() && drag.grabbedRow() == i);
            }

            IconRenderer.draw(child.icon, iconLeft, position[1] + 2);

            String title = child.title == null || child.title.isEmpty() ? I18n.format("radialmenu.menu.untitled")
                : child.title;
            this.fontRendererObj.drawString(Ui.fit(title, rowButtonsLeft() - iconLeft - 24), iconLeft + 20,
                position[1] + (Ui.ROW - 8) / 2, Ui.TEXT);
        }

        drawPendingMoves();

        drag.drawInsertion(rows(), contentLeft(), contentRight());
        scrollTowardsDrag();
    }

    /** Says where entries have gone, for as long as they have not gone there yet. */
    private void drawPendingMoves() {
        if (pendingMoves.isEmpty()) {
            return;
        }
        int y = rowPositions.isEmpty() ? entriesSectionTop + 18
            : rowPositions.get(rowPositions.size() - 1)[1] + Ui.STEP;
        for (MenuNode[] move : pendingMoves) {
            if (!isVisibleRow(y)) {
                y += 10;
                continue;
            }
            this.fontRendererObj.drawString(
                // #tr radialmenu.menu.movePending
                // # %s moves to %s when you save
                // # zh_CN %s 将在保存时移动到 %s
                Ui.fit(I18n.format("radialmenu.menu.movePending", name(move[0]), name(move[1])), contentWidth()),
                contentLeft(), y + 2, Ui.TEXT_MUTED);
            y += 10;
        }
    }

    private String name(MenuNode node) {
        if (node == ProfileManager.active().root) {
            return ProfileManager.activeName();
        }
        return node.title == null || node.title.isEmpty() ? I18n.format("radialmenu.menu.untitled") : node.title;
    }

    private void scrollTowardsDrag() {
        if (!drag.isDragging()) {
            return;
        }
        int overshoot = drag.overshoot(rows());
        if (overshoot == 0) {
            return;
        }
        int step = Math.max(-6, Math.min(6, overshoot));
        int next = Math.max(0, Math.min(maxScroll(), scrollOffset + step));
        if (next != scrollOffset) {
            beforeScroll();
            scrollOffset = next;
            requestRebuild();
        }
    }

    private UiDragReorder.Rows rows() {
        return new UiDragReorder.Rows() {

            @Override
            public int count() {
                return rowPositions.size();
            }

            @Override
            public int top(int row) {
                return rowPositions.get(row)[1];
            }

            @Override
            public int height() {
                return Ui.ROW;
            }
        };
    }

    private void moveRow(int from, int to) {
        if (toolBelt || from < 0 || to < 0 || from == to || from >= rowIndices.size() || to >= rowIndices.size()) {
            return;
        }

        int direction = to > from ? 1 : -1;
        int index = rowIndices.get(from).intValue();
        int steps = 0;
        int cap = workingChildren.size() * 2;

        while (rowOf(index) != to && steps++ < cap) {
            if (!MenuNode.moveInList(workingChildren, index, direction, isFixedLayout())) {
                break;
            }
            index = workingChildren.indexOf(rowNodes.get(from));
        }
    }

    /** Which drawn row a list position is, counting only the entries that are drawn. */
    private int rowOf(int index) {
        int row = 0;
        for (int i = 0; i < index && i < workingChildren.size(); i++) {
            if (workingChildren.get(i) != null) {
                row++;
            }
        }
        return row;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        // Before super, and the whole point of the grip: a press that lands on one must not reach a button at all.
        if (!toolBelt && button == 0 && isInsideViewport(mouseY) && drag.press(mouseX, mouseY, contentLeft(), rows())) {
            return;
        }
        super.mouseClicked(mouseX, mouseY, button);
        // A field clipped at the panel edge still answers to clicks on the part that was cut away.
        if (readOnly || !isShowing() || !isInsideViewport(mouseY)) {
            return;
        }
        titleField.mouseClicked(mouseX, mouseY, button);
        for (GuiTextField input : inputs) {
            if (input != null) {
                input.mouseClicked(mouseX, mouseY, button);
            }
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
            if (to >= 0 && to != from) {
                capture();
                moveRow(from, to);
                requestRebuild();
            }
        }
        super.mouseMovedOrUp(mouseX, mouseY, which);
    }

    @Override
    protected boolean handleKey(char typedChar, int keyCode) {
        if (readOnly) {
            return false;
        }
        if (keyCode == Keyboard.KEY_ESCAPE && drag.isDragging()) {
            drag.cancel();
            return true;
        }
        if (titleField.textboxKeyTyped(typedChar, keyCode)) {
            return true;
        }
        for (GuiTextField input : inputs) {
            if (input != null && input.textboxKeyTyped(typedChar, keyCode)) {
                return true;
            }
        }
        return false;
    }
}
