// Adapted from GT5-Unofficial 5.09.54.183, LGPL-3.0. See META-INF/super-storage-input-port/.
package com.xyp.gtnotgood.common.machines.hatch.me;

import static com.xyp.gtnotgood.common.machines.hatch.me.SuperAdvancedMEInputBus.SLOT_COUNT;
import static gregtech.api.modularui2.GTGuis.createPopUpPanel;

import java.text.MessageFormat;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.item.IItemHandlerModifiable;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.GenericListSyncHandler;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

import appeng.core.localization.WailaText;
import appeng.me.GridAccessException;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTWidgetThemes;
import gregtech.api.util.GTDataUtils;
import gregtech.api.util.GTUtility;
import gregtech.common.gui.modularui.adapter.MTEHatchInputBusMESlotAdapter;
import gregtech.common.gui.modularui.hatch.base.MTEHatchBaseGui;
import gregtech.common.gui.modularui.util.StockingSlot;
import gregtech.common.tileentities.machines.MTEHatchInputBusME.Slot;

public class SuperAdvancedMEInputBusGui extends MTEHatchBaseGui<SuperAdvancedMEInputBus> {

    private static final String FILTER_INV_NAME = "filter_inv";
    private static final String STOCK_INV_NAME = "stock_inv";
    private static final int FILTER_SLOT_ROW = 100;
    private static final int FILTER_SLOT_PER_ROW = 9;
    private static final int STOCK_SLOT_ROW = 100;
    private static final int STOCK_SLOT_PER_ROW = 9;
    private final Slot[] slots;
    private final VerticalScrollData itemScroll = new VerticalScrollData();
    private int selectedSlot;
    private final IntSyncValue selection = new IntSyncValue(
        () -> selectedSlot,
        value -> selectedSlot = Math.max(0, Math.min(SLOT_COUNT - 1, value))).allowC2S();
    private IPanelHandler policyPanel;

    public SuperAdvancedMEInputBusGui(SuperAdvancedMEInputBus hatch, Slot[] slots) {
        super(hatch);
        this.slots = slots;
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        syncManager.syncValue("selectedItem", selection);
        policyPanel = syncManager
            .syncedPanel("itemPolicy", true, (manager, handler) -> createPolicyPanel(panel, manager));
        BooleanSyncValue isAutoPullSyncer = new BooleanSyncValue(
            machine::isAutoPullItemList,
            machine::setAutoPullItemList).allowC2S();

        IItemHandlerModifiable configItemHandler = new IItemHandlerModifiable() {

            @Override
            public int getSlots() {
                return SLOT_COUNT * 2;
            }

            @Override
            public ItemStack getStackInSlot(int slotIndex) {
                boolean forConfig = slotIndex < SLOT_COUNT;
                slotIndex %= SLOT_COUNT;

                Slot slot = GTDataUtils.getIndexSafe(slots, slotIndex);

                if (slot == null) return null;

                return forConfig ? slot.config : GTUtility.copyAmountUnsafe(slot.extractedAmount, slot.extracted);
            }

            @Override
            public @Nullable ItemStack insertItem(int slot, @Nullable ItemStack stack, boolean simulate) {
                return null;
            }

            @Override
            public @Nullable ItemStack extractItem(int slot, int amount, boolean simulate) {
                return null;
            }

            @Override
            public int getSlotLimit(int slot) {
                return Integer.MAX_VALUE;
            }

            @Override
            public void setStackInSlot(int slotIndex, ItemStack stack) {
                if (slotIndex < 0 || slotIndex >= SLOT_COUNT || !baseMetaTileEntity.isServerSide()) return;

                machine.setSlotConfig(slotIndex, GTUtility.copyAmount(1, stack));

                if (baseMetaTileEntity.isServerSide()) {
                    try {
                        machine.updateInformationSlot(slotIndex);
                    } catch (GridAccessException e) {
                        // :P
                    }
                }
            }
        };

        Flow mainRow = Flow.row()
            .coverChildren()
            .crossAxisAlignment(Alignment.CrossAxis.START);

        mainRow.child(createFilterSlots(syncManager, configItemHandler, isAutoPullSyncer));
        mainRow.child(createMiddleColumn(syncManager, panel, isAutoPullSyncer));
        mainRow.child(createStockSlots(syncManager, configItemHandler));

        return super.createContentSection(panel, syncManager).child(mainRow);
    }

