package com.xyp.gtnotgood.mixins.late.cutcorners;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.xyp.gtnotgood.config.Config;

import gregtech.common.tileentities.machines.multi.purification.MTEPurificationPlant;

/**
 * Purification units follow the plant's cycle instead of their recipe duration. Shorten the normal cycle before
 * the plant passes it to linked units, keeping their progress synchronized and leaving debug cycles unchanged.
 */
@Mixin(value = MTEPurificationPlant.class, remap = false)
public abstract class PurificationPlantSpeedMixin {

    @ModifyConstant(
        method = "startCycle",
        constant = @Constant(intValue = MTEPurificationPlant.CYCLE_TIME_TICKS),
        require = 1,
        allow = 1)
    private int gtnotgood$modifyPurificationCycleDuration(int duration) {
        Config.ensureLoaded();
        return Config.getModifiedRecipeDuration(duration);
    }
}
