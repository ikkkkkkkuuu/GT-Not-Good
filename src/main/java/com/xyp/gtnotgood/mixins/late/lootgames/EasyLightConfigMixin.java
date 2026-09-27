package com.xyp.gtnotgood.mixins.late.lootgames;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ru.timeconqueror.lootgames.common.config.ConfigGOL;

/** Applies the easiest supported light-game settings after every native configuration load. */
@Mixin(value = ConfigGOL.class, remap = false)
public abstract class EasyLightConfigMixin {

    /**
     * Keeps all four reward stages, with one round each and a cumulative sequence of one to four symbols.
     * The timeout is capped at the largest whole seconds value whose conversion to ticks cannot overflow.
     * Configuration files and reward settings are left under LootGames' ownership.
     *
     * @param ci completed configuration initialization
     */
    @Inject(method = "init()V", at = @At("RETURN"), require = 1)
    private void gtng$lowestDifficulty(CallbackInfo ci) {
        ConfigGOL config = (ConfigGOL) (Object) this;
        config.startDigitAmount = 1;
        config.attemptCount = Integer.MAX_VALUE;
        config.expandFieldAtStage = 0;
        config.explodeOnFail = false;
        config.zombiesOnFail = false;
        config.lavaOnFail = false;
        config.timeout = Integer.MAX_VALUE / 20;
        for (int stage = 0; stage < 4; stage++) {
            ConfigGOL.StageConfig settings = config.getStageByIndex(stage);
            settings.rounds = 1;
            settings.randomizeSequence = false;
            settings.displayTime = 40;
        }
    }
}
