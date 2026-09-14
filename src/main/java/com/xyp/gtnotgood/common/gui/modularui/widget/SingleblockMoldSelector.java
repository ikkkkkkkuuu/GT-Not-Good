package com.xyp.gtnotgood.common.gui.modularui.widget;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.xyp.gtnotgood.common.compat.VirtualMachineMolds;
import com.xyp.gtnotgood.common.utils.MoldDataManager;

import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTGuis;
import gregtech.common.modularui2.widget.SlotLikeButtonWidget;

/** Server-validated selector for singleblock molds; no real inventory slot or transferable item is created. */
public final class SingleblockMoldSelector {

    private SingleblockMoldSelector() {}

    /** Builds a scrollable catalog selector, sharing the hatch mold list while keeping machine storage independent. */
    public static Widget<?> create(MTEBasicMachine machine, PanelSyncManager syncManager) {
        IntSyncValue selection = new IntSyncValue(
            () -> VirtualMachineMolds.indexOf(VirtualMachineMolds.get(machine)),
            index -> {
                if (syncManager.isClient() || index < -1 || index >= MoldDataManager.getMoldCount()) return;
                // Manual selection obeys the same empty/idle boundary as automatic configuration changes.
                if (machine.mMaxProgresstime > 0) return;
                for (int i = 0; i < machine.mInputSlotCount; i++) {
                    ItemStack input = machine.getStackInSlot(machine.getInputSlot() + i);
                    if (input != null && input.stackSize > 0) return;
                }
                if (machine.getFillableStack() != null && machine.getFillableStack().amount > 0) return;
                VirtualMachineMolds.set(machine, VirtualMachineMolds.at(index));
                machine.getBaseMetaTileEntity()
                    .markInventoryBeenModified();
                machine.getBaseMetaTileEntity()
                    .markDirty();
            }).allowC2S();
        syncManager.syncValue("gtngVirtualMold", selection);
        IPanelHandler popup = syncManager.syncedPanel("gtngMoldSelector", true, (mainPanel, player) -> {
            ModularPanel panel = GTGuis.createPopUpPanel("gtngMoldSelector")
                .size(176, 212);
            // #tr gtng.singleblock.mold.title
            // # Virtual Mold
            // # zh_CN 虚拟模具
            panel.child(
                IKey.lang("gtng.singleblock.mold.title")
                    .asWidget()
                    .pos(7, 7));
            panel.child(
                new SlotLikeButtonWidget(() -> VirtualMachineMolds.at(selection.getIntValue()))
                    .onMousePressed(button -> {
                        if (button == 1) selection.setIntValue(-1);
                        return true;
                    })
                    .horizontalCenter()
                    .top(24));
            ItemStack[] choices = MoldDataManager.getMolds();
            panel.child(
                new Grid().minColWidth(18)
                    .gridOfWidthHeight(9, (choices.length + 8) / 9, (x, y, index) -> {
                        if (index >= choices.length) return null;
                        return new SlotLikeButtonWidget(choices[index]).size(18)
                            .background(
                                new DynamicDrawable(
                                    () -> selection.getIntValue() == index ? GTGuiTextures.SLOT_ITEM_DARK
                                        : GTGuiTextures.SLOT_ITEM_STANDARD))
                            .onMousePressed(button -> {
                                selection.setIntValue(button == 0 ? index : -1);
                                return true;
                            });
                    })
                    .size(166, 162)
                    .scrollable()
                    .pos(5, 46));
            return panel;
        });
        return new SlotLikeButtonWidget(() -> VirtualMachineMolds.at(selection.getIntValue())) {

            @Override
            public Result onMousePressed(int button) {
                if (button == 0 && Interactable.hasShiftDown()) popup.openPanel();
                else if (button == 1 && Interactable.hasShiftDown()) selection.setIntValue(-1);
                else if (button == 0 || button == 1) cycle(button == 0 ? 1 : -1);
                return Result.SUCCESS;
            }

            @Override
            public boolean onMouseScroll(UpOrDown direction, int amount) {
                cycle(direction.modifier);
                return true;
            }

            private void cycle(int delta) {
                int next = selection.getIntValue() + delta;
                if (next < -1) next = MoldDataManager.getMoldCount() - 1;
                if (next >= MoldDataManager.getMoldCount()) next = -1;
                selection.setIntValue(next);
            }
        }.size(18)
            .background(GTGuiTextures.SLOT_ITEM_STANDARD, GTGuiTextures.OVERLAY_SLOT_MOLD)
            // #tr gtng.singleblock.mold.help
            // # Shift+click: select; scroll: cycle; Shift+right-click: clear
            // # zh_CN Shift+左键选择；滚轮切换；Shift+右键清空
            .addTooltipLine(IKey.lang("gtng.singleblock.mold.help"))
            // #tr gtng.singleblock.mold.idle
            // # Changes require an idle machine with empty inputs.
            // # zh_CN 机器停止且输入清空后可更换模具。
            .addTooltipLine(IKey.lang("gtng.singleblock.mold.idle"));
    }
}
