package com.xyp.gtnotgood.common.gui.modularui.widget;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.DrawableStack;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.MouseData;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.PhantomItemSlotSH;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.slot.PhantomItemSlot;
import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtnotgood.common.utils.MoldDataManager;

import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTGuis;
import gregtech.common.modularui2.widget.SlotLikeButtonWidget;

/**
 * 幽灵模具槽位组件
 * 仿照 GhostCircuitSlotWidget/GhostMoldSlotWidget，适配 SuperMTEHatchCraftingInputME
 * 点击可选择模具，模具参与合成配方匹配
 */
public class GhostMoldSlotWidget extends PhantomItemSlot {

    private static final String GUI_ID = "mold_selector";

    private final SuperMTEHatchCraftingInputME hatch;
    private GhostMoldSyncHandler moldSyncHandler;
    private IPanelHandler selectorPanelHandler;
    private final PanelSyncManager syncManager;

    public GhostMoldSlotWidget(SuperMTEHatchCraftingInputME hatch, PanelSyncManager syncManager) {
        super();
        this.hatch = hatch;
        this.syncManager = syncManager;
        tooltipBuilder(this::getMoldSlotTooltip);
        selectorPanelHandler = buildSelectorPanel();
    }

    @Override
    public IDrawable getCurrentBackground(WidgetThemeEntry<?> widgetTheme) {
        IDrawable background = super.getCurrentBackground(widgetTheme);
        return new DrawableStack(background, GTGuiTextures.OVERLAY_SLOT_MOLD);
    }

    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        if (!isSelectorPanelOpen()) {
            if (mouseButton == 0 && Interactable.hasShiftDown()) {
                openSelectorPanel();
            } else {
                MouseData mouseData = MouseData.create(mouseButton);
                getSyncHandler().syncToServer(PhantomItemSlotSH.SYNC_CLICK, mouseData::writeToPacket);
            }
        }
        return Result.SUCCESS;
    }

    @Override
    public boolean onMouseScroll(UpOrDown scrollDirection, int amount) {
        if (isSelectorPanelOpen()) return true;
        MouseData mouseData = MouseData.create(scrollDirection.modifier);
        getSyncHandler().syncToServer(PhantomItemSlotSH.SYNC_SCROLL, mouseData::writeToPacket);
        return true;
    }

    @Override
    public PhantomItemSlot slot(ModularSlot slot) {
        moldSyncHandler = new GhostMoldSyncHandler(slot, hatch);
        setSyncOrValue(moldSyncHandler);
        moldSyncHandler.registerIndexSync(syncManager, "ghostMoldIndex");
        return this;
    }

    private void getMoldSlotTooltip(RichTooltip tooltip) {
        ItemStack current = hatch.inventoryHandler.getStackInSlot(hatch.getMoldSlot());
        String moldName;
        if (current == null) {
            // #tr GT5U.machines.select_mold.tooltip
            // # Ghost Mold Slot
            // # zh_CN 虚拟模具槽
            moldName = StatCollector.translateToLocal("GT5U.machines.select_mold.tooltip");
        } else {
            moldName = current.getDisplayName();
        }
        tooltip.clearText()
            .addLine(moldName)
            // #tr GT5U.machines.select_mold.tooltip.1
            // # Shift+Click to select mold
            // # zh_CN Shift+点击以选择模具
            .addLine(IKey.lang("GT5U.machines.select_mold.tooltip.1"))
            // #tr GT5U.machines.select_mold.tooltip.2
            // # Scroll to change mold
            // # zh_CN 滚动鼠标滚轮切换模具
            .addLine(IKey.lang("GT5U.machines.select_mold.tooltip.2"));
    }

    private boolean isSelectorPanelOpen() {
        return getPanel().getScreen()
            .isPanelOpen(GUI_ID);
    }

    private void openSelectorPanel() {
        if (selectorPanelHandler == null) {
            selectorPanelHandler = buildSelectorPanel();
        }
        selectorPanelHandler.openPanel();
    }

    private static final int SELECTOR_COLS = 9;
    private static final int SELECTOR_VISIBLE_ROWS = 9;
    private static final int SELECTOR_SLOT_SIZE = 18;
    private static final int SELECTOR_GUI_WIDTH = SELECTOR_SLOT_SIZE * SELECTOR_COLS + 7 * 2;
    private static final int SELECTOR_HEADER_HEIGHT = 46;
    private static final int SELECTOR_GUI_HEIGHT = SELECTOR_HEADER_HEIGHT + SELECTOR_SLOT_SIZE * SELECTOR_VISIBLE_ROWS;

    private IPanelHandler buildSelectorPanel() {
        return syncManager.syncedPanel("moldSlotPanel", true, (mainPanel, player) -> {
            ModularPanel panel = GTGuis.createPopUpPanel(GUI_ID);
            ItemStack[] molds = MoldDataManager.getMolds();
            int totalRows = (molds.length + SELECTOR_COLS - 1) / SELECTOR_COLS;

            panel.size(SELECTOR_GUI_WIDTH, SELECTOR_GUI_HEIGHT);

            // Header: item icon + title
            panel.child(
                Flow.row()
                    .coverChildren()
                    .childPadding(4)
                    .pos(5, 5)
                    .child(
                        new ItemDrawable(hatch.getStackForm(1)).asWidget()
                            .size(16))
                    .child(
                        IKey.lang("GT5U.machines.select_mold")
                            .asWidget()));

            // "Current" label
            panel.child(
                IKey.lang("GT5U.gui.select.current")
                    .asWidget()
                    .leftRel(0.5f, -(SELECTOR_SLOT_SIZE / 2) - 3, 1)
                    .height(SELECTOR_SLOT_SIZE)
                    .top(22));

            // Current selection widget
            panel.child(new SlotLikeButtonWidget(() -> {
                int idx = moldSyncHandler.getIndexSync()
                    .getIntValue();
                return idx >= 0 && idx < molds.length ? molds[idx] : null;
            }).background(GTGuiTextures.SLOT_ITEM_DARK, new DynamicDrawable(() -> GTGuiTextures.OVERLAY_SLOT_MOLD))
                .playClickSound(false)
                .onMousePressed(mouseButton -> true)
                .horizontalCenter()
                .top(22));

            // Scrollable grid of all choices - shows 9 rows, scroll for the rest
            // Use minColWidth to keep columns properly sized
            panel.child(
                new Grid().minColWidth(SELECTOR_SLOT_SIZE)
                    .gridOfWidthHeight(SELECTOR_COLS, totalRows, (x, y, index) -> {
                        if (index >= molds.length) return null;
                        SlotLikeButtonWidget widget = new SlotLikeButtonWidget(molds[index])
                            .background(
                                new DynamicDrawable(
                                    () -> moldSyncHandler.getSelectedIndex() == index ? GTGuiTextures.SLOT_ITEM_DARK
                                        : GTGuiTextures.SLOT_ITEM_STANDARD))
                            .size(SELECTOR_SLOT_SIZE)
                            .onMousePressed(mouseButton -> {
                                if (mouseButton == 0) {
                                    moldSyncHandler.setSelectedIndex(index);
                                } else {
                                    moldSyncHandler.setSelectedIndex(-1);
                                }
                                MouseData mouseData = MouseData.create(mouseButton);
                                if (mouseData.shift) {
                                    panel.closeIfOpen();
                                }
                                return true;
                            });
                        return widget;
                    })
                    .size(SELECTOR_COLS * SELECTOR_SLOT_SIZE + 4, SELECTOR_VISIBLE_ROWS * SELECTOR_SLOT_SIZE)
                    .scrollable()
                    .pos(7, SELECTOR_HEADER_HEIGHT));

            return panel;
        });
    }

    @NotNull
    @Override
    public GhostMoldSyncHandler getSyncHandler() {
        return moldSyncHandler;
    }
}
