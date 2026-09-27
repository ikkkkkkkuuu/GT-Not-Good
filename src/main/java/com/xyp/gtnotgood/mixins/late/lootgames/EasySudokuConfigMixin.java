package com.xyp.gtnotgood.mixins.late.lootgames;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ru.timeconqueror.lootgames.common.config.ConfigSudoku;

/** Uses Sudoku's supported zero-blank setting for all four reward stages. */
@Mixin(value = ConfigSudoku.class, remap = false)
public abstract class EasySudokuConfigMixin {

    /**
     * Supplies completed puzzles that still use the normal submission and reward flow.
     * The settings are copied by native snapshots and synchronized by LootGames with new boards.
     *
     * @param ci completed configuration initialization
     */
    @Inject(method = "init()V", at = @At("RETURN"), require = 1)
    private void gtng$lowestDifficulty(CallbackInfo ci) {
        ConfigSudoku config = (ConfigSudoku) (Object) this;
        config.attemptCount = Integer.MAX_VALUE;
        config.clearOnWrongAnswer = ConfigSudoku.ClearOnWrongAnswer.WRONG_ONLY;
        config.hintDuplicates = true;
        config.hintOverflow = true;
        config.hintCompletedDigit = true;
        config.hintCompletedSection = true;
        config.level1.blanksCount = 0;
        config.level2.blanksCount = 0;
        config.level3.blanksCount = 0;
        config.level4.blanksCount = 0;
    }
}
