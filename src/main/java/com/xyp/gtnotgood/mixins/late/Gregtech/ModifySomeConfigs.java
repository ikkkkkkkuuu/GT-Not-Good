package com.xyp.gtnotgood.mixins.late.Gregtech;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnotgood.config.Config;

import gregtech.GTMod;
import gregtech.loaders.preload.GTPreLoad;

/**
 * Forces selected GregTech client display options after GregTech reads its client config.
 * <p>
 * This mixin runs during GregTech preload, so it asks this mod's config to load lazily before reading the toggles.
 */
@SuppressWarnings("UnusedMixin")
@Mixin(value = GTPreLoad.class, remap = false)
public class ModifySomeConfigs {

    @Inject(
        method = "loadClientConfig",
        at = @At(value = "INVOKE", target = "Lgregtech/common/GTProxy;reloadNEICache()V", shift = At.Shift.BEFORE),
        require = 1)
    private static void gtnotgood$modifyClientConfig(CallbackInfo ci) {
        Config.ensureLoaded();
        if (Config.enableAlwaysDisplayRecipeOwner) GTMod.gregtechproxy.mNEIRecipeOwner = true;
        if (Config.enableAlwaysDisplayWailaAverageNS) GTMod.gregtechproxy.wailaAverageNS = true;
        if (Config.enableAlwaysDisplayNEIOriginalVoltage) GTMod.gregtechproxy.mNEIOriginalVoltage = true;
    }
}
