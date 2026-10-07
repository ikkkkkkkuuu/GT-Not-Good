package com.xyp.gtnotgood.mixins.late.cutcorners;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.xyp.gtnotgood.config.Config;

import ggfab.mte.MTEAdvAssLine;
import gregtech.common.tileentities.machines.multi.MTEAssemblyLine;

/**
 * Assembly-line recipes bypass RecipeMapBackend. Apply the duration policy when either controller reads the
 * recipe for overclocking, preserving the shared recipe and data-stick identity. Advanced lines read it inside
 * a compiler-generated lambda, so match the field read across methods rather than depending on the lambda name.
 */
@Mixin(value = { MTEAssemblyLine.class, MTEAdvAssLine.class }, remap = false)
public abstract class AssemblyLineSpeedMixin {

    @ModifyExpressionValue(
        method = "*",
        at = @At(value = "FIELD", target = "Lgregtech/api/util/GTRecipe$RecipeAssemblyLine;mDuration:I"),
        require = 1,
        allow = 1)
    private static int gtnotgood$modifyAssemblyLineDuration(int duration) {
        Config.ensureLoaded();
        return Config.getModifiedRecipeDuration(duration);
    }
}
