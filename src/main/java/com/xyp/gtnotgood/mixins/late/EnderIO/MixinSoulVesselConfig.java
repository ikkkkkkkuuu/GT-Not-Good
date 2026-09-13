package com.xyp.gtnotgood.mixins.late.EnderIO;

import net.minecraftforge.common.config.Configuration;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import crazypants.enderio.config.Config;

/**
 * Enables boss capture on both sides before integrations such as Mobs Info cache Soul Vial eligibility.
 * Reapplies the runtime setting whenever Ender IO processes its configuration, without modifying the config file.
 */
@Mixin(value = Config.class, remap = false)
public abstract class MixinSoulVesselConfig {

    /**
     * Overrides the configured boss restriction after all Ender IO configuration values have been read.
     *
     * @param config Ender IO configuration being processed
     * @param ci     callback for the completed configuration pass
     */
    @Inject(method = "processConfig", at = @At("RETURN"))
    private static void gtng$enableBossCapture(Configuration config, CallbackInfo ci) {
        Config.soulVesselCapturesBosses = true;
    }
}
