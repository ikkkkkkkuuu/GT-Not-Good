package com.xyp.gtnotgood.common.gui.modularui.multiblock;

import static com.gtnewhorizon.cropsnh.tileentity.multi.MTEIndustrialFarm.SLOT_SUB_SOIL;
import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;
import static net.minecraft.util.StatCollector.translateToLocal;
import static net.minecraft.util.StatCollector.translateToLocalFormatted;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.SlotGroup;
import com.gtnewhorizon.cropsnh.tileentity.multi.MTEIndustrialFarmGui;
import com.xyp.gtnotgood.common.machines.multiblock.LargeIndustrialFarm;

import gregtech.api.modularui2.GTGuiTextures;

public final class LargeIndustrialFarmGui extends MTEIndustrialFarmGui {

    private final LargeIndustrialFarm farm;

    public LargeIndustrialFarmGui(LargeIndustrialFarm farm) {
        super(farm);
        this.farm = farm;
        withMachineModeIcons(GTGuiTextures.OVERLAY_BUTTON_ALLOW_INPUT, GTGuiTextures.OVERLAY_BUTTON_CYCLIC,
            GTGuiTextures.OVERLAY_BUTTON_ALLOW_OUTPUT);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager sync) {
        super.registerSyncValues(sync);
        sync.syncValue("selectedCrop", new IntSyncValue(farm::getSelectedCrop, farm::syncSelectedCrop));
        sync.syncValue("usedSeedCount", new IntSyncValue(farm::getUsedSeedCount, farm::syncUsedSeedCount));
        sync.syncValue("previousCrop", cropSelection(-1));
        sync.syncValue("nextCrop", cropSelection(1));
    }

    private InteractionSyncHandler cropSelection(int direction) {
        return new InteractionSyncHandler().setOnMousePressed(mouse -> {
            if (mouse.side.isServer() && mouse.mouseButton == 0)
                farm.setSelectedCrop(farm.getSelectedCrop() + direction);
        });
    }

    @Override
    protected Flow createLeftPanelGapRow(ModularPanel panel, PanelSyncManager sync) {
        Flow row = super.createLeftPanelGapRow(panel, sync);
        for (int index = row.getChildren().size() - 1; index >= 0; index--) {
            IWidget child = row.getChildren().get(index);
            if (
                child instanceof ItemSlot slot && slot.getSlot().getItemHandler() == farm.ifStackHandler
                    && slot.getSlot().getSlotIndex() == SLOT_SUB_SOIL
            ) row.remove(index);
        }
        IntSyncValue selected = sync.findSyncHandler("selectedCrop", IntSyncValue.class);
        ButtonWidget<?> previous = new ButtonWidget<>().size(12, 18).overlay(IKey.str("<")).syncHandler("previousCrop")
            // #tr gui.gtnotgood.industrial_farm.previous_crop
            // # Previous seed type (stop the farm to switch)
            // # zh_CN 上一种种子（停机后切换）
            .tooltipBuilder(t -> t.addLine(translateToLocal("gui.gtnotgood.industrial_farm.previous_crop")))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
        TextWidget<?> counter = new TextWidget<>(IKey.dynamic(() -> selected.getIntValue() + 1 + "/36")).size(28, 18)
            .scale(0.75f).tooltipBuilder(t -> t.addLine(IKey.dynamic(() -> {
                // #tr gui.gtnotgood.industrial_farm.selected_crop
                // # Seed type: %s / 36
                // # zh_CN 种子种类：%s / 36
                return translateToLocalFormatted("gui.gtnotgood.industrial_farm.selected_crop",
                    selected.getIntValue() + 1);
            }))).tooltipShowUpTimer(TOOLTIP_DELAY);
        ButtonWidget<?> next = new ButtonWidget<>().size(12, 18).overlay(IKey.str(">")).syncHandler("nextCrop")
            // #tr gui.gtnotgood.industrial_farm.next_crop
            // # Next seed type (stop the farm to switch)
            // # zh_CN 下一种种子（停机后切换）
            .tooltipBuilder(t -> t.addLine(translateToLocal("gui.gtnotgood.industrial_farm.next_crop")))
            .tooltipShowUpTimer(TOOLTIP_DELAY);
        return row.child(previous).child(counter).child(next);
    }

    @Override
    protected Flow createButtonColumn(ModularPanel panel, PanelSyncManager sync) {
        Flow column = super.createButtonColumn(panel, sync);
        SlotGroup controller = new SlotGroup("farmController", 1, false);
        sync.registerSlotGroup(controller);
        for (IWidget child : column.getChildren()) {
            if (child instanceof ItemSlot slot) slot.getSlot().slotGroup(controller);
        }
        return column;
    }
}
