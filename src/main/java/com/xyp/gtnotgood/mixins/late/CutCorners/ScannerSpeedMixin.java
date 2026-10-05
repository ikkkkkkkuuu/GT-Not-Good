package com.xyp.gtnotgood.mixins.late.CutCorners;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.xyp.gtnotgood.config.Config;

import gregtech.common.tileentities.machines.basic.MTEScanner;

/**
 * Scanner handlers generate runtime results independently of RecipeMapBackend. Scale the duration only when
 * the scanner reads it for overclocking, after validity and research voltage checks, leaving cached results intact.
 */
@Mixin(value = MTEScanner.class, remap = false)
public abstract class ScannerSpeedMixin {

    @ModifyExpressionValue(
        method = "checkRecipe",
        at = @At(value = "FIELD", target = "Lgregtech/api/util/GTScannerResult;duration:I"),
        require = 1,
        allow = 1)
    private int gtnotgood$modifyScannerDuration(int duration) {
        Config.ensureLoaded();
        return Config.getModifiedRecipeDuration(duration);
    }
}
