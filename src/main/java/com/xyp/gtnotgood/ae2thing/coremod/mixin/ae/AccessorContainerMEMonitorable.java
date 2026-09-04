package com.xyp.gtnotgood.ae2thing.coremod.mixin.ae;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import appeng.api.storage.ITerminalHost;
import appeng.container.implementations.ContainerMEMonitorable;

@Mixin(ContainerMEMonitorable.class)
public interface AccessorContainerMEMonitorable {

    @Accessor(value = "host", remap = false)
    ITerminalHost getHost();

}
