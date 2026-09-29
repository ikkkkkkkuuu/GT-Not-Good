package com.xyp.gtnotgood.mixins.late.AppliedEnergistics;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "appeng.client.gui.implementations.GuiInterfaceTerminal$InterfaceTerminalEntry", remap = false)
public interface InterfaceEntryViewportAccessor {

    @Accessor("rowSize")
    int getRowSize();
}
