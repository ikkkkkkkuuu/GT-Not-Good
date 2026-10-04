package com.xyp.gtnotgood.mixins.late.CutCorners;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.config.Config;

import gregtech.api.recipe.check.CheckRecipeResult;
import kubatech.api.implementations.KubaTechGTMultiBlockBase;
import kubatech.tileentity.gregtech.multiblock.MTEExtremeEntityCrusher;

/**
 * Mob recipes bypass RecipeMapBackend. Apply the duration policy after weapon, batch and overclock calculations,
 * including the ritual branch, so neither the minimum overclock duration nor batching can undo the speed setting.
 */
@Mixin(value = MTEExtremeEntityCrusher.class, remap = false)
public abstract class ExtremeEntityCrusherSpeedMixin extends KubaTechGTMultiBlockBase<MTEExtremeEntityCrusher> {

    protected ExtremeEntityCrusherSpeedMixin(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    @Inject(method = "checkProcessing", at = @At("RETURN"))
    private void gtnotgood$modifyMobProcessingDuration(CallbackInfoReturnable<CheckRecipeResult> cir) {
        if (!cir.getReturnValue()
            .wasSuccessful() || mMaxProgresstime <= 0) return;
        Config.ensureLoaded();
        mMaxProgresstime = Config.getModifiedRecipeDuration(mMaxProgresstime);
    }
}