    private Grid createFilterSlots(PanelSyncManager syncManager, IItemHandlerModifiable handler,
        BooleanSyncValue autoPull) {
        syncManager.registerSlotGroup(FILTER_INV_NAME, FILTER_SLOT_PER_ROW);
        return new Grid().gridOfWidthHeight(FILTER_SLOT_PER_ROW, FILTER_SLOT_ROW, (x, y, index) -> {
            StockingSlot widget = new StockingSlot(autoPull) {

                @Override
                public @NotNull Result onMousePressed(int button) {
                    if (button == 1 && slots[index] != null) {
                        selection.setIntValue(index);
                        policyPanel.openPanel();
                        return Result.SUCCESS;
                    }
                    return super.onMousePressed(button);
                }
            };
            // #tr gtng.super_storage_bus.configure_slot
            // # Right-click: configure reserve and fixed availability
            // # zh_CN 右键：配置保留量与固定可用量
            widget.itemTooltip()
                .tooltipBuilder(t -> t.addLine(IKey.lang("gtng.super_storage_bus.configure_slot")));
            return widget.slot(
                new ModularSlot(handler, index).slotGroup(FILTER_INV_NAME)
                    .filter(stack -> !autoPull.getBoolValue()));
        })
            .size(9 * SLOT_SIZE + 4, 4 * SLOT_SIZE)
            .scrollable(itemScroll);
    }

    private Flow createMiddleColumn(PanelSyncManager syncManager, ModularPanel panel,
        BooleanSyncValue isAutoPullSyncer) {
        Flow mainColumn = Flow.col()
            .width(18)
            .mainAxisAlignment(Alignment.MainAxis.START)
            .coverChildrenHeight();

        // toggle button for config panel
        IPanelHandler settingsPanel = syncManager
            .syncedPanel("configPanel", true, (manager, handler) -> createStackSizeConfigurationPanel(panel));
        mainColumn.child(new ToggleButton() {

            @Override
            public @NotNull Result onMousePressed(int mouseButton) {
                switch (mouseButton) {
                    case 0:
                        next();
                        playClickSound();
                        return Result.SUCCESS;
                    case 1:
                        if (!settingsPanel.isPanelOpen()) settingsPanel.openPanel();
                        else settingsPanel.closePanel();
                        playClickSound();
                        return Result.SUCCESS;
                }
                return Result.IGNORE;
            }
        }.value(isAutoPullSyncer)
            .size(16)
            .margin(1)
            .setEnabledIf(b -> machine.autoPullAvailable)
            .overlay(true, GTGuiTextures.OVERLAY_BUTTON_AUTOPULL_ME)
            .overlay(false, GTGuiTextures.OVERLAY_BUTTON_AUTOPULL_ME_DISABLED)
            .tooltip(t -> {
                t.addLine(IKey.lang("GT5U.machines.stocking_bus.auto_pull.tooltip.1"));
                t.addLine(IKey.lang("GT5U.machines.stocking_bus.auto_pull.tooltip.2"));
            }));

        // arrow
        mainColumn.child(
            GTGuiTextures.PICTURE_ARROW_DOUBLE.asWidget()
                .size(12)
                .margin(3));

        BooleanSyncValue limited = new BooleanSyncValue(machine::isLimitedMode, machine::setLimitedMode).allowC2S();
        BooleanSyncValue fixed = new BooleanSyncValue(machine::isFixedMode, machine::setFixedMode).allowC2S();
        // #tr gtng.super_storage_bus.limit_mode
        // # Reserve mode: keep each item's configured reserve in ME
        // # zh_CN 限制模式：在ME中保留每种物品设定的数量
        mainColumn.child(
            new ToggleButton().value(limited)
                .size(16)
                .margin(1)
                .overlay(true, GTGuiTextures.OVERLAY_BUTTON_LOCKED)
                .overlay(false, GTGuiTextures.OVERLAY_BUTTON_LOCK)
                .addTooltipLine(IKey.lang("gtng.super_storage_bus.limit_mode")));
        // #tr gtng.super_storage_bus.fixed_mode
        // # Fixed mode: offer each item's configured quantity per recipe check
        // # zh_CN 固定模式：每次配方检查提供各物品设定的数量
        mainColumn.child(
            new ToggleButton().value(fixed)
                .size(16)
                .margin(1)
                .overlay(true, GTGuiTextures.OVERLAY_BUTTON_CHECKMARK)
                .overlay(false, GTGuiTextures.OVERLAY_BUTTON_CROSS)
                .addTooltipLine(IKey.lang("gtng.super_storage_bus.fixed_mode")));
        return mainColumn;
    }

