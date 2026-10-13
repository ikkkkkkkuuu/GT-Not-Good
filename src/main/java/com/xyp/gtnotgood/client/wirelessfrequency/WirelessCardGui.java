// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure PR #627 at 651396c75d108e6ff5399a44c56c12214ad27d3d.

package com.xyp.gtnotgood.client.wirelessfrequency;

import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.APPLY_FILTER;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.BACK;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.CYCLE_FILTER;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.CYCLE_VISUALISATION;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.DELETE_SOURCE;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.OPEN_DETAILS;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.OPEN_SOURCES;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.RENAME_SOURCE;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.SELECT_SOURCE;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.TOGGLE_AUTO;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.TOGGLE_GROUPING;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.rowIndex;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.type;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardContainer;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardLayout;

import appeng.core.localization.ColorUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class WirelessCardGui extends WirelessCardPanel {

    private final TextFieldWidget search, dimension;
    private final IPanelHandler detailsPanel, groupPanel, sourceRenamePanel;
    private long renameSourceId;
    private boolean showingDetails;
    private String searchText = "", dimensionText = "";

    public WirelessCardGui(WirelessCardContainer container) {
        super("wireless_card", container);
        search = textField(64).hintText(tr("search_placeholder")).hintColor(ColorUtils.craftingTreeRequest.getColor());
        dimension = textField(12);
        search.autoUpdateOnChange(true).value(new StringValue.Dynamic(() -> searchText, text -> {
            if (searchText.equals(text)) return;
            searchText = text;
            applyFilters(false, false);
        }));
        dimension.autoUpdateOnChange(true).value(new StringValue.Dynamic(() -> dimensionText, text -> {
            if (dimensionText.equals(text)) return;
            dimensionText = text;
            applyFilters(false, false);
        }));
        child(search).child(dimension);
        groupPanel = IPanelHandler.simple(this, (parent, player) -> new WirelessCardGroupPanel(container), true);
        detailsPanel = IPanelHandler.simple(this, (parent, player) -> new WirelessCardDetailsPanel(container), true);
        sourceRenamePanel = IPanelHandler.simple(this,
            (parent, player) -> new WirelessCardRenameDialog(this, () -> renameSourceId), true);
        for (int id : new int[] { TOGGLE_AUTO, CYCLE_FILTER, CYCLE_VISUALISATION, TOGGLE_GROUPING, OPEN_SOURCES })
            addButton(id);
        addRowButtons(OPEN_DETAILS, RENAME_SOURCE, DELETE_SOURCE);
    }

    @Override
    boolean isDetailView() {
        return false;
    }

    @Override
    int rowTop() {
        return 82;
    }

    @Override
    void confirm() {
        action(APPLY_FILTER);
    }

    @Override
    boolean editingText() {
        return search.isFocused() || dimension.isFocused();
    }

    public ModularScreen screen() {
        return new ModularScreen(GTNotGood.MODID, this) {

            @Override
            public void onResize(int width, int height) {
                resizeLayout(width, height);
                for (ModularPanel panel : getPanelManager().getOpenPanels()) {
                    if (panel instanceof WirelessCardPanel cardPanel && panel != WirelessCardGui.this)
                        cardPanel.resizeLayout(width, height);
                }
                super.onResize(width, height);
            }

            @Override
            public boolean onMousePressed(int button) {
                ModularPanel previous = getPanelManager().getTopMostPanel();
                ModularPanel hovered = hoveredPanel();
                if (button == 1) {
                    if (hovered == null) return true;
                    if (hovered.getTopHovering() instanceof TextFieldWidget) super.onMousePressed(button);
                    else if (hovered instanceof WirelessCardRenameDialog dialog) dialog.closeIfOpen();
                    else if (
                        hovered instanceof WirelessCardPanel panel
                            && (panel.isDetailView() || panel.view().getBoolean("group")
                                || panel.view().getBoolean("sourcesPage"))
                    ) panel.action(BACK);
                    return true;
                }
                if (
                    hovered instanceof WirelessCardPanel panel && button > 1
                        && WirelessCardInput.visualisationKey(button - 100)
                        && !(getContext().getFocusedWidget().getElement() instanceof TextFieldWidget)
                ) {
                    panel.action(CYCLE_VISUALISATION);
                    return true;
                }
                boolean handled = super.onMousePressed(button);
                // The opening button clears focus after its callback returns.
                if (
                    getPanelManager().getTopMostPanel() instanceof WirelessCardRenameDialog dialog && previous != dialog
                ) getContext().focus(dialog.name);
                return handled;
            }

            private ModularPanel hoveredPanel() {
                for (ModularPanel panel : getPanelManager().getOpenPanels()) {
                    if (panel.isBelowMouse()) return panel;
                    if (panel.disablePanelsBelow()) break;
                }
                return null;
            }

            @Override
            public boolean onMouseRelease(int button) {
                return super.onMouseRelease(button) || button == 1;
            }
        };
    }

    @Override
    void resizePanel(WirelessCardLayout layout) {
        xSize = layout.width();
        ySize = layout.height();
        container.clientView().resize(layout.rows());
        int dx = xSize - 320;
        search.pos(10, 62).size(124 + dx, 12);
        dimension.pos(148 + dx, 62).size(34, 12);
    }

    @Override
    void layoutButton(WirelessCardButton control, WirelessCardLayout layout) {
        var button = control.widget;
        int dx = xSize - 320;
        switch (control.id) {
            case OPEN_SOURCES -> button.pos(8, 23).size(xSize - 16, 12);
            case TOGGLE_AUTO -> button.pos(xSize - 28, 10).size(16, 16);
            case TOGGLE_GROUPING -> button.pos(264 + dx, 59).size(44, 20);
            case CYCLE_FILTER -> button.pos(188 + dx, 59).size(72, 20);
            case CYCLE_VISUALISATION -> button.pos(layout.visualisationX(), ySize - 27)
                .size(layout.visualisationWidth(), 20);
        }
    }

    @Override
    boolean updatePanel() {
        boolean detail = container.display.getBoolean("detail");
        if (!detail) {
            if (container.clientView().groupDisplay().getBoolean("group")) groupPanel.openPanel();
            else groupPanel.closePanel();
        }
        if (detail != showingDetails) {
            showingDetails = detail;
            if (detail) detailsPanel.openPanel();
            else detailsPanel.closePanel();
        }
        boolean showFilters = !detail && !detailsPanel.isPanelOpen() && !view().getBoolean("sourcesPage");
        if (!showFilters && editingText()) getContext().removeFocus();
        search.setEnabled(showFilters);
        dimension.setEnabled(showFilters);
        return true;
    }

    @Override
    public void action(int id) {
        if (type(id) == RENAME_SOURCE) {
            if (!layoutReady() || !view().getBoolean("sourcesPage")) return;
            renameSourceId = view().getTagList("rows", 10).getCompoundTagAt(rowIndex(id)).getLong("targetId");
            if (renameSourceId > 0) sourceRenamePanel.openPanel();
            return;
        }
        super.action(id);
    }

    @Override
    public void sendAction(int id) {
        if (type(id) == DELETE_SOURCE) {
            if (!view().getBoolean("sourcesPage")) return;
            long target = view().getTagList("rows", 10).getCompoundTagAt(rowIndex(id)).getLong("targetId");
            container.sync.request(DELETE_SOURCE, target, "", 0);
            return;
        }
        if (view().getBoolean("sourcesPage") && type(id) == OPEN_DETAILS) {
            long target = view().getTagList("rows", 10).getCompoundTagAt(rowIndex(id)).getLong("targetId");
            container.sync.request(SELECT_SOURCE, target, "", 0);
            container.clientView().navigate(BACK, false);
            return;
        }
        if (id == APPLY_FILTER || id == CYCLE_FILTER) {
            applyFilters(true, id == CYCLE_FILTER);
            return;
        }
        super.sendAction(id);
    }

    private void applyFilters(boolean reportInvalid, boolean cycle) {
        int state = container.display.getInteger("filterState");
        if (cycle) state = (state + 1) % 5;
        if (!container.clientView().filter(searchText, dimensionText, state) && reportInvalid) {
            // #tr gtnotgood.wireless.gui.filter_invalid
            // # Invalid filter: enter an integer dimension ID or leave it blank.
            // # zh_CN 筛选无效：维度应填写整数或留空。
            mc.thePlayer.addChatMessage(new ChatComponentTranslation("gtnotgood.wireless.gui.filter_invalid"));
        }
    }

    @Override
    void drawHeader(NBTTagCompound data, int textColor) {
        if (data.getBoolean("sourcesPage")) {
            fontRendererObj.drawString(tr("sources_title"), 10, 11, textColor);
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(tr("sources_hint"), xSize - 20), 10, 29,
                textColor);
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(tr("sources_auto_hint"), xSize - 20), 10, 47,
                textColor);
            return;
        }
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(tr("title"), xSize - 140), 10, 11, textColor);
        String autoLabel = fontRendererObj.trimStringToWidth(tr(data.getBoolean("auto") ? "auto_on" : "auto_off"), 94);
        fontRendererObj.drawString(autoLabel, xSize - 32 - fontRendererObj.getStringWidth(autoLabel),
            10 + (16 - fontRendererObj.FONT_HEIGHT) / 2, textColor);
        String sourceLabel = data.getString("sourceLabel");
        if (!data.getString("sourceName").isEmpty())
            sourceLabel = data.getString("sourceName") + " (" + sourceLabel + ")";
        String source = "> " + (data.getBoolean("bound") ? tr("source", sourceLabel) : tr("unbound"));
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(source, xSize - 20), 10, 25, textColor);
        boolean budgetKnown = data.getBoolean("sourceLoaded") && data.getBoolean("channels")
            && data.getBoolean("stable")
            && data.getInteger("used") >= 0
            && data.getInteger("capacity") > 0;
        int budgetColor = !budgetKnown ? textColor
            : data.getInteger("used") >= data.getInteger("capacity") ? badColor() : goodColor();
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(budgetText(data), xSize - 116), 10, 36,
            budgetColor);
        ratioBar(xSize - 96, 37, 84, 6, budgetKnown ? data.getInteger("used") : -1, data.getInteger("capacity"),
            budgetColor, ColorUtils.craftingTreeTask.getColor());
        fontRendererObj.drawString("D", xSize - 182, 64, textColor);
    }

    @Override
    void drawFooter(NBTTagCompound data, int textColor) {
        if (data.getBoolean("sourcesPage")) return;
        if (!data.getBoolean("group")) {
            var layout = new WirelessCardLayout(xSize, ySize, visibleRows);
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(
                tr(data.getBoolean("filtered") ? "matched" : "loaded", data.getInteger("total")),
                layout.visualisationX() - layout.footerInfoX() - 4), layout.footerInfoX(), ySize - 20, textColor);
        }
    }
}
