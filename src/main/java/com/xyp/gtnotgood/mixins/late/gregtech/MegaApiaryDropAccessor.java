package com.xyp.gtnotgood.mixins.late.gregtech;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import gregtech.api.util.GTUtility.ItemId;

@Mixin(targets = "kubatech.tileentity.gregtech.multiblock.MTEMegaIndustrialApiary$BeeSimulator$BeeDrop", remap = false)
public interface MegaApiaryDropAccessor {

    @Accessor("id")
    ItemId gtng$getDropId();
}