    private ModularPanel createStackSizeConfigurationPanel(ModularPanel parent) {
        IntSyncValue minAutoPullStackSizeSyncer = new IntSyncValue(
            machine::getMinAutoPullStackSize,
            machine::setMinAutoPullStackSize).allowC2S();
        IntSyncValue autoPullRefreshTimeSyncer = new IntSyncValue(
            machine::getAutoPullRefreshTime,
            machine::setAutoPullRefreshTime).allowC2S();

        Flow mainColumn = Flow.col()
            .coverChildren()
            .marginTop(15)
            .childPadding(3);

        // stack size label
        mainColumn.child(
            IKey.lang("GT5U.machines.stocking_bus.min_stack_size")
                .asWidget());

        // stack size text field
        mainColumn.child(
            new TextFieldWidget().value(minAutoPullStackSizeSyncer)
                .numbersInt(1, Integer.MAX_VALUE)
                .formatAsInteger(true)
                .setMaxLength(10)
                .setTextAlignment(Alignment.CENTER)
                .width(72));

        // refresh time label
        mainColumn.child(
            IKey.lang("GT5U.machines.stocking_bus.refresh_time")
                .asWidget()
                .maxWidth(72)
                .textAlign(Alignment.Center));

        // refresh time text field
        mainColumn.child(
            new TextFieldWidget().value(autoPullRefreshTimeSyncer)
                .numbersInt(1, Integer.MAX_VALUE)
                .formatAsInteger(true)
                .setMaxLength(10)
                .setTextAlignment(Alignment.CENTER)
                .width(72));

        return createPopUpPanel("configPanel").coverChildren()
            .relative(parent)
            .padding(5)
            .child(mainColumn)
            .leftRel(1)
            .topRel(0);
    }

    private Grid createStockSlots(PanelSyncManager syncManager, IItemHandlerModifiable handler) {
        syncManager.registerSlotGroup(STOCK_INV_NAME, STOCK_SLOT_PER_ROW);
        return new Grid()
            .gridOfWidthHeight(
                STOCK_SLOT_PER_ROW,
                STOCK_SLOT_ROW,
                (x, y, index) -> new ItemSlot()
                    .slot(
                        new ModularSlot(handler, SLOT_COUNT + index).slotGroup(STOCK_INV_NAME)
                            .accessibility(false, false))
                    .backgroundOverlay(GTGuiTextures.SLOT_ITEM_DARK))
            .size(9 * SLOT_SIZE + 4, 4 * SLOT_SIZE)
            .scrollable(itemScroll);
    }

