package com.xyp.gtnotgood.client.radialmenu.client.gui.editor;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.input.Mouse;

import com.xyp.gtnotgood.client.radialmenu.client.gui.GuiStack;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.Ui;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.UiList;
import com.xyp.gtnotgood.client.radialmenu.client.gui.ui.UiScreen;
import com.xyp.gtnotgood.client.radialmenu.client.profile.ProfileManager;
import com.xyp.gtnotgood.client.radialmenu.client.profile.ProfileStorage;

public class GuiProfileManager extends UiScreen {

    private static final int ID_ACTIVATE = 1;
    private static final int ID_RULES = 2;
    private static final int ID_COLORS = 7;
    private static final int ID_NEW = 3;
    private static final int ID_RENAME = 4;
    private static final int ID_DUPLICATE = 5;
    private static final int ID_DELETE = 6;
    private static final int ID_BACKUPS = 8;

    private static final int BUTTON_COLUMN = 104;

    private final List<String> profiles = new ArrayList<>();

    private String selected;

    private UiList list;

    /** Kept across rebuilds, which make a fresh list, so selecting a row does not jump back to the top. */
    private int listScroll;

    /** Set when an operation fails, so a refused name or a failed delete is visible instead of silent. */
    private String errorKey;

    @Override
    protected String titleKey() {
        // #tr radialmenu.profiles.title
        // # RadialMenu profiles
        // # zh_CN RadialMenu 配置
        return "radialmenu.profiles.title";
    }

    @Override
    protected int panelWidth() {
        return 360;
    }

    /** A line for the error, reserved only while there is one - the list is worth more than a permanent blank. */
    @Override
    protected int footerHeight() {
        return errorKey == null ? 0 : 10;
    }

    @Override
    protected void buildControls() {
        profiles.clear();
        profiles.addAll(ProfileStorage.listProfileNames());
        if (selected == null || !profiles.contains(selected)) {
            selected = ProfileManager.activeName();
        }

        int listRight = viewportRight() - BUTTON_COLUMN - Ui.GAP;
        list = new UiList(viewportLeft(), viewportTop(), listRight, viewportBottom(), 13);
        list.scrollTo(listScroll, profiles.size());

        int x = listRight + Ui.GAP;
        int y = viewportTop();
        boolean has = profiles.contains(selected);
        boolean isActive = has && selected.equals(ProfileManager.activeName());

        y = addAction(ID_ACTIVATE,
            // #tr radialmenu.profiles.activate
            // # Activate
            // # zh_CN 启用
            "radialmenu.profiles.activate",
            // #tr radialmenu.profiles.activate.tip
            // # Use the selected profile
            // # zh_CN 启用选中的配置方案
            "radialmenu.profiles.activate.tip", x, y, has && !isActive);
        y = addAction(ID_RULES,
            // #tr radialmenu.profiles.rules
            // # Auto-bind rules
            // # zh_CN 自动绑定规则
            "radialmenu.profiles.rules",
            // #tr radialmenu.profiles.rules.tip
            // # Set when joining a server or world should activate this profile
            // # zh_CN 设置进入服务器或世界时自动启用此配置方案的条件
            "radialmenu.profiles.rules.tip", x, y, has);
        y = addAction(ID_COLORS,
            // #tr radialmenu.profiles.colors
            // # Colors
            // # zh_CN 颜色
            "radialmenu.profiles.colors",
            // #tr radialmenu.profiles.colors.tip
            // # Set ring and highlight colors for this profile
            // # zh_CN 设置此配置方案的轮盘和高亮颜色
            "radialmenu.profiles.colors.tip", x, y, has);
        y += Ui.GAP;
        y = addAction(ID_NEW,
            // #tr radialmenu.profiles.new
            // # New profile
            // # zh_CN 新建配置
            "radialmenu.profiles.new",
            // #tr radialmenu.profiles.new.tip
            // # Create an empty profile
            // # zh_CN 创建空白配置方案
            "radialmenu.profiles.new.tip", x, y, true);
        y = addAction(ID_RENAME,
            // #tr radialmenu.profiles.rename
            // # Rename
            // # zh_CN 重命名
            "radialmenu.profiles.rename",
            // #tr radialmenu.profiles.rename.tip
            // # Rename this profile and its file
            // # zh_CN 重命名此配置方案及其文件
            "radialmenu.profiles.rename.tip", x, y, has);
        y = addAction(ID_DUPLICATE,
            // #tr radialmenu.profiles.duplicate
            // # Duplicate
            // # zh_CN 复制
            "radialmenu.profiles.duplicate",
            // #tr radialmenu.profiles.duplicate.tip
            // # Copy this profile, including its rules and entries
            // # zh_CN 复制此配置方案，包含规则和全部条目
            "radialmenu.profiles.duplicate.tip", x, y, has);
        y = addAction(ID_BACKUPS,
            // #tr radialmenu.profiles.backups
            // # Backups...
            // # zh_CN 备份...
            "radialmenu.profiles.backups",
            // #tr radialmenu.profiles.backups.tip
            // # View versions backed up when changes were saved
            // # zh_CN 查看保存修改时备份的历史版本
            "radialmenu.profiles.backups.tip", x, y, has);
        y += Ui.GAP;
        addAction(ID_DELETE, "radialmenu.profiles.delete",
            // #tr radialmenu.profiles.delete.tip
            // # Delete this profile. If active, another profile is selected automatically
            // # zh_CN 删除此配置方案；若正在使用，则自动切换到其他方案
            "radialmenu.profiles.delete.tip", x, y, has);

        addBottomBar("gui.done", null, null);
    }

