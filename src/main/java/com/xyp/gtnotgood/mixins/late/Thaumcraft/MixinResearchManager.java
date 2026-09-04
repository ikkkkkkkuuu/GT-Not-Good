package com.xyp.gtnotgood.mixins.late.Thaumcraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.config.Config;

import thaumcraft.common.lib.research.ResearchManager;

/** Makes Thaumcraft report all research keys as completed when configured. */
@Mixin(value = ResearchManager.class, remap = false)
public class MixinResearchManager {

    @Inject(method = "isResearchComplete", at = @At("HEAD"), cancellable = true, remap = false)
    private static void gtnotgood$unlockAllResearch(String playername, String key,
        CallbackInfoReturnable<Boolean> cir) {
        if (Config.tcUnlockAllResearch) {
            cir.setReturnValue(true);
        }
    }
}
