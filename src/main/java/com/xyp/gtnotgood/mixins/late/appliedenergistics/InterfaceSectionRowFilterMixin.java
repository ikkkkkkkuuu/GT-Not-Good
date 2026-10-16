package com.xyp.gtnotgood.mixins.late.appliedenergistics;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xyp.gtnotgood.ae2thing.quickterminal.client.InterfacePatternRows;
import com.xyp.gtnotgood.ae2thing.quickterminal.client.InterfaceRowFilter;

import appeng.client.gui.implementations.GuiInterfaceTerminal;

@Mixin(targets = "appeng.client.gui.implementations.GuiInterfaceTerminal$InterfaceSection", remap = false)
public abstract class InterfaceSectionRowFilterMixin {

    @Shadow
    @Final
    private GuiInterfaceTerminal this$0;

    @ModifyExpressionValue(
        method = "refreshVisible",
        at = @At(
            value = "FIELD",
            target = "Lappeng/core/AEConfig;showOnlyInterfacesWithFreeSlotsInInterfaceTerminal:Z"),
        require = 1)
    private boolean keepLastFullRow(boolean original) {
        return original && !(this$0 instanceof InterfaceRowFilter);
    }

    @WrapOperation(
        method = "getEntriesHeight",
        at = @At(
            value = "FIELD",
            target = "Lappeng/client/gui/implementations/GuiInterfaceTerminal$InterfaceTerminalEntry;guiHeight:I"),
        require = 1)
    private int visibleHeight(@Coerce Object entry, Operation<Integer> original) {
        return ((InterfacePatternRows) entry).getVisibleHeight();
    }
}
