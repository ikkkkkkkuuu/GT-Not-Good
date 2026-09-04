package com.xyp.gtnotgood.mixins.late.CutCorners;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.config.Config;

import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.util.GTRecipe;

/**
 * Adjusts GregTech recipe durations while recipes are being registered.
 */
@Mixin(value = RecipeMapBackend.class, remap = false)
public class RecipeSpeedMixin {

    @Inject(method = "compileRecipe", at = @At("HEAD"))
    private void gtnotgood$modifyRecipeDuration(GTRecipe recipe, CallbackInfoReturnable<GTRecipe> cir) {
        Config.ensureLoaded();
        if (Config.recipeSpeedMode == 0 || recipe == null) return;
        recipe.mDuration = Config.getModifiedRecipeDuration(recipe.mDuration);
    }
}
