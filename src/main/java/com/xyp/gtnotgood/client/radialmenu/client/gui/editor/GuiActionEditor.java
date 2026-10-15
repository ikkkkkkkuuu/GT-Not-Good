package com.xyp.gtnotgood.client.radialmenu.client.gui.editor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

import org.lwjgl.input.Mouse;

import com.xyp.gtnotgood.client.radialmenu.client.gui.GuiStack;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.Ui;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.UiCheckbox;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.UiColorButton;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.UiDragReorder;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.UiIconButton;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.UiScreen;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.UiTabButton;
import com.xyp.gtnotgood.client.radialmenu.client.profile.ProfileManager;
import com.xyp.gtnotgood.client.radialmenu.client.script.ScriptHost;
import com.xyp.gtnotgood.client.radialmenu.config.ColorConfig;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionField;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSpec;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSteps;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionType;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionTypes;
import com.xyp.gtnotgood.client.radialmenu.core.model.StyleResolver;

import cpw.mods.fml.client.config.GuiSlider;

public abstract class GuiActionEditor extends UiScreen implements GuiKeyBindPicker.Callback {

    protected static final int ID_TAB_BASE = 10;
    protected static final int ID_FIELD_BASE = 40;

    private static final int ID_CLEAR_BASE = 200;

    /** Step controls are numbered far apart, so a long chain cannot reach into the next range. */
    private static final int ID_STEP_ADD = 300;
    private static final int ID_STEP_EDIT_BASE = 1000;
    private static final int ID_STEP_UP_BASE = 2000;
    private static final int ID_STEP_DOWN_BASE = 3000;
    private static final int ID_STEP_REMOVE_BASE = 4000;

    private static final int ARROW = 18;

    private static final int TAB_PADDING = 6;
    private static final int TAB_GAP = 2;
    private static final int TAB_ARROW = 12;

    private static final int ID_TAB_PREV = 8;
    private static final int ID_TAB_NEXT = 9;

    /** First tab shown, when there are more than fit. Kept across rebuilds, so typing does not move the strip. */
    private int tabScroll;

    private int tabVisible;
    private int tabCount;

    private int tabRowTop = -1;

    protected ActionSpec spec;

    protected String selectedType;

    private final Map<String, ActionSpec> specByType = new LinkedHashMap<>();

    private final List<ActionField> editableFields = new ArrayList<>();
    private final List<GuiTextField> fieldInputs = new ArrayList<>();

    /** Parallel to the fields, like the text boxes: a slider holds its value in the widget rather than in the spec. */
    private final List<GuiSlider> fieldSliders = new ArrayList<>();

    /** Row tops recorded during the build, so drawing never replays the layout arithmetic and disagrees with it. */
    private final List<Integer> fieldRowTops = new ArrayList<>();

    private int stepsSectionTop = -1;
    private int stepCount;

    private int stepRowTop;

    private final UiDragReorder stepDrag = new UiDragReorder();

    protected abstract List<ActionType> offeredTypes();

    protected List<ActionField> offeredFields(ActionType type) {
        return type.fields;
    }

    protected String fieldTooltip(ActionField field) {
        return describe(field.tooltipKey());
    }

    /** Sets the action to edit. Call from the subclass constructor, before the screen is shown. */
    protected void setSpec(ActionSpec spec, String typeId) {
        this.spec = spec;
        this.selectedType = typeId;
        specByType.put(typeId, spec);
    }

    // -- building ------------------------------------------------------------------------------------------------

