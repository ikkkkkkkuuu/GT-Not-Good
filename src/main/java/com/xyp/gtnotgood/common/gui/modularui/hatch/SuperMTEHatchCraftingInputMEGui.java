package com.xyp.gtnotgood.common.gui.modularui.hatch;

import static gregtech.api.modularui2.GTGuis.createPopUpPanel;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.xyp.gtnotgood.common.gui.modularui.widget.GhostMoldItemStackHandler;
import com.xyp.gtnotgood.common.gui.modularui.widget.GhostMoldSlotWidget;
import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputME;

import appeng.api.implementations.ICraftingPatternItem;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.gui.modularui.hatch.base.MTEHatchBaseGui;
import gregtech.common.gui.modularui.util.PatternSlot;
import gregtech.common.modularui2.widget.builder.ItemSlotGridBuilder;

/**
 * Complete GUI implementation for Super ME Crafting Input Bus
 * Shares controls between the full-size and single-row pattern hatches.
 */
public class SuperMTEHatchCraftingInputMEGui extends MTEHatchBaseGui<SuperMTEHatchCraftingInputME> {

    private static final String PATTERN_INV_NAME = "pattern_inv";
    private static final String MANUAL_ITEM_INV_NAME = "manual_item_inv";
    private static final int PATTERN_SLOT_PER_ROW = 9;
    private static final int MANUAL_SLOT_ROW = 3;
    private static final int MANUAL_SLOT_PER_ROW = 3;

    public SuperMTEHatchCraftingInputMEGui(SuperMTEHatchCraftingInputME hatch) {
        super(hatch);
    }

    @Override
    protected int getBasePanelHeight() {
        return super.getBasePanelHeight() + SLOT_SIZE - (4 - visiblePatternRows()) * SLOT_SIZE;
    }

    private int visiblePatternRows() {
        return Math.min(4, machine.getPatternCount() / PATTERN_SLOT_PER_ROW);
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        return super.createContentSection(panel, syncManager).child(createSlots(syncManager));
    }

    private Grid createSlots(PanelSyncManager syncManager) {
        int rows = machine.getPatternCount() / PATTERN_SLOT_PER_ROW;
        syncManager.registerSlotGroup(PATTERN_INV_NAME, rows);
        Grid grid = new Grid().minColWidth(SLOT_SIZE)
            .gridOfWidthHeight(PATTERN_SLOT_PER_ROW, rows,
                ($x, $y,
                    index) -> new PatternSlot().slot(new ModularSlot(machine.inventoryHandler, index)
                        .filter(itemStack -> itemStack.getItem() instanceof ICraftingPatternItem)
                        .changeListener((itemStack, onlyAmount, client, init) -> {
                            if (!client) {
                                machine.onPatternChange(index, itemStack);
                            }
                        }).slotGroup(PATTERN_INV_NAME)))
            .size(PATTERN_SLOT_PER_ROW * SLOT_SIZE + 4, visiblePatternRows() * SLOT_SIZE);
        return rows > visiblePatternRows() ? grid.scrollable() : grid;
    }

