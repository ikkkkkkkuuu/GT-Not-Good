// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure PR #627 at 651396c75d108e6ff5399a44c56c12214ad27d3d.

package com.xyp.gtnotgood.client.wirelessfrequency;

import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.BACK;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.COPY_POSITION;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.CYCLE_FILTER;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.CYCLE_VISUALISATION;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.DELETE_SOURCE;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.HIGHLIGHT;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.NEXT_PAGE;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.OPEN_DETAILS;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.OPEN_SOURCES;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.PREVIOUS_PAGE;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.REFRESH;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.RENAME;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.RENAME_SOURCE;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.TELEPORT;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.TOGGLE_AUTO;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.TOGGLE_GROUPING;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.TOGGLE_PAUSE;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.isRowAction;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.rowIndex;
import static com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardActions.type;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.layout.IViewportStack;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.value.IntValue;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardVisualisation;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.client.gui.ScreenColor;
import appeng.core.localization.GuiText;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class WirelessCardButton {

    public static final UITexture[] BUTTON_TEXTURES = { buttonTexture(0), buttonTexture(1), buttonTexture(2) };
    private static final UITexture TOGGLE_BACKGROUND = toggleTexture(255), TOGGLE_ON = toggleTexture(129),
        TOGGLE_OFF = toggleTexture(128);
    private static final IDrawable[] TINTED_BUTTONS = { tinted(BUTTON_TEXTURES[0]), tinted(BUTTON_TEXTURES[1]),
        tinted(BUTTON_TEXTURES[2]) };
    public final int id;
    public final Widget<?> widget;
    private final WirelessCardPanel panel;
    private final FontRenderer fontRendererObj = Minecraft.getMinecraft().fontRenderer;
    private boolean available, on;
    private String label = "";

    WirelessCardButton(int id, WirelessCardPanel panel) {
        this.id = id;
        this.panel = panel;
        if (id == TOGGLE_AUTO || type(id) == TOGGLE_PAUSE) {
            widget = new ToggleButton() {

                @Override
                public boolean canBeSeen(IViewportStack stack) {
                    return isEnabled() && super.canBeSeen(stack);
                }

                @Override
                public @NotNull Result onMousePressed(int button) {
                    return button == 0 && available ? super.onMousePressed(button) : Result.ACCEPT;
                }
            }.value(new BoolValue.Dynamic(() -> on, ignored -> panel.action(id)))
                .background(false, tinted(TOGGLE_BACKGROUND)).background(true, tinted(TOGGLE_BACKGROUND))
                .hoverBackground(false, tinted(TOGGLE_BACKGROUND)).hoverBackground(true, tinted(TOGGLE_BACKGROUND))
                .overlay(false, TOGGLE_OFF).overlay(true, TOGGLE_ON);
        } else if (id == CYCLE_FILTER) {
            widget = new CycleButtonWidget() {

                @Override
                public boolean canBeSeen(IViewportStack stack) {
                    return isEnabled() && super.canBeSeen(stack);
                }

                @Override
                public @NotNull Result onMousePressed(int button) {
                    return button == 0 && available ? super.onMousePressed(button) : Result.ACCEPT;
                }
            }.stateCount(5)
                .value(new IntValue.Dynamic(() -> panel.view().getInteger("filterState"), ignored -> panel.action(id)));
            styleTextButton();
        } else {
            widget = new ActionButton().onMousePressed(button -> {
                if (button != 0 || !available) return false;
                panel.action(id);
                return true;
            });
            if (id == OPEN_SOURCES) widget.background(IDrawable.EMPTY).hoverBackground(IDrawable.EMPTY);
            else styleTextButton();
        }
        if (type(id) == OPEN_DETAILS || type(id) == RENAME_SOURCE) widget.tooltipBuilder(tooltip -> {
            NBTTagCompound row = panel.view().getTagList("rows", 10).getCompoundTagAt(rowIndex(id));
            String name = WirelessCardPanel.rowName(row, ItemStack.loadItemStackFromNBT(row.getCompoundTag("icon")));
            String targetName = row.getString("targetName");
            tooltip.addLine(targetName.isEmpty() || targetName.equals(name) ? name : name + " - " + targetName);
            tooltip.addLine(row.getString("positionLabel"));
            if (row.getBoolean("group")) tooltip.addLine(tr("loaded", row.getInteger("clusters")));
            if (panel.view().getBoolean("sourcesPage"))
                tooltip.addLine(tr(row.getBoolean("loaded") ? "source_loaded" : "source_unloaded"));
        }).tooltipAutoUpdate(true);
        if (id == OPEN_SOURCES) widget.tooltipBuilder(tooltip -> tooltip.addLine(tr("sources_open")));
        if (type(id) == DELETE_SOURCE) widget.tooltipBuilder(tooltip -> tooltip.addLine(tr("source_delete_hint")));
        if (
            id == TOGGLE_GROUPING
        ) widget
            .tooltipBuilder(tooltip -> tooltip.addLine(GuiText.Types.getLocal() + ": "
                + StatCollector
                    .translateToLocal(panel.view().getBoolean("collapseTypes") ? "options.on" : "options.off")))
            .tooltipAutoUpdate(true);
    }

    private static class ActionButton extends ButtonWidget<ActionButton> {

        @Override
        public boolean canBeSeen(IViewportStack stack) {
            return isEnabled() && super.canBeSeen(stack);
        }
    }

    private void styleTextButton() {
        IDrawable background = (context, x, y, width, height, theme) -> {
            if (type(id) == OPEN_DETAILS && !panel.view().getBoolean("sourcesPage")) return;
            TINTED_BUTTONS[!available ? 0 : widget.isHovering() ? 2 : 1].draw(context, x, y, width, height, theme);
        };
        widget.background(background).hoverBackground(background).overlay(
            IKey.dynamic(() -> fontRendererObj.trimStringToWidth(label, Math.max(0, widget.getArea().width - 4)))
                .color(() -> !available ? 0xffa0a0a0 : widget.isHovering() ? 0xffffffa0 : 0xffe0e0e0).shadow(true));
    }

    private static IDrawable tinted(UITexture texture) {
        return (context, x, y, width, height, theme) -> {
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            try {
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                ScreenColor.setGuiColor();
                texture.draw(x, y, width, height);
            } finally {
                GL11.glPopAttrib();
            }
        };
    }

    void update(NBTTagCompound data, boolean ready, int visibleRows) {
        NBTTagList rows = data.getTagList("rows", 10);
        int rowCount = Math.min(visibleRows, rows.tagCount());
        int actionType = type(id);
        if (actionType == OPEN_DETAILS) {
            int y = panel.rowTop() + rowIndex(id) * WirelessCardPanel.ROW_HEIGHT;
            if (data.getBoolean("sourcesPage")) widget.pos(panel.xSize - 116, y + 2).size(60, 20);
            else widget.pos(10, y).size(panel.rowStateX() - 10, WirelessCardPanel.ROW_HEIGHT);
        }
        boolean visible = true;
        available = ready;
        if (isRowAction(id)) {
            int index = rowIndex(id);
            if (index >= rowCount) {
                widget.setEnabled(false);
                return;
            }
            NBTTagCompound row = rows.getCompoundTagAt(index);
            if (actionType == TOGGLE_PAUSE) on = !row.getBoolean("paused");
            if (actionType == TOGGLE_PAUSE || actionType == HIGHLIGHT || actionType == TELEPORT)
                visible = !row.getBoolean("group");
            if (actionType == COPY_POSITION) visible = !row.getBoolean("category");
            if (actionType == OPEN_DETAILS) available &= !row.getBoolean("leaf");
            if (actionType == OPEN_DETAILS && data.getBoolean("sourcesPage")) available &= !row.getBoolean("selected");
        }
        if (id == TOGGLE_AUTO) on = data.getBoolean("auto");
        if (id == BACK) visible = data.getBoolean("detail") || data.getBoolean("group");
        if (id == RENAME) {
            visible = data.getBoolean("detail");
            available &= data.getBoolean("renameable");
        }
        if (id == CYCLE_FILTER || id == TOGGLE_AUTO || id == CYCLE_VISUALISATION || id == TOGGLE_GROUPING)
            visible = !data.getBoolean("detail");
        if (id == TOGGLE_GROUPING) visible &= !data.getBoolean("typeGroup");
        if (actionType == OPEN_DETAILS) visible &= !data.getBoolean("detail");
        if (id == OPEN_SOURCES) visible = !data.getBoolean("detail") && !data.getBoolean("sourcesPage");
        if (actionType == RENAME_SOURCE || actionType == DELETE_SOURCE) visible = data.getBoolean("sourcesPage");
        if (data.getBoolean("sourcesPage")) {
            if (isRowAction(id))
                visible = actionType == OPEN_DETAILS || actionType == RENAME_SOURCE || actionType == DELETE_SOURCE;
            if (id == BACK) visible = true;
            if (id == TOGGLE_AUTO || id == CYCLE_FILTER || id == CYCLE_VISUALISATION || id == TOGGLE_GROUPING)
                visible = false;
        }
        widget.setEnabled(visible);
        if (!visible) return;
        if (id == PREVIOUS_PAGE) available &= data.getInteger("page") > 0;
        if (id == NEXT_PAGE) available &= data.getInteger("page") + 1 < data.getInteger("pages");
        label = switch (actionType) {
            case PREVIOUS_PAGE -> "<";
            case NEXT_PAGE -> ">";
            case REFRESH -> tr("refresh");
            case BACK -> tr("back_compact");
            case RENAME, RENAME_SOURCE -> StatCollector.translateToLocal("selectWorld.rename");
            case DELETE_SOURCE -> tr("source_delete");
            case CYCLE_FILTER -> tr("filter." + data.getInteger("filterState"));
            case TOGGLE_GROUPING -> GuiText.Types.getLocal() + (data.getBoolean("collapseTypes") ? " +" : " −");
            case TELEPORT -> tr("teleport");
            case HIGHLIGHT -> tr("highlight");
            case COPY_POSITION -> tr("copy");
            case OPEN_DETAILS -> data.getBoolean("sourcesPage")
                ? tr(rows.getCompoundTagAt(rowIndex(id)).getBoolean("selected") ? "source_selected" : "source_select")
                : "";
            default -> "";
        };
        if (id == CYCLE_VISUALISATION) {
            int mode = data.getInteger("visualMode");
            String label = StatCollector
                .translateToLocal(WirelessCardVisualisation.modeKey(mode) + (mode > 0 ? ".short" : ""));
            this.label = fontRendererObj.trimStringToWidth(
                widget.getArea().width < 102 ? label : tr("visualisation_button", label), widget.getArea().width - 8);
        }
    }

    private static UITexture buttonTexture(int state) {
        return UITexture.builder().location(new ResourceLocation("minecraft", "textures/gui/widgets.png"))
            .imageSize(256, 256).subAreaXYWH(0, 46 + state * 20, 200, 20).adaptable(2).build();
    }

    private static UITexture toggleTexture(int icon) {
        return UITexture.builder().location(ModList.AE2.getResourceLocation("textures/guis/states.png"))
            .imageSize(256, 256).subAreaXYWH(icon % 16 * 16, icon / 16 * 16, 16, 16).build();
    }

    private static String tr(String key, Object... args) {
        return WirelessCardText.tr(key, args);
    }

}
