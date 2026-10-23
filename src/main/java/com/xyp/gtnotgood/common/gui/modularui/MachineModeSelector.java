package com.xyp.gtnotgood.common.gui.modularui;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import java.util.List;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;

public final class MachineModeSelector {

    private MachineModeSelector() {}

    public static ButtonWidget<?> createButton(PanelSyncManager sync, String prefix, List<IKey> names,
        IntSyncValue mode) {
        IPanelHandler selector = sync.syncedPanel(prefix + "MachineModes", true,
            (panelSync, handler) -> createPanel(prefix, names, mode, handler));
        return new ButtonWidget<>().name(prefix + "ModeSelector").size(16).background(GTNGGuiTextures.MODERN_BUTTON)
            .hoverBackground(GTNGGuiTextures.MODERN_BUTTON_HOVER).overlay(GTNGGuiTextures.OVERLAY_BUTTON_MACHINE_MODE)
            .onMousePressed(button -> {
                if (selector.isPanelOpen()) selector.closePanel();
                else selector.openPanel();
                return true;
            }).tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    private static ModularPanel createPanel(String prefix, List<IKey> names, IntSyncValue mode, IPanelHandler handler) {
        int height = Math.min(6, names.size()) * 22;
        ModularPanel panel = new ModularPanel(prefix + "MachineModes").size(180, height + 32)
            .background(GTNGGuiTextures.MODERN_BACKGROUND);
        panel.child(ButtonWidget.panelCloseButton());
        // #tr gtng.light_hunter.satellite.select_mode
        // # Select machine mode
        // # zh_CN 选择机器模式
        panel.child(IKey.lang("gtng.light_hunter.satellite.select_mode").color(Color.WHITE.main).asWidget().pos(8, 7)
            .size(148, 12));
        ListWidget<IWidget, ?> modes = new ListWidget<>().name(prefix + "ModeList").pos(8, 24).size(164, height);
        for (int index = 0; index < names.size(); index++) {
            int selected = index;
            modes.child(new ButtonWidget<>()
                .name(prefix + "Mode:" + index).height(20).fullWidth().marginBottom(2)
                .background(
                    new DynamicDrawable(() -> mode.getIntValue() == selected ? GTNGGuiTextures.MODERN_BUTTON_SELECTED
                        : GTNGGuiTextures.MODERN_BUTTON))
                .hoverBackground(
                    new DynamicDrawable(() -> mode.getIntValue() == selected ? GTNGGuiTextures.MODERN_BUTTON_SELECTED
                        : GTNGGuiTextures.MODERN_BUTTON_HOVER))
                .overlay(names.get(index).color(Color.WHITE.main))
                // #tr gtng.gui.select_machine_mode.shift
                // # Shift-click to select and close
                // # zh_CN Shift 点击选择并关闭
                .addTooltipLine(IKey.lang("gtng.gui.select_machine_mode.shift")).tooltipShowUpTimer(TOOLTIP_DELAY)
                .syncHandler(new InteractionSyncHandler().setOnMousePressed(data -> {
                    if (data.isClient() || data.mouseButton > 1) return;
                    if (mode.getIntValue() != selected) mode.setIntValue(selected, true, true);
                    if (mode.getIntValue() == selected && data.shift) handler.closePanel();
                })));
        }
        return panel.child(modes);
    }
}
