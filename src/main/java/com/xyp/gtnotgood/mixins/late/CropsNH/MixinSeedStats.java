package com.xyp.gtnotgood.mixins.late.CropsNH;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.gtnewhorizon.cropsnh.farming.SeedStats;
import com.xyp.gtnotgood.config.Config;

/**
 * Forces generated CropsNH seed stats to maximum when enabled in config.
 */
@Mixin(SeedStats.class)
public abstract class MixinSeedStats {

    @ModifyVariable(method = "<init>(BBBZ)V", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private static byte gtnotgood$maxGrowth(byte original) {
        Config.ensureLoaded();
        return Config.enableCropMaxStats ? (byte) 31 : original;
    }

    @ModifyVariable(method = "<init>(BBBZ)V", at = @At("HEAD"), argsOnly = true, ordinal = 1, remap = false)
    private static byte gtnotgood$maxGain(byte original) {
        Config.ensureLoaded();
        return Config.enableCropMaxStats ? (byte) 31 : original;
    }

    @ModifyVariable(method = "<init>(BBBZ)V", at = @At("HEAD"), argsOnly = true, ordinal = 2, remap = false)
    private static byte gtnotgood$maxResistance(byte original) {
        Config.ensureLoaded();
        return Config.enableCropMaxStats ? (byte) 31 : original;
    }
}
