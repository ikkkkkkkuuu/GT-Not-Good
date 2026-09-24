package com.xyp.gtnotgood.mixins.late.AppliedEnergistics.compact;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.common.machines.multiblock.AssemblerMatrix;

import appeng.api.util.IInterfaceViewable;
import appeng.container.implementations.ContainerInterfaceTerminal;

/** Honors the matrix's own visibility switch; AE2 otherwise reads an unused DualityInterface setting. */
@Mixin(value = ContainerInterfaceTerminal.class, remap = false)
public abstract class MatrixInterfaceTerminalVisibilityMixin {

    /** Leaves every other interface's visibility rules unchanged. */
    @Inject(method = "getTerminalVisibility", at = @At("HEAD"), cancellable = true)
    private static void gtng$matrixVisibility(IInterfaceViewable machine, CallbackInfoReturnable<Boolean> result) {
        if (machine instanceof AssemblerMatrix matrix) result.setReturnValue(matrix.shouldDisplay());
    }
}
