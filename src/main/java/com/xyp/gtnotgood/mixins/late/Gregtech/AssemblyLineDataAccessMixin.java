package com.xyp.gtnotgood.mixins.late.Gregtech;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.xyp.gtnotgood.common.packaged.AssemblyLineDataAccess;

import ggfab.mte.MTEAdvAssLine;
import gregtech.api.metatileentity.implementations.MTEHatchDataAccess;

/** Exposes recipe authorization without changing the advanced assembly line's processing. */
@Mixin(value = MTEAdvAssLine.class, remap = false)
public interface AssemblyLineDataAccessMixin extends AssemblyLineDataAccess {

    @Override
    @Accessor("mDataAccessHatches")
    List<MTEHatchDataAccess> gtnotgood$getDataAccessHatches();
}