    /**
     * Builds the tabs, the selected type's fields and, for a chain, its steps. @return the y below everything it added
     */
    protected int buildActionSection(int y, int left, int controlLeft, int controlWidth) {
        editableFields.clear();
        fieldInputs.clear();
        fieldSliders.clear();
        fieldRowTops.clear();
        stepsSectionTop = -1;
        stepCount = 0;

        y = buildTabs(offeredTypes(), y, left);

        ActionType selected = ActionTypes.get(selectedType);
        if (selected == null) {
            return y;
        }

        List<ActionField> fields = offeredFields(selected);
        for (int i = 0; i < fields.size(); i++) {
            ActionField field = fields.get(i);
            // A field that has nothing to say about this action is not built at all, so it takes no row, no
            // position in the parallel lists, and no space in the panel's height.
            if (!FieldControls.isRelevant(field, spec)) {
                continue;
            }
            ActionField next = i + 1 < fields.size() ? fields.get(i + 1) : null;
            if (FieldControls.standsApart(field)) {
                y += Ui.GAP;
            }
            addFieldControl(field, controlLeft, controlWidth, y);
            y += FieldControls.stepAfter(field, next);
            if (FieldControls.standsApart(field)) {
                y += Ui.GAP;
            }
        }

        if (selected.chain) {
            y += Ui.GAP;
            stepsSectionTop = y;
            y += 14;
            y = buildStepRows(y, left);
        }
        return y;
    }

    /** One row of tabs, each as wide as its own label, scrolled sideways when they no longer fit. */
    private int buildTabs(List<ActionType> types, int y, int left) {
        tabRowTop = y;
        tabCount = types.size();

        int[] widths = new int[types.size()];
        int natural = 0;
        for (int i = 0; i < types.size(); i++) {
            widths[i] = this.fontRendererObj.getStringWidth(I18n.format(types.get(i).labelKey)) + TAB_PADDING * 2;
            natural += widths[i] + (i > 0 ? TAB_GAP : 0);
        }

        int available = contentWidth();
        if (natural <= available) {
            tabScroll = 0;
            tabVisible = types.size();
            layoutTabs(types, widths, 0, types.size(), left, y, available - natural);
            return y + UiTabButton.HEIGHT + Ui.GAP;
        }

        int strip = available - 2 * (TAB_ARROW + TAB_GAP);
        tabScroll = Math.max(0, Math.min(tabScroll, types.size() - 1));

        // The selected tab is the one the player is looking at; scrolling it out of sight would leave the row showing
        // six types and no sign of which is in use.
        int selected = indexOfType(types, selectedType);
        if (selected >= 0) {
            if (selected < tabScroll) {
                tabScroll = selected;
            }
            while (selected >= tabScroll + fittingTabs(widths, tabScroll, strip) && tabScroll < types.size() - 1) {
                tabScroll++;
            }
        }

        tabVisible = fittingTabs(widths, tabScroll, strip);
        int tabsLeft = left + TAB_ARROW + TAB_GAP;

        addArrow(ID_TAB_PREV, left, y, "<", tabScroll > 0);
        layoutTabs(types, widths, tabScroll, tabVisible, tabsLeft, y, 0);
        addArrow(ID_TAB_NEXT, contentRight() - TAB_ARROW, y, ">", tabScroll + tabVisible < types.size());

        return y + UiTabButton.HEIGHT + Ui.GAP;
    }

    private void layoutTabs(List<ActionType> types, int[] widths, int from, int count, int left, int y, int extra) {
        int x = left;
        for (int i = 0; i < count; i++) {
            int index = from + i;
            ActionType type = types.get(index);
            int width = widths[index] + (count > 0 ? extra / count + (i < extra % count ? 1 : 0) : 0);
            this.buttonList.add(new UiTabButton(ID_TAB_BASE + index, x, y, width, I18n.format(type.labelKey),
                type.id.equals(selectedType)));
            tooltip(ID_TAB_BASE + index, describe(type.tooltipKey()));
            x += width + TAB_GAP;
        }
    }

    private static int fittingTabs(int[] widths, int from, int strip) {
        int count = 0;
        int total = 0;
        while (from + count < widths.length) {
            int next = total + widths[from + count] + (count > 0 ? TAB_GAP : 0);
            if (count > 0 && next > strip) {
                break;
            }
            total = next;
            count++;
        }
        return Math.max(1, count);
    }

    private void addArrow(int id, int x, int y, String label, boolean enabled) {
        UiTabButton arrow = new UiTabButton(id, x, y, TAB_ARROW, label, false);
        arrow.enabled = enabled;
        this.buttonList.add(arrow);
    }

