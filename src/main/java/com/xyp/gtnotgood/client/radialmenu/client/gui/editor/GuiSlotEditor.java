package com.xyp.gtnotgood.client.radialmenu.client.gui.editor;

import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

import org.lwjgl.input.Keyboard;

import com.xyp.gtnotgood.client.radialmenu.client.gui.GuiStack;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.Ui;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.UiCheckbox;
import com.xyp.gtnotgood.client.radialmenu.client.icon.IconRenderer;
import com.xyp.gtnotgood.client.radialmenu.client.profile.ProfileManager;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionType;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionTypes;
import com.xyp.gtnotgood.client.radialmenu.core.model.IconSpec;
import com.xyp.gtnotgood.client.radialmenu.core.model.MenuNode;

public class GuiSlotEditor extends GuiActionEditor implements GuiIconPicker.Callback {

    private static final int ID_ICON = 1;
    private static final int ID_KEEP_OPEN = 2;
    private static final int ID_ENTRIES = 3;

    private final MenuNode parent;
    private final boolean readOnly;

    /** Position in the parent's child list, not the sector it is drawn in. */
    private final int childIndex;

    /** Working copy; the parent is only touched on save. */
    private final MenuNode draft;

    private GuiTextField titleField;

    /** Survives control rebuilds, which recreate the field. */
    private String titleText;

    private int actionSectionTop;

    private int iconPreviewLeft;
    private int iconPreviewTop;

    public GuiSlotEditor(MenuNode parent, int childIndex) {
        this.parent = parent;
        this.childIndex = childIndex;

        MenuNode existing = parent.childAtIndex(childIndex);
        this.readOnly = parent.isBuiltIn() || existing != null && existing.isBuiltIn();
        this.draft = existing != null ? existing.copy()
            : MenuNode.leaf(
                // #tr radialmenu.editor.newEntry
                // # New entry
                // # zh_CN 新建条目
                I18n.format("radialmenu.editor.newEntry"), IconSpec.item("minecraft:stone", 0),
                ActionTypes.get(ActionTypes.KEYBIND).newSpec());

        this.titleText = draft.title == null ? "" : draft.title;

        if (draft.isCategory()) {
            // Submenu settings live on the node, so mirror them into a spec the generic field code can drive.
            setSpec(SubmenuFields.toSpec(draft), ActionTypes.SUBMENU);
        } else if (draft.action != null && ActionTypes.isRegistered(draft.action.type)) {
            setSpec(draft.action, draft.action.type);
        } else {
            // A hand-edited file can name a type no build of the mod has. Showing an empty screen would be worse
            // than showing the default one, and the original is only overwritten if the player saves.
            setSpec(ActionTypes.get(ActionTypes.KEYBIND).newSpec(), ActionTypes.KEYBIND);
        }
    }

    @Override
    protected String titleKey() {
        // #tr radialmenu.editor.slot
        // # Slot %s
        // # zh_CN 槽位 %s
        return "radialmenu.editor.slot";
    }

