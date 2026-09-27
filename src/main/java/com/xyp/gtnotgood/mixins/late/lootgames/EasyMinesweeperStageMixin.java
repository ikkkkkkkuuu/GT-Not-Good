package com.xyp.gtnotgood.mixins.late.lootgames;

import net.minecraftforge.common.config.Configuration;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ru.timeconqueror.lootgames.common.config.ConfigMS;

/** Sets every newly snapshotted Minesweeper stage to the native minimum of a 5x5 board and one bomb. */
@Mixin(value = ConfigMS.StageConfig.class, remap = false)
public abstract class EasyMinesweeperStageMixin {

    @Shadow
    private int bombCount;

    @Shadow
    private int boardRadius;

    /**
     * Uses radius two rather than a smaller unsupported board that could break first-click safe generation.
     * Existing saved boards retain their snapshots so their physical field and serialized cells stay consistent.
     *
     * @param config native configuration being loaded
     * @param ci     completed stage initialization
     */
    @Inject(method = "init", at = @At("RETURN"), require = 1)
    private void gtng$lowestDifficulty(Configuration config, CallbackInfo ci) {
        boardRadius = 2;
        bombCount = 1;
    }
}
