package com.xyp.gtnotgood.common.machines.basic;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil.formatNumber;
import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil.getFluidUnit;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ProgressWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.gtnewhorizon.cropsnh.init.CropsNHUITextures;
import com.xyp.gtnotgood.utils.enums.ModList;

import gregtech.api.modularui2.GTGuiTextures;
import gregtech.common.gui.modularui.singleblock.base.MTETieredMachineBlockBaseGui;
import gregtech.common.gui.modularui.util.MachineModularSlot;
import gregtech.common.gui.modularui.widget.SteamGaugeWidget;

final class SteamCropManagerGui extends MTETieredMachineBlockBaseGui<SteamCropManager> {

    private static final String INPUT_SLOT_GROUP = "cropInputs";
    private static final String TOOLTIP_PREFIX = ModList.CropsNH.getID() + "_tooltip.cropManager.";
    private static final int INPUT_COLUMNS = 2;
    private static final int OUTPUT_COLUMNS = 5;
    private static final int SLOT_ROWS = 3;

    SteamCropManagerGui(SteamCropManager machine) {
        super(machine);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        syncManager.registerSlotGroup(INPUT_SLOT_GROUP, INPUT_COLUMNS, true);
        syncManager.syncValue("powerSwitch", new BooleanSyncValue(baseMetaTileEntity::isAllowedToWork, enabled -> {
            if (!baseMetaTileEntity.isServerSide()) return;
            if (enabled) baseMetaTileEntity.enableWorking();
            else baseMetaTileEntity.disableWorking();
            baseMetaTileEntity.markDirty();
        }).allowC2S());
    }

    @Override
    protected int getBasePanelWidth() {
        return 228;
    }

    @Override
    protected boolean supportsMuffler() {
        return false;
    }

    @Override
    protected boolean supportsTopRightCornerFlow() {
        return false;
    }

    private ItemSlot createSlot(int index) {
        ModularSlot slot = new MachineModularSlot(machine.inventoryHandler, index, baseMetaTileEntity);
        ItemSlot widget = new ItemSlot().slot(slot);
        if (index >= SteamCropManager.SLOT_WEEDEX_START && index <= SteamCropManager.SLOT_WEEDEX_END) {
            widget.backgroundOverlay(CropsNHUITextures.OVERLAY_SLOT_WEED_EX_STANDARD);
            slot.slotGroup(INPUT_SLOT_GROUP);
        } else if (index >= SteamCropManager.SLOT_FERT_START && index <= SteamCropManager.SLOT_FERT_END) {
            widget.backgroundOverlay(CropsNHUITextures.OVERLAY_SLOT_FERTILIZER_STANDARD);
            slot.slotGroup(INPUT_SLOT_GROUP);
        } else {
            slot.accessibility(false, true);
        }
        return widget;
    }