    @Override
    protected Flow createBottomRightCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        return Flow.row()
            .coverChildren()
            .childPadding(2)
            .verticalCenter()
            .rightRel(0)
            .child(createCircuitSlot(syncManager))
            .child(
                new ItemSlot().slot(
                    new ModularSlot(machine.inventoryHandler, machine.getManualSlot()).slotGroup("item_inv")
                        .changeListener((stack, amountOnly, client, init) -> {
                            if (!client) {
                                baseMetaTileEntity.enableTicking();
                                machine.onContentsChanged(machine.getManualSlot());
                            }
                        }))
                    .addTooltipLine(IKey.lang("GT5U.machines.stocking_bus.manual_slot.tooltip.1")))
            .child(makeLogoWidget());
    }

    @Override
    protected Flow createBottomLeftCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        BooleanSyncValue isActiveSyncer = new BooleanSyncValue(machine::isActive);
        BooleanSyncValue isPoweredSyncer = new BooleanSyncValue(machine::isPowered);
        BooleanSyncValue isBootingSyncer = new BooleanSyncValue(machine::isBooting);
        BooleanSyncValue isAllowedToWorkSyncer = new BooleanSyncValue(machine::isAllowedToWork);

        syncManager.syncValue("isActive", isActiveSyncer);
        syncManager.syncValue("isPowered", isPoweredSyncer);
        syncManager.syncValue("isBooting", isBootingSyncer);
        syncManager.syncValue("isAllowedToWork", isAllowedToWorkSyncer);

        // status label
        TextWidget<?> status = IKey.dynamic(() -> {
            boolean isActive = isActiveSyncer.getBoolValue();
            boolean isPowered = isPoweredSyncer.getBoolValue();
            boolean isBooting = isBootingSyncer.getBoolValue();

            String state = WailaText.getPowerState(isActive, isPowered, isBooting);

            if (isActive && isPowered) {
                return MessageFormat.format(
                    "{0} ({1})",
                    EnumChatFormatting.GREEN + state + EnumChatFormatting.RESET,
                    IKey.lang(
                        isAllowedToWorkSyncer.getBoolValue() ? "GT5U.gui.text.enabled" : "GT5U.gui.text.disabled"));
            } else {
                return EnumChatFormatting.DARK_RED + state + EnumChatFormatting.RESET;
            }
        })
            .asWidget()
            .widgetTheme(GTWidgetThemes.DISPLAY_TEXT_WHITE);

        return super.createBottomLeftCornerFlow(panel, syncManager).child(status);
    }

    @Override
    public void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.registerSlotGroup("item_inv", 1);

        GenericListSyncHandler<Slot> slotSyncHandler = GenericListSyncHandler.<Slot>builder()
            .getter(() -> GTDataUtils.mapToList(slots, slot -> {
                if (slot == null) return null;
                Slot copy = new Slot(slot.config.copy());
                copy.extracted = slot.extracted == null ? null : slot.extracted.copy();
                copy.extractedAmount = slot.extractedAmount;
                return copy;
            }))
            .setter(slots2 -> System.arraycopy(slots2.toArray(new Slot[0]), 0, slots, 0, SLOT_COUNT))
            .adapter(new MTEHatchInputBusMESlotAdapter())
            .build();
        syncManager.syncValue("slots", slotSyncHandler);
    }

    @Override
    protected int getBasePanelHeight() {
        return super.getBasePanelHeight() + SLOT_SIZE;
    }

    @Override
    protected int getBasePanelWidth() {
        return 366;
    }

    private ModularPanel createPolicyPanel(ModularPanel parent, PanelSyncManager sync) {
        LongSyncValue reserve = new LongSyncValue(() -> machine.getPolicy(selectedSlot).reserve, value -> {
            if (baseMetaTileEntity.isServerSide()) {
                machine.getPolicy(selectedSlot).reserve = Math.max(0, value);
                machine.policyChanged();
            }
        }).allowC2S();
        IntSyncValue batch = new IntSyncValue(() -> machine.getPolicy(selectedSlot).batch, value -> {
            if (baseMetaTileEntity.isServerSide()) {
                machine.getPolicy(selectedSlot).batch = Math.max(1, value);
                machine.policyChanged();
            }
        }).allowC2S();
        Flow content = Flow.col()
            .coverChildren()
            .childPadding(4)
            .marginTop(16);
        content.child(IKey.dynamic(() -> {
            ItemStack item = machine.getSlotConfig(selection.getIntValue());
            return item == null ? "" : item.getDisplayName();
        })
            .asWidget()
            .maxWidth(200));
        // #tr gtng.super_storage_bus.reserve_amount
        // # Keep in ME (items)
        // # zh_CN ME网络保留量（个）
        content.child(
            IKey.lang("gtng.super_storage_bus.reserve_amount")
                .asWidget());
        content.child(
            new TextFieldWidget().value(reserve)
                .numbersLong(0, Long.MAX_VALUE)
                .formatAsInteger(true)
                .setMaxLength(19)
                .width(180));
        // #tr gtng.super_storage_bus.batch_amount
        // # Available per recipe check (items)
        // # zh_CN 每次配方检查可用量（个）
        content.child(
            IKey.lang("gtng.super_storage_bus.batch_amount")
                .asWidget());
        content.child(
            new TextFieldWidget().value(batch)
                .numbersInt(1, Integer.MAX_VALUE)
                .formatAsInteger(true)
                .setMaxLength(10)
                .width(180));
        // #tr gtng.super_storage_bus.batch_help
        // # Waits for the full amount; consumes only what the recipe needs
        // # zh_CN 不足设定量则等待，实际只扣除配方消耗
        content.child(
            IKey.lang("gtng.super_storage_bus.batch_help")
                .asWidget()
                .maxWidth(200));
        return createPopUpPanel("itemPolicy").size(224, 160)
            .relative(parent)
            .center()
            .padding(8)
            .child(content);
    }

}
