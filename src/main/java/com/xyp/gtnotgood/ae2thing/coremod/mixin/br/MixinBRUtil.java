package com.xyp.gtnotgood.ae2thing.coremod.mixin.br;

import static com.xyp.gtnotgood.ae2thing.util.BRUtil.getIngredients;
import static com.xyp.gtnotgood.ae2thing.util.BRUtil.sendToServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blockrenderer6343.client.renderer.WorldSceneRenderer;
import blockrenderer6343.client.utils.BRUtil;

/**
 * Redirects BlockRenderer6343's "?" (NEI overlay) button in the multiblock structure preview: when the current recipe
 * screen was opened from one of our pattern terminals, pack the structure's blocks into the terminal instead of
 * BlockRenderer6343's own (NEE) overlay handling.
 */
@Mixin(value = BRUtil.class, remap = false)
public class MixinBRUtil {

    @Inject(method = "neiOverlay", at = @At(value = "HEAD"), remap = false, cancellable = true)
    private static void ae2thing$neiOverlay(WorldSceneRenderer renderer, CallbackInfo ci) {
        if (sendToServer(getIngredients(renderer))) {
            ci.cancel();
        }
    }
}