    private IWidget createTankBar(PanelSyncManager syncManager, IntSupplier amount, IntSupplier capacity, String name,
        UITexture texture, String tooltipKey) {
        IntSyncValue stored = new IntSyncValue(amount);
        IntSyncValue maximum = new IntSyncValue(capacity);
        String progressName = name + "Progress";
        syncManager.syncValue(name + "Stored", stored);
        syncManager.syncValue(name + "Capacity", maximum);
        syncManager.syncValue(progressName,
            new DoubleSyncValue(() -> (double) amount.getAsInt() / Math.max(1, capacity.getAsInt())));
        int height = SLOT_ROWS * SLOT_SIZE;
        return new ProgressWidget().syncHandler(progressName).direction(ProgressWidget.Direction.UP)
            .texture(GTGuiTextures.SLOT_ITEM_STANDARD, texture, height).size(10, height)
            .tooltipDynamic(tooltip -> tooltip.add(StatCollector.translateToLocalFormatted(TOOLTIP_PREFIX + tooltipKey,
                formatNumber(stored.getIntValue()), formatNumber(maximum.getIntValue()), getFluidUnit())))
            .tooltipAutoUpdate(true);
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        int inputCount = SteamCropManager.SLOT_FERT_END - SteamCropManager.SLOT_WEEDEX_START + 1;
        int outputCount = SteamCropManager.SLOT_OUTPUT_END - SteamCropManager.SLOT_OUTPUT_START + 1;
        Grid inputs = new Grid().coverChildren().gridOfSizeWidth(inputCount, INPUT_COLUMNS,
            (x, y, index) -> createSlot(index + SteamCropManager.SLOT_WEEDEX_START));
        Grid outputs = new Grid().coverChildren().gridOfSizeWidth(outputCount, OUTPUT_COLUMNS,
            (x, y, index) -> createSlot(index + SteamCropManager.SLOT_OUTPUT_START));
        Flow tanks = Flow.row().coverChildren()
            .child(createTankBar(syncManager, machine::getWaterAmount, machine::getWaterCapacity, "water",
                CropsNHUITextures.PROGRESSBAR_CROP_MANAGER_WATER, "waterStorage"))
            .child(createTankBar(syncManager, machine::getWeedEXAmount, machine::getWeedEXCapacity, "weedEX",
                CropsNHUITextures.PROGRESSBAR_CROP_MANAGER_WEED_EX, "weedEXStorage"))
            .child(createTankBar(syncManager, machine::getLiquidFertilizerAmount, machine::getLiquidFertilizerCapacity,
                "fertilizer", CropsNHUITextures.PROGRESSBAR_CROP_MANAGER_LIQUID_FERTILIZER, "liquidFertilizerStorage"));
        LongSyncValue steamStored = new LongSyncValue(() -> machine.getSteamVar() * 2);
        LongSyncValue steamCapacity = new LongSyncValue(() -> machine.maxSteamStore() * 2);
        syncManager.syncValue("steamStored", steamStored);
        syncManager.syncValue("steamCapacity", steamCapacity);
        return super.createContentSection(panel, syncManager)
            .child(Flow.row().coverChildren().horizontalCenter().childPadding(3).child(inputs).child(tanks)
                .child(outputs).child(new SteamGaugeWidget(steamStored, steamCapacity)));
    }

    private ToggleButton createModeButton(String mode, UITexture overlay, BooleanSupplier enabled,
        Consumer<Boolean> setter) {
        BooleanSyncValue value = new BooleanSyncValue(enabled, state -> {
            if (!baseMetaTileEntity.isServerSide()) return;
            setter.accept(state);
            baseMetaTileEntity.markDirty();
        }).allowC2S();
        return new ToggleButton().name(mode).value(value).overlay(overlay)
            .tooltip(true,
                tooltip -> tooltip.addLine(IKey.lang(TOOLTIP_PREFIX + "toggle." + mode))
                    .addLine(IKey.lang(TOOLTIP_PREFIX + "toggle.enabled")))
            .tooltip(false, tooltip -> tooltip.addLine(IKey.lang(TOOLTIP_PREFIX + "toggle." + mode))
                .addLine(IKey.lang(TOOLTIP_PREFIX + "toggle.disabled")));
    }

    @Override
    protected Flow createBottomLeftCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        return super.createBottomLeftCornerFlow(panel, syncManager)
            .child(createModeButton("water", CropsNHUITextures.BUTTON_OVERLAY_TOGGLE_WATER, () -> machine.waterEnabled,
                value -> machine.waterEnabled = value))
            .child(createModeButton("weedEX", CropsNHUITextures.BUTTON_OVERLAY_TOGGLE_WEED_EX,
                () -> machine.weedEXEnabled, value -> machine.weedEXEnabled = value))
            .child(createModeButton("fertilizer", CropsNHUITextures.BUTTON_OVERLAY_TOGGLE_FERTILIZER,
                () -> machine.fertilizerEnabled, value -> machine.fertilizerEnabled = value))
            .child(createModeButton("harvest", CropsNHUITextures.BUTTON_OVERLAY_TOGGLE_HARVEST,
                () -> machine.harvestEnabled, value -> machine.harvestEnabled = value))
            .childIf(supportsPowerSwitch(), () -> createPowerSwitchButton().marginLeft(4));
    }
}
