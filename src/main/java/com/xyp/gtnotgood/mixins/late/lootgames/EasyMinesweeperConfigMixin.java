package com.xyp.gtnotgood.mixins.late.lootgames;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ru.timeconqueror.lootgames.common.config.ConfigMS;

/** Gives Minesweeper the maximum supported retry allowance and a no-guess board generator. */
@Mixin(value = ConfigMS.class, remap = false)
public abstract class EasyMinesweeperConfigMixin {

    /**
     * Overrides runtime difficulty after loading, including subsequent native configuration reloads.
     *
     * @param ci completed configuration initialization
     */
    @Inject(method = "init()V", at = @At("RETURN"), require = 1)
    private void gtng$lowestDifficulty(CallbackInfo ci) {
        ConfigMS config = (ConfigMS) (Object) this;
        config.attemptCount = Integer.MAX_VALUE;
        config.detonationTime = 600;
        config.boardLogic = 1;
    }
}
