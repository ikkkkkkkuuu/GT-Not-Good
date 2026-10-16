package com.xyp.gtnotgood.mixins.late.appliedenergistics;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xyp.gtnotgood.ae2thing.quickterminal.client.InterfacePatternRows;

import appeng.client.gui.implementations.GuiInterfaceTerminal;

@Mixin(value = GuiInterfaceTerminal.class, remap = false)
public abstract class InterfaceTerminalRowFilterMixin {

    @WrapOperation(
        method = { "drawSection", "drawEntry" },
        at = @At(
            value = "FIELD",
            target = "Lappeng/client/gui/implementations/GuiInterfaceTerminal$InterfaceTerminalEntry;rows:I"),
        require = 3)
    private int visibleRows(@Coerce Object entry, Operation<Integer> original) {
        return ((InterfacePatternRows) entry).getVisibleRowCount();
    }

    @ModifyVariable(method = "drawEntry", at = @At("STORE"), name = "slotIdx", require = 2)
    private int actualSlot(int slot, @Coerce Object entry, int viewY, int titleBottom, int mouseX, int mouseY) {
        int actual = ((InterfacePatternRows) entry).getActualSlot(slot);
        return actual < 0 ? Integer.MAX_VALUE : actual;
    }
}
