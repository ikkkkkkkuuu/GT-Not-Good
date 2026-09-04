package com.xyp.gtnotgood.mixins.late.Thaumcraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnotgood.config.Config;

import thaumcraft.common.tiles.TileInfusionMatrix;

/** Prevents infusion instability events while preserving normal infusion progress. */
@Mixin(value = TileInfusionMatrix.class, remap = false)
public abstract class MixinTileInfusionMatrix {

    @Inject(method = "craftCycle", at = @At("HEAD"), require = 1)
    private void gtnotgood$noInstability(CallbackInfo ci) {
        if (Config.tcInfusionNoInstability) {
            ((TileInfusionMatrix) (Object) this).instability = 0;
        }
    }
}
