package com.xyp.gtnotgood.mixins.late.Accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import gregtech.common.tileentities.machines.multi.purification.MTEPurificationUnitPhAdjustment;

@Mixin(value = MTEPurificationUnitPhAdjustment.class, remap = false)
public interface Grade4WaterPurificationAccessor {

    @Accessor("currentpHValue")
    float getCurrentpHValue();

    @Accessor("currentpHValue")
    void setCurrentpHValue(float value);
}
