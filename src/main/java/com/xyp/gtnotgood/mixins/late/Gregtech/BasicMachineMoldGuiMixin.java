package com.xyp.gtnotgood.mixins.late.Gregtech;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.xyp.gtnotgood.common.compat.VirtualMachineMolds;
import com.xyp.gtnotgood.common.gui.modularui.widget.SingleblockMoldSelector;

import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.metatileentity.implementations.MTETieredMachineBlock;
import gregtech.common.gui.modularui.singleblock.base.MTETieredMachineBlockBaseGui;

/** Stacks a virtual mold above the native circuit slot and reserves another row in standard singleblock GUIs. */
@Mixin(value = MTETieredMachineBlockBaseGui.class, remap = false)
public abstract class BasicMachineMoldGuiMixin {

    @Shadow
    @Final
    protected MTETieredMachineBlock machine;

    @Inject(method = "createCircuitSlot", at = @At("RETURN"), cancellable = true)
    private void gtng$addMoldSlot(PanelSyncManager syncManager, CallbackInfoReturnable<Widget<?>> cir) {
        if (machine instanceof MTEBasicMachine basic && VirtualMachineMolds.supports(basic)) {
            cir.setReturnValue(
                Flow.column()
                    .coverChildren()
                    .child(SingleblockMoldSelector.create(basic, syncManager))
                    .child(cir.getReturnValue()));
        }
    }

    @Inject(method = "getBasePanelHeight", at = @At("RETURN"), cancellable = true)
    private void gtng$makeRoomForMold(CallbackInfoReturnable<Integer> cir) {
        if (machine instanceof MTEBasicMachine basic && VirtualMachineMolds.supports(basic))
            cir.setReturnValue(cir.getReturnValue() + 18);
    }
}
