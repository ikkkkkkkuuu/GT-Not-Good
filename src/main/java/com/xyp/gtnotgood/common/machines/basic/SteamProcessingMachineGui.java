package com.xyp.gtnotgood.common.machines.basic;

import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;

import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.FluidSlotSyncHandler;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.gtnewhorizons.modularui.common.fluid.FluidStackTank;

import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.recipe.BasicUIProperties;
import gregtech.api.util.GTUtility;
import gregtech.common.gui.modularui.singleblock.MTEBasicMachineBronzeGui;

final class SteamProcessingMachineGui extends MTEBasicMachineBronzeGui {

    private final SteamProcessingMachine processingMachine;

    SteamProcessingMachineGui(SteamProcessingMachine machine, BasicUIProperties properties) {
        super(machine, properties);
        processingMachine = machine;
    }

    @Override
    protected void registerSyncValues(PanelSyncManager syncManager) {
        super.registerSyncValues(syncManager);
        syncManager.findSyncHandler("fluidAutoOutput", BooleanSyncValue.class)
            .changeListener(() -> { if (!syncManager.isClient()) baseMetaTileEntity.markDirty(); });
        syncManager.findSyncHandler("itemAutoOutput", BooleanSyncValue.class)
            .changeListener(() -> { if (!syncManager.isClient()) baseMetaTileEntity.markDirty(); });
        syncManager.syncValue("steamAutoOutputSide",
            new IntSyncValue(() -> processingMachine.getAutoOutputSide().ordinal()));
        syncManager.syncValue("steamAutoOutputCycle", new InteractionSyncHandler().setOnMousePressed(mouseData -> {
            if (
                !mouseData.isClient() && mouseData.shift && (mouseData.mouseButton == 0 || mouseData.mouseButton == 1)
            ) {
                processingMachine.cycleAutoOutputSide();
            }
        }));
    }

    @Override
    protected int getBasePanelHeight() {
        return 230;
    }

    @Override
    protected ParentWidget<?> createContentSection(ModularPanel panel, PanelSyncManager syncManager) {
        return super.createContentSection(panel, syncManager)
            .child(Flow.row().coverChildren().horizontalCenter().top(58).child(createFluidSlots(false))
                .child(new Widget<>().size(2 * SLOT_SIZE, 2 * SLOT_SIZE)).child(createFluidSlots(true)));
    }

    private ParentWidget<?> createFluidSlots(boolean output) {
        FluidStackTank[] tanks = output ? processingMachine.getFluidOutputTanks()
            : processingMachine.getFluidInputTanks();
        ParentWidget<?> slots = new ParentWidget<>().size(3 * SLOT_SIZE, 2 * SLOT_SIZE);
        for (int i = 0; i < tanks.length; i++) {
            FluidSlotSyncHandler handler = new FluidSlotSyncHandler(tanks[i]) {

                @Override
                protected void onValueChanged() {
                    super.onValueChanged();
                    if (!getSyncManager().isClient()) baseMetaTileEntity.markInventoryBeenModified();
                }
            }.canFillSlot(!output);
            slots.child(new FluidSlot().pos(i % 3 * SLOT_SIZE, i / 3 * SLOT_SIZE)
                .backgroundOverlay(slotOverlayFunction.apply(i, true, output, false)).syncHandler(handler));
        }
        return slots;
    }

    @Override
    protected Flow createBottomLeftCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        return super.createBottomLeftCornerFlow(panel, syncManager).child(createAutoOutputButton(true, syncManager))
            .child(createAutoOutputButton(false, syncManager));
    }

    private ToggleButton createAutoOutputButton(boolean fluid, PanelSyncManager syncManager) {
        InteractionSyncHandler outputCycle = syncManager.findSyncHandler("steamAutoOutputCycle",
            InteractionSyncHandler.class);
        IntSyncValue outputSide = syncManager.findSyncHandler("steamAutoOutputSide", IntSyncValue.class);
        return new ToggleButton() {

            @Override
            public Result onMousePressed(int mouseButton) {
                if (Interactable.hasShiftDown() && (mouseButton == 0 || mouseButton == 1)) {
                    outputCycle.onMousePressed(mouseButton);
                    playClickSound();
                    return Result.SUCCESS;
                }
                return super.onMousePressed(mouseButton);
            }
        }.name(fluid ? "auto_fluid_output" : "auto_item_output")
            .value(syncManager.findSyncHandler(fluid ? "fluidAutoOutput" : "itemAutoOutput", BooleanSyncValue.class))
            .overlay(
                fluid ? GTGuiTextures.OVERLAY_BUTTON_AUTOOUTPUT_FLUID : GTGuiTextures.OVERLAY_BUTTON_AUTOOUTPUT_ITEM)
            .tooltipDynamic(tooltip -> {
                tooltip.addLine(autoOutputName(fluid));
                ForgeDirection side = ForgeDirection.getOrientation(outputSide.getIntValue());
                String sideName = StatCollector.translateToLocal(GTUtility.getUnlocalizedSideName(side));
                // #tr gui.steam_processing.output_side
                // # Output side: %s
                // # zh_CN 输出面：%s
                tooltip.addLine(StatCollector.translateToLocalFormatted("gui.steam_processing.output_side", sideName));
                // #tr gui.steam_processing.output_side_cycle
                // # Shift-click to cycle the output side; the steam vent is excluded
                // # zh_CN Shift 点击切换输出面；自动跳过蒸汽排气面
                tooltip.addLine(StatCollector.translateToLocal("gui.steam_processing.output_side_cycle"));
            }).tooltipAutoUpdate(true).tooltipShowUpTimer(TOOLTIP_DELAY);
    }

    private static String autoOutputName(boolean fluid) {
        if (fluid) {
            // #tr gui.steam_processing.auto_fluid_output
            // # Automatic fluid output
            // # zh_CN 自动输出流体
            return StatCollector.translateToLocal("gui.steam_processing.auto_fluid_output");
        }
        // #tr gui.steam_processing.auto_item_output
        // # Automatic item output
        // # zh_CN 自动输出物品
        return StatCollector.translateToLocal("gui.steam_processing.auto_item_output");
    }

    @Override
    protected Flow createBottomRightCornerFlow(ModularPanel panel, PanelSyncManager syncManager) {
        return Flow.row().mainAxisAlignment(Alignment.MainAxis.END).reverseLayout(true).coverChildren().verticalCenter()
            .rightRel(0).childIf(doesAddGregTechLogo(), this::makeLogoWidget)
            .childIf(doesAddCircuitSlot(), () -> createCircuitSlot(syncManager))
            .childIf(doesAddSpecialSlot(), this::createSpecialSlot);
    }
}