    @Override
    protected Flow createBottomLeftCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        Flow flow = super.createBottomLeftCornerFlow(panel, syncManager).child(createOptimizerButton())
            .child(createShowPatternButton()).child(createExportButton()).child(createDoublePatternButton())
            .child(createManualItemsButton(syncManager));
        if (machine.hasVirtualMoldSlot()) flow.child(createMoldSlotButton(syncManager));
        return flow;
    }

    private ToggleButton createOptimizerButton() {
        BooleanSyncValue optimizerSync = new BooleanSyncValue(() -> !machine.disablePatternOptimization,
            val -> machine.disablePatternOptimization = !val).allowC2S();

        return new ToggleButton().value(optimizerSync).overlay(GTGuiTextures.OVERLAY_BUTTON_PATTERN_OPTIMIZE)
            .addTooltipLine(StatCollector.translateToLocal("GT5U.infodata.hatch.crafting_input_me.optimize_pattern"))
            .addTooltip(true,
                StatCollector.translateToLocal("GT5U.infodata.hatch.crafting_input_me.optimize_pattern.enable"))
            .addTooltip(false,
                StatCollector.translateToLocal("GT5U.infodata.hatch.crafting_input_me.optimize_pattern.disabled"));
    }

    private ToggleButton createShowPatternButton() {
        BooleanSyncValue showPatternSync = new BooleanSyncValue(() -> machine.showPattern,
            val -> machine.showPattern = val).allowC2S();

        return new ToggleButton().value(showPatternSync).overlay(true, GTGuiTextures.OVERLAY_BUTTON_WHITELIST)
            .overlay(false, GTGuiTextures.OVERLAY_BUTTON_BLACKLIST)
            .addTooltip(true,
                StatCollector.translateToLocal("GT5U.infodata.hatch.crafting_input_me.show_pattern.enable"))
            .addTooltip(false,
                StatCollector.translateToLocal("GT5U.infodata.hatch.crafting_input_me.show_pattern.disabled"));
    }

    private ButtonWidget<?> createExportButton() {
        InteractionSyncHandler exportSyncHandler = new InteractionSyncHandler().setOnMousePressed(mouseDelta -> {
            if (!mouseDelta.isClient() && mouseDelta.mouseButton == 0) {
                machine.refundAll(false);
            }
        });

        return new ButtonWidget<>().syncHandler(exportSyncHandler).overlay(GTGuiTextures.OVERLAY_BUTTON_EXPORT)
            .addTooltipLine(StatCollector.translateToLocal("GT5U.gui.tooltip.hatch.crafting_input_me.export"));
    }

    private ButtonWidget<?> createDoublePatternButton() {
        InteractionSyncHandler doubleSyncHandler = new InteractionSyncHandler().setOnMousePressed(mouseDelta -> {
            if (!mouseDelta.isClient()) {
                int val = mouseDelta.shift ? 1 : 0;
                if (mouseDelta.mouseButton == 1) val |= 0b10;
                machine.doublePatterns(val);
            }
        });

        return new ButtonWidget<>().syncHandler(doubleSyncHandler).overlay(GTGuiTextures.OVERLAY_BUTTON_X2)
            .addTooltipLine(StatCollector.translateToLocal("gui.tooltips.appliedenergistics2.DoublePatterns"));
    }

    private ButtonWidget<?> createManualItemsButton(PanelSyncManager syncManager) {
        IPanelHandler popupPanel = syncManager.syncedPanel("manual_slots_panel", true,
            (manager, handler) -> createManualSlotUI(manager));

        return new ButtonWidget<>().overlay(GTGuiTextures.OVERLAY_BUTTON_PLUS_LARGE)
            .addTooltipLine(
                StatCollector.translateToLocal("GT5U.gui.tooltip.hatch.crafting_input_me.place_manual_items"))
            .onMousePressed(mouseButton -> {
                popupPanel.openPanel();
                return popupPanel.isPanelOpen();
            });
    }

    private GhostMoldSlotWidget createMoldSlotButton(PanelSyncManager syncManager) {
        GhostMoldSlotWidget widget = new GhostMoldSlotWidget(machine, syncManager);
        widget.slot(new ModularSlot(new GhostMoldItemStackHandler(machine), 0));
        widget.size(18, 18);
        return widget;
    }

    private ModularPanel createManualSlotUI(PanelSyncManager syncManager) {
        return createPopUpPanel("manual_slots_panel").size(68, 76).bottomRelOffset(0.5f, 52)
            .child(new ItemSlotGridBuilder(machine.inventoryHandler, syncManager)
                .size(MANUAL_SLOT_PER_ROW, MANUAL_SLOT_ROW).slotGroupKey(MANUAL_ITEM_INV_NAME)
                .indexOffset(machine.getManualSlotStart()).build().marginTop(16).horizontalCenter());
    }
}