    private int addAction(int id, String labelKey, String tooltipKey, int x, int y, boolean enabled) {
        GuiButton button = new GuiButton(id, x, y, BUTTON_COLUMN, Ui.ROW, I18n.format(labelKey));
        button.enabled = enabled;
        this.buttonList.add(button);
        tooltip(id, describe(tooltipKey));
        return y + Ui.STEP;
    }

    private static String describe(String key) {
        String translated = I18n.format(key);
        return translated.equals(key) ? null : translated;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        errorKey = null;

        switch (button.id) {
            case ID_PRIMARY:
                GuiStack.closeAll();
                return;

            case ID_ACTIVATE:
                if (!ProfileManager.switchTo(selected)) {
                    // #tr radialmenu.profiles.error.load
                    // # Could not load that profile
                    // # zh_CN 无法加载该配置
                    errorKey = "radialmenu.profiles.error.load";
                }
                requestRebuild();
                return;

            case ID_RULES:
                GuiStack.push(new GuiAutoBindRules(selected));
                return;

            case ID_COLORS:
                GuiStack.push(new GuiProfileColors(selected));
                return;

            case ID_NEW:
                GuiStack.push(new GuiTextPrompt("radialmenu.profiles.new", "", new GuiTextPrompt.Result() {

                    @Override
                    public String onConfirm(String value) {
                        if (!ProfileManager.create(value)) {
                            // #tr radialmenu.profiles.error.exists
                            // # A profile with that name already exists
                            // # zh_CN 已存在同名配置
                            return "radialmenu.profiles.error.exists";
                        }
                        selected = ProfileStorage.sanitize(value);
                        return null;
                    }
                }));
                return;

            case ID_RENAME:
                final String renameFrom = selected;
                GuiStack.push(new GuiTextPrompt("radialmenu.profiles.rename", renameFrom, new GuiTextPrompt.Result() {

                    @Override
                    public String onConfirm(String value) {
                        if (!ProfileManager.rename(renameFrom, value)) {
                            return "radialmenu.profiles.error.exists";
                        }
                        selected = ProfileStorage.sanitize(value);
                        return null;
                    }
                }));
                return;

            case ID_DUPLICATE:
                final String copyFrom = selected;
                GuiStack.push(
                    new GuiTextPrompt("radialmenu.profiles.duplicate", copyFrom + "-copy", new GuiTextPrompt.Result() {

                        @Override
                        public String onConfirm(String value) {
                            if (!ProfileManager.duplicate(copyFrom, value)) {
                                return "radialmenu.profiles.error.exists";
                            }
                            selected = ProfileStorage.sanitize(value);
                            return null;
                        }
                    }));
                return;

            case ID_BACKUPS:
                GuiStack.push(new GuiProfileBackups(selected));
                return;

            case ID_DELETE:
                // #tr radialmenu.profiles.confirmDelete
                // # Delete this profile?
                // # zh_CN 删除此配置？
                GuiStack.push(new GuiConfirm("radialmenu.profiles.confirmDelete", selected, new GuiConfirm.Result() {

                    @Override
                    public void onConfirmed() {
                        if (!ProfileManager.delete(selected)) {
                            // #tr radialmenu.profiles.error.delete
                            // # Could not delete that profile
                            // # zh_CN 无法删除该配置
                            errorKey = "radialmenu.profiles.error.delete";
                        }
                        selected = null;
                    }
                }));
                return;

            default:
                break;
        }
    }

    @Override
    protected void drawContent(int mouseX, int mouseY, float partialTicks) {
        list.drawFrame();

        String activeName = ProfileManager.activeName();
        int hovered = list.itemAt(mouseX, mouseY, profiles.size());

        for (int row = 0; row < list.rowsVisible() && row + list.firstRow() < profiles.size(); row++) {
            int index = row + list.firstRow();
            String name = profiles.get(index);

            list.drawRowBackground(row, name.equals(selected), index == hovered);

            boolean active = name.equals(activeName);
            String label = Ui.fit(name, list.textWidth() - (active ? 42 : 0));
            this.fontRendererObj.drawString(label, list.textLeft(), list.rowTop(row) + 3,
                active ? Ui.TEXT_ACTIVE : Ui.TEXT);

            if (active) {
                // #tr radialmenu.profiles.activeMarker
                // # (active)
                // # zh_CN (当前)
                String marker = I18n.format("radialmenu.profiles.activeMarker");
                this.fontRendererObj.drawString(EnumChatFormatting.DARK_GREEN + marker,
                    list.right - 5 - this.fontRendererObj.getStringWidth(marker), list.rowTop(row) + 3, Ui.TEXT);
            }
        }

        list.drawScrollbar(profiles.size());

        if (profiles.isEmpty()) {
            this.drawCenteredString(this.fontRendererObj,
                // #tr radialmenu.profiles.empty
                // # No profiles yet
                // # zh_CN 暂无配置
                EnumChatFormatting.GRAY + I18n.format("radialmenu.profiles.empty"), (list.left + list.right) / 2,
                list.top + 8, Ui.TEXT);
        }

        if (errorKey != null) {
            this.drawCenteredString(this.fontRendererObj, EnumChatFormatting.RED + I18n.format(errorKey),
                this.width / 2, viewportBottom() + Ui.GAP, Ui.TEXT);
        }
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        list.scroll(Mouse.getEventDWheel(), profiles.size());
        listScroll = list.firstRow();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        if (button != 0) {
            return;
        }
        int index = list.itemAt(mouseX, mouseY, profiles.size());
        if (index >= 0) {
            selected = profiles.get(index);
            errorKey = null;
            requestRebuild();
        }
    }

    @Override
    protected void onCancel() {
        GuiStack.closeAll();
    }
}