    @Override
    protected Object[] titleArgs() {
        // Numbered by the sector, which is what the player clicked; on a dynamic wheel that is not the list position.
        return new Object[] { Integer.valueOf(parent.slotForChildIndex(childIndex) + 1) };
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
    protected List<ActionType> offeredTypes() {
        return ActionTypes.all();
    }

    @Override
    protected void buildControls() {
        if (readOnly) {
            setContentHeight(Ui.STEP);
            addBottomBar("gui.done", null, null);
            return;
        }
        int left = contentLeft();
        int controlLeft = left + Ui.LABEL_WIDTH + Ui.GAP;
        int controlWidth = contentRight() - controlLeft;
        int y = scrolledTop() + 14;

        // Built at its real size: GuiTextField works out its horizontal scroll from the width it has when the
        // text is set, so a placeholder-sized field resized afterwards renders blank until it is clicked.
        titleField = new GuiTextField(this.fontRendererObj, controlLeft + 1, y + 3, controlWidth - 2, 14);
        titleField.setMaxStringLength(64);
        titleField.setText(titleText);
        titleField.setCursorPositionZero();
        y += Ui.STEP;

        iconPreviewLeft = controlLeft;
        iconPreviewTop = y;
        this.buttonList.add(new GuiButton(ID_ICON, controlLeft + Ui.ROW + Ui.GAP, y, controlWidth - Ui.ROW - Ui.GAP,
            Ui.ROW, iconLabel()));
        // #tr radialmenu.editor.icon.tip
        // # Pick an item, a bundled sprite, or your own PNG.
        // # zh_CN 选择一个物品、内置精灵图或你自己的 PNG 文件。
        tooltip(ID_ICON, describe("radialmenu.editor.icon.tip"));
        y += Ui.STEP;

        this.buttonList.add(new UiCheckbox(ID_KEEP_OPEN, controlLeft, y, controlWidth,
            // #tr radialmenu.editor.keepOpen
            // # Keep menu open
            // # zh_CN 保持菜单打开
            I18n.format("radialmenu.editor.keepOpen"), draft.keepOpen));
        // #tr radialmenu.editor.keepOpen.tip
        // # Leave the wheel open after this entry fires, so it can be triggered again.
        // # zh_CN 执行此条目后保持转盘打开，以便再次触发。
        tooltip(ID_KEEP_OPEN, describe("radialmenu.editor.keepOpen.tip"));
        y += Ui.STEP + Ui.GAP;

        actionSectionTop = y;
        y += 14;

        y = buildActionSection(y, left, controlLeft, controlWidth);

        if (draft.isCategory() && ActionTypes.SUBMENU.equals(selectedType)) {
            y += Ui.GAP;
            this.buttonList.add(new GuiButton(ID_ENTRIES, controlLeft, y, controlWidth, Ui.ROW,
                // #tr radialmenu.editor.entries
                // # Entries (%d)...
                // # zh_CN 条目 (%d)...
                I18n.format("radialmenu.editor.entries", Integer.valueOf(draft.filledCount()))));
            // #tr radialmenu.editor.entries.tip
            // # Save this slot, then reorder, copy, delete or move its submenu entries.
            // # zh_CN 先保存此槽位，再对子菜单条目排序、复制、删除或移动。
            tooltip(ID_ENTRIES, describe("radialmenu.editor.entries.tip"));
            y += Ui.STEP;
        }

        setContentHeight(y - scrolledTop() + Ui.PAD);
        // #tr radialmenu.editor.delete
        // # Delete
        // # zh_CN 删除
        addBottomBar("radialmenu.editor.save", "radialmenu.editor.delete", "gui.cancel");
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (readOnly) {
            onCancel();
            return;
        }
        switch (button.id) {
            case ID_PRIMARY:
                save();
                return;
            case ID_SECONDARY:
                onCancel();
                return;
            case ID_DANGER:
                apply(null);
                return;
            case ID_ICON:
                captureInputs();
                GuiStack.push(new GuiIconPicker(this, draft.icon == null ? null : draft.icon.color));
                return;
            case ID_ENTRIES:
                openEntries();
                return;
            case ID_KEEP_OPEN:
                ((UiCheckbox) button).toggle();
                draft.keepOpen = ((UiCheckbox) button).checked;
                return;
            default:
                handleActionButton(button);
        }
    }

    /** Captures the title as well, so opening a picker or scrolling cannot discard it. */
    @Override
    protected void captureInputs() {
        if (titleField != null) {
            titleText = titleField.getText();
        }
        draft.title = titleText.trim();
        super.captureInputs();
    }

    private void save() {
        if (!dropsEntries()) {
            write();
            GuiStack.closeAll();
            return;
        }
        captureInputs();
        GuiStack.push(new GuiConfirm(
            // #tr radialmenu.editor.confirmDiscard
            // # Discard this submenu?
            // # zh_CN 丢弃此子菜单？
            "radialmenu.editor.confirmDiscard",
            // #tr radialmenu.editor.confirmDiscard.subject
            // # %d entries inside it will be lost
            // # zh_CN 其中的 %d 个条目将会丢失
            I18n.format("radialmenu.editor.confirmDiscard.subject", Integer.valueOf(draft.filledCount())),
            // #tr radialmenu.editor.discard
            // # Discard
            // # zh_CN 丢弃
            "radialmenu.editor.discard", new GuiConfirm.Result() {

                @Override
                public void onConfirmed() {
                    write();
                    GuiStack.closeAll();
                }
            }));
    }

    /** Whether saving would turn a submenu that still holds entries into something that cannot hold any. */
    private boolean dropsEntries() {
        return draft.isCategory() && draft.filledCount() > 0 && !ActionTypes.SUBMENU.equals(selectedType);
    }

    private MenuNode write() {
        captureInputs();

        draft.action = spec;
        draft.action.type = selectedType;

        if (ActionTypes.SUBMENU.equals(selectedType)) {
            SubmenuFields.applyToNode(draft);
        } else {
            draft.children = null;
            draft.layout = null;
            draft.style = null;
        }

        draft.normalize();
        store(draft);
        return draft;
    }

    private void openEntries() {
        if (!ActionTypes.SUBMENU.equals(selectedType)) {
            return;
        }
        MenuNode node = write();
        if (node == null || !node.isCategory()) {
            return;
        }
        GuiStack.push(new GuiMenuSettings(node));
    }

    private void apply(MenuNode node) {
        store(node);
        // Editing is finished, so the wheel closes with the editor instead of reappearing beneath it.
        GuiStack.closeAll();
    }

    /** Puts a node in this slot and saves the profile. Null is how an entry is deleted. */
    private void store(MenuNode node) {
        if (readOnly) {
            return;
        }
        parent.setChildAt(childIndex, node);
        ProfileManager.active().normalize();
        ProfileManager.saveActive();
        IconRenderer.clearCache();
    }

    @Override
    public void onIconPicked(IconSpec icon) {
        draft.icon = icon;
        requestRebuild();
    }

    @Override
    protected void onCancel() {
        GuiStack.closeAll();
    }

    @Override
    protected void drawContent(int mouseX, int mouseY, float partialTicks) {
        if (readOnly) {
            this.fontRendererObj.drawString(EditorRestrictions.builtInTip(), contentLeft(), contentTop(), Ui.TEXT);
            return;
        }
        int left = contentLeft();
        int y = scrolledTop();

        // #tr radialmenu.editor.sectionLook
        // # Appearance
        // # zh_CN 外观
        Ui.sectionHeader(I18n.format("radialmenu.editor.sectionLook"), left, y, contentRight());
        y += 14;

        if (isVisibleRow(y)) {
            // #tr radialmenu.editor.title
            // # Title
            // # zh_CN 标题
            Ui.rowLabel(I18n.format("radialmenu.editor.title"), left, y);
            titleField.drawTextBox();
        }
        y += Ui.STEP;

        if (isVisibleRow(y)) {
            // #tr radialmenu.editor.icon
            // # Icon
            // # zh_CN 图标
            Ui.rowLabel(I18n.format("radialmenu.editor.icon"), left, y);
        }

        if (isVisibleRow(actionSectionTop)) {
            // #tr radialmenu.editor.sectionAction
            // # Action
            // # zh_CN 操作
            Ui.sectionHeader(I18n.format("radialmenu.editor.sectionAction"), left, actionSectionTop, contentRight());
        }

        drawActionSection();
        drawIconPreview();
    }

    private String iconLabel() {
        return draft.icon == null || draft.icon.id == null ? I18n.format("radialmenu.editor.pickIcon")
            : Ui.fit(draft.icon.id, contentWidth() - Ui.LABEL_WIDTH - Ui.ROW - 16);
    }

    /** The preview tile, drawn with the content so it never has to fight a button for the GL state. */
    private void drawIconPreview() {
        if (!isVisibleRow(iconPreviewTop)) {
            return;
        }
        Ui.frame(iconPreviewLeft, iconPreviewTop, iconPreviewLeft + Ui.ROW, iconPreviewTop + Ui.ROW, 0xFF101010,
            Ui.PANEL_BORDER);
        if (draft.icon != null) {
            IconRenderer.draw(draft.icon, iconPreviewLeft + 2, iconPreviewTop + 2);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        // A field clipped at the panel edge still answers to clicks on the part that was cut away.
        if (readOnly || !isShowing() || !isInsideViewport(mouseY)) {
            return;
        }
        titleField.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean handleKey(char typedChar, int keyCode) {
        if (readOnly) {
            return false;
        }
        if (keyCode == Keyboard.KEY_TAB) {
            return true;
        }
        if (titleField.textboxKeyTyped(typedChar, keyCode)) {
            return true;
        }
        return super.handleKey(typedChar, keyCode);
    }
}