    private static int indexOfType(List<ActionType> types, String id) {
        for (int i = 0; i < types.size(); i++) {
            if (types.get(i).id.equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private void scrollTabs(int by) {
        int next = Math.max(0, Math.min(tabScroll + by, Math.max(0, tabCount - 1)));
        if (next != tabScroll) {
            captureInputs();
            tabScroll = next;
            requestRebuild();
        }
    }

    private int buildStepRows(int y, int left) {
        List<ActionSpec> steps = spec.stepsOrEmpty();
        stepCount = steps.size();

        int removeLeft = contentRight() - ARROW;
        int downLeft = removeLeft - ARROW - 2;
        int upLeft = downLeft - ARROW - 2;
        int rowWidth = upLeft - Ui.GAP - left;

        stepRowTop = y;

        for (int i = 0; i < steps.size(); i++) {
            String label = (i + 1) + ". " + ActionSummary.of(steps.get(i));
            this.buttonList.add(new GuiButton(ID_STEP_EDIT_BASE + i, left + UiDragReorder.GRIP + 2, y,
                rowWidth - UiDragReorder.GRIP - 2, Ui.ROW, Ui.fit(label, rowWidth - UiDragReorder.GRIP - 10)));
            // #tr radialmenu.steps.edit.tip
            // # Open this step. The arrows move it, x removes it.
            // # zh_CN 打开此步骤。箭头可以移动它，X 可以删除它。
            tooltip(ID_STEP_EDIT_BASE + i, describe("radialmenu.steps.edit.tip"));

            GuiButton up = new UiIconButton(ID_STEP_UP_BASE + i, upLeft, y, ARROW, Ui.ROW, UiIconButton.Icon.Up);
            GuiButton down = new UiIconButton(ID_STEP_DOWN_BASE + i, downLeft, y, ARROW, Ui.ROW,
                UiIconButton.Icon.Down);
            up.enabled = i > 0;
            down.enabled = i < steps.size() - 1;
            this.buttonList.add(up);
            this.buttonList.add(down);
            this.buttonList
                .add(new UiIconButton(ID_STEP_REMOVE_BASE + i, removeLeft, y, ARROW, Ui.ROW, UiIconButton.Icon.Cross));

            y += Ui.STEP;
        }

        this.buttonList
            // #tr radialmenu.steps.add
            // # Add step
            // # zh_CN 添加步骤
            .add(new GuiButton(ID_STEP_ADD, left, y, contentWidth(), Ui.ROW, I18n.format("radialmenu.steps.add")));
        // #tr radialmenu.steps.add.tip
        // # Append a step to the end of the chain.
        // # zh_CN 在操作链末尾添加一个步骤。
        tooltip(ID_STEP_ADD, describe("radialmenu.steps.add.tip"));
        return y + Ui.STEP;
    }

    private void addFieldControl(ActionField field, int controlLeft, int controlWidth, int y) {
        int index = editableFields.size();
        editableFields.add(field);
        fieldRowTops.add(Integer.valueOf(y));
        tooltip(ID_FIELD_BASE + index, fieldTooltip(field));

        String current = spec.getString(field.key, field.defaultValue);
        fieldSliders.add(null);

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
            fieldInputs.add(null);

            if (FieldControls.hasClear(field)) {
                GuiButton clear = new UiIconButton(ID_CLEAR_BASE + index, controlLeft + mainWidth + Ui.GAP, y,
                    FieldControls.CLEAR_WIDTH, Ui.ROW, UiIconButton.Icon.Cross);
                clear.enabled = current != null && !current.trim().isEmpty();
                this.buttonList.add(clear);
                // #tr radialmenu.profileColors.clear.tip
                // # Clear this one color, so it inherits again.
                // # zh_CN 清除该颜色设置，使其重新继承。
                tooltip(ID_CLEAR_BASE + index, describe("radialmenu.profileColors.clear.tip"));
            }
            if (!editable) {
                tooltip(ID_FIELD_BASE + index, FieldControls.colorOffTip(field.key));
            }
            return;
        }

        if (field.kind == ActionField.Kind.Boolean) {
            this.buttonList.add(new UiCheckbox(ID_FIELD_BASE + index, controlLeft, y, controlWidth,
                I18n.format(field.labelKey), Boolean.parseBoolean(current)));
            fieldInputs.add(null);
            return;
        }

        if (field.hasRange()) {
            GuiSlider slider = FieldControls.slider(ID_FIELD_BASE + index, controlLeft, y, controlWidth, Ui.ROW, field,
                current);
            this.buttonList.add(slider);
            fieldSliders.set(index, slider);
            fieldInputs.add(null);
            return;
        }

        GuiTextField input = new GuiTextField(this.fontRendererObj, controlLeft + 1, y + 3, controlWidth - 2, 14);
        input.setMaxStringLength(256);
        input.setText(current == null ? "" : current);
        fieldInputs.add(input);
    }

    // -- input ---------------------------------------------------------------------------------------------------

    @Override
    public void handleMouseInput() {
        if (tabRowTop >= 0 && tabCount > tabVisible) {
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            if (mouseY >= tabRowTop && mouseY < tabRowTop + UiTabButton.HEIGHT) {
                int wheel = Mouse.getEventDWheel();
                if (wheel != 0) {
                    scrollTabs(wheel > 0 ? -1 : 1);
                    return;
                }
            }
        }
        super.handleMouseInput();
    }

    /** @return whether the button belonged to the action section */
    protected boolean handleActionButton(GuiButton button) {
        if (button.id >= ID_STEP_EDIT_BASE) {
            return handleStepButton(button.id);
        }
        if (button.id == ID_STEP_ADD) {
            captureInputs();
            editStep(spec.stepsOrEmpty().size(), null);
            return true;
        }

        if (button.id == ID_TAB_PREV) {
            scrollTabs(-1);
            return true;
        }
        if (button.id == ID_TAB_NEXT) {
            scrollTabs(1);
            return true;
        }

        List<ActionType> types = offeredTypes();
        int tabIndex = button.id - ID_TAB_BASE;
        if (tabIndex >= 0 && tabIndex < types.size()) {
            selectType(types.get(tabIndex).id);
            return true;
        }

        int clearIndex = button.id - ID_CLEAR_BASE;
        if (clearIndex >= 0 && clearIndex < editableFields.size()) {
            captureInputs();
            spec.set(editableFields.get(clearIndex).key, "");
            requestRebuild();
            return true;
        }

        int fieldIndex = button.id - ID_FIELD_BASE;
        if (fieldIndex >= 0 && fieldIndex < editableFields.size()) {
            onFieldButton(editableFields.get(fieldIndex), button);
            return true;
        }
        return false;
    }

    private boolean handleStepButton(int id) {
        List<ActionSpec> steps = spec.stepsOrEmpty();

        if (id >= ID_STEP_REMOVE_BASE) {
            int index = id - ID_STEP_REMOVE_BASE;
            if (index < steps.size()) {
                captureInputs();
                steps.remove(index);
                requestRebuild();
            }
            return true;
        }
        if (id >= ID_STEP_DOWN_BASE) {
            moveStep(id - ID_STEP_DOWN_BASE, 1);
            return true;
        }
        if (id >= ID_STEP_UP_BASE) {
            moveStep(id - ID_STEP_UP_BASE, -1);
            return true;
        }

        int index = id - ID_STEP_EDIT_BASE;
        if (index < steps.size()) {
            captureInputs();
            editStep(index, steps.get(index));
        }
        return true;
    }

    private void moveStep(int index, int direction) {
        captureInputs();
        if (ActionSteps.move(spec.stepsOrEmpty(), index, direction)) {
            requestRebuild();
        }
    }

    private void editStep(final int index, ActionSpec step) {
        GuiStack.push(new GuiStepEditor(step, index + 1, new GuiStepEditor.Result() {

            @Override
            public void onStepEdited(ActionSpec edited) {
                List<ActionSpec> steps = spec.stepsOrEmpty();
                if (index < steps.size()) {
                    steps.set(index, edited);
                } else {
                    steps.add(edited);
                }
                requestRebuild();
            }
        }));
    }

    private void selectType(String typeId) {
        if (typeId.equals(selectedType)) {
            return;
        }
        captureInputs();
        specByType.put(selectedType, spec);
        selectedType = typeId;
        spec = specFor(typeId);
        requestRebuild();
    }

    protected ActionSpec specFor(String typeId) {
        ActionSpec remembered = specByType.get(typeId);
        if (remembered != null) {
            return remembered;
        }
        ActionSpec fresh = ActionTypes.get(typeId).newSpec();
        FieldControls.applyClientDefaults(fresh);
        return fresh;
    }

    private UiColorButton colorButton(int id, final ActionField field, int x, int y, int width) {
        return new UiColorButton(id, x, y, width, new UiColorButton.Value() {

            @Override
            public String get() {
                return spec.getString(field.key, "");
            }
        }, FieldControls.inheritedColor(field.key), I18n.format("radialmenu.editor.inherit"));
    }

    private String accentStart() {
        return String.format("#%06X",
            Integer.valueOf(
                StyleResolver.resolve(ColorConfig.defaultColors(), ProfileManager.active().style, null).highlight
                    & 0x00FFFFFF));
    }

    private void onFieldButton(final ActionField field, GuiButton button) {
        final String key = field.key;
        String current = spec.getString(key, field.defaultValue);

        switch (field.kind) {
            case KeybindRef:
                captureInputs();
                GuiStack.push(new GuiKeyBindPicker(this));
                return;
            case MultilineString:
                captureInputs();
                GuiStack.push(new GuiMultilineEditor(field.labelKey, current, new GuiMultilineEditor.Result() {

                    @Override
                    public void onLinesEdited(String text) {
                        spec.set(key, text);
                        requestRebuild();
                    }
                }));
                return;
            case Code:
                captureInputs();
                GuiStack.push(new GuiScriptEditor(
                    // #tr radialmenu.script.title
                    // # Script
                    // # zh_CN 脚本
                    "radialmenu.script.title", current, ScriptHost.lastError(spec), new GuiScriptEditor.Result() {

                        @Override
                        public void onScriptEdited(String text) {
                            spec.set(key, text);
                            requestRebuild();
                        }
                    }));
                return;
            case Color:
                captureInputs();
                GuiStack.push(new GuiColorPicker(current, true, new GuiColorPicker.Result() {

                    @Override
                    public void onColorPicked(String hex) {
                        spec.set(key, hex);
                        requestRebuild();
                    }
                }));
                return;
            case Accent:
                captureInputs();
                // Nothing to open it on: an accent is not stored, so every pick starts from the wheel's own colour.
                GuiStack.push(new GuiColorPicker(accentStart(), false, new GuiColorPicker.Result() {

                    @Override
                    public void onColorPicked(String hex) {
                        FieldControls.applyAccent(spec, field, hex);
                        requestRebuild();
                    }
                }));
                return;
            case Boolean:
                ((UiCheckbox) button).toggle();
                spec.set(key, Boolean.toString(((UiCheckbox) button).checked));
                return;
            case Enum:
                captureInputs();
                spec.set(key, next(field.options, current));
                requestRebuild();
                return;
            case ProfileRef:
                spec.set(key, next(FieldControls.profileOptions(), current));
                break;
            default:
                return;
        }
        button.displayString = FieldControls.buttonLabel(field, spec.getString(key, ""));
    }

    @Override
    public void onKeyBindPicked(String description, String category) {
        spec.set(ActionTypes.PARAM_BINDING, description);
        spec.set(ActionTypes.PARAM_CATEGORY, category == null ? "" : category);
        requestRebuild();
    }

    protected void captureInputs() {
        for (int i = 0; i < editableFields.size() && i < fieldInputs.size(); i++) {
            GuiTextField input = fieldInputs.get(i);
            if (input != null) {
                spec.set(editableFields.get(i).key, input.getText());
            }
            GuiSlider slider = fieldSliders.get(i);
            if (slider != null) {
                spec.set(editableFields.get(i).key, Integer.toString(slider.getValueInt()));
            }
        }
    }

    @Override
    protected void beforeScroll() {
        captureInputs();
    }

    // -- drawing -------------------------------------------------------------------------------------------------

    protected void drawActionSection() {
        int left = contentLeft();

        for (int i = 0; i < editableFields.size() && i < fieldRowTops.size(); i++) {
            ActionField field = editableFields.get(i);
            int y = fieldRowTops.get(i).intValue();
            if (field.kind != ActionField.Kind.Boolean && isVisibleRow(y)) {
                Ui.rowLabel(Ui.fit(I18n.format(field.labelKey), Ui.LABEL_WIDTH), left, y);
            }
        }

        for (GuiTextField input : fieldInputs) {
            if (input != null && isVisibleRow(input.yPosition - 3)) {
                input.drawTextBox();
            }
        }

        if (stepsSectionTop < 0) {
            return;
        }
        if (isVisibleRow(stepsSectionTop)) {
            // #tr radialmenu.steps.section
            // # Steps
            // # zh_CN 步骤
            Ui.sectionHeader(I18n.format("radialmenu.steps.section"), left, stepsSectionTop, contentRight());
        }
        if (stepCount == 0 && isVisibleRow(stepsSectionTop + 14)) {
            this.fontRendererObj
                // #tr radialmenu.steps.empty
                // # This chain has no steps yet
                // # zh_CN 此操作链还没有步骤
                .drawString(I18n.format("radialmenu.steps.empty"), left, stepsSectionTop + 18, Ui.TEXT_MUTED);
        }

        for (int i = 0; i < stepCount; i++) {
            int top = stepRows().top(i);
            if (!isVisibleRow(top)) {
                continue;
            }
            if (stepDrag.isDragging() && stepDrag.grabbedRow() == i) {
                Gui.drawRect(left, top, contentRight(), top + Ui.ROW, Ui.ROW_SELECTED);
            }
            UiDragReorder.drawGrip(left, top, Ui.ROW, stepDrag.isDragging() && stepDrag.grabbedRow() == i);
        }
        stepDrag.drawInsertion(stepRows(), left, contentRight());
        scrollTowardsDrag();
    }

    private UiDragReorder.Rows stepRows() {
        return new UiDragReorder.Rows() {

            @Override
            public int count() {
                return stepCount;
            }

            @Override
            public int top(int row) {
                return stepRowTop + row * Ui.STEP;
            }

            @Override
            public int height() {
                return Ui.ROW;
            }
        };
    }

    private void scrollTowardsDrag() {
        if (!stepDrag.isDragging()) {
            return;
        }
        int overshoot = stepDrag.overshoot(stepRows());
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

    private void dropStep(int from, int to) {
        List<ActionSpec> steps = spec.stepsOrEmpty();
        if (from < 0 || to < 0 || from == to || from >= steps.size() || to >= steps.size()) {
            return;
        }
        int direction = to > from ? 1 : -1;
        for (int at = from; at != to; at += direction) {
            if (!ActionSteps.move(steps, at, direction)) {
                break;
            }
        }
    }

    /** @return the translated text, or null when the key has no string - so a missing tooltip shows nothing */
    protected static String describe(String key) {
        String translated = I18n.format(key);
        return translated.equals(key) ? null : translated;
    }

    private static String next(List<String> options, String current) {
        if (options.isEmpty()) {
            return current == null ? "" : current;
        }
        int index = options.indexOf(current);
        return options.get((index + 1) % options.size());
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        // Before super: a press on a grip must not reach the row button underneath, which opens the step.
        if (button == 0 && isInsideViewport(mouseY) && stepDrag.press(mouseX, mouseY, contentLeft(), stepRows())) {
            return;
        }
        super.mouseClicked(mouseX, mouseY, button);
        // A field clipped at the panel edge still answers to clicks on the part that was cut away.
        if (!isShowing() || !isInsideViewport(mouseY)) {
            return;
        }
        for (GuiTextField input : fieldInputs) {
            if (input != null) {
                input.mouseClicked(mouseX, mouseY, button);
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int mouseButton, long timeSinceClick) {
        stepDrag.moveTo(mouseY);
        super.mouseClickMove(mouseX, mouseY, mouseButton, timeSinceClick);
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int which) {
        if (which == 0) {
            int from = stepDrag.grabbedRow();
            int to = stepDrag.release(stepRows());
            if (to >= 0 && to != from) {
                captureInputs();
                dropStep(from, to);
                requestRebuild();
            }
        }
        super.mouseMovedOrUp(mouseX, mouseY, which);
    }

    @Override
    protected boolean handleKey(char typedChar, int keyCode) {
        for (GuiTextField input : fieldInputs) {
            if (input != null && input.textboxKeyTyped(typedChar, keyCode)) {
                return true;
            }
        }
        return false;
    }
}
