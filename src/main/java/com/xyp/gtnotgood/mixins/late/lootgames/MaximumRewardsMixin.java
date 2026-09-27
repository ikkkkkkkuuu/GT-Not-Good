package com.xyp.gtnotgood.mixins.late.lootgames;

import java.util.Random;

import net.minecraft.inventory.IInventory;
import net.minecraft.util.WeightedRandomChestContent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.xyp.gtnotgood.common.compat.LootGamesMaximumRewards;

import ru.timeconqueror.lootgames.api.util.RewardUtils;
import ru.timeconqueror.lootgames.common.config.base.RewardConfig;
import ru.timeconqueror.lootgames.common.config.base.StagedRewardConfig;

/** Maximizes the shared server-side reward path for all three LootGames minigames. */
@Mixin(value = RewardUtils.class, remap = false)
public abstract class MaximumRewardsMixin {

    /**
     * Awards four chests whenever the native game invokes its reward settlement.
     * This does not create rewards for a loss that never invokes settlement.
     *
     * @param level original earned reward level
     * @return highest supported reward level
     */
    @ModifyVariable(method = "spawnFourStagedReward", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1)
    private static int gtng$maximumLevel(int level) {
        return 4;
    }

    /**
     * Uses each game's fourth-stage configuration, preserving dimension-specific loot-table overrides.
     *
     * @param config reward configuration of the current game
     * @param index  original chest index
     * @return highest-stage reward configuration
     */
    @Redirect(
        method = "spawnFourStagedReward",
        at = @At(
            value = "INVOKE",
            target = "Lru/timeconqueror/lootgames/common/config/base/StagedRewardConfig$FourStagedRewardConfig;getStageByIndex(I)Lru/timeconqueror/lootgames/common/config/base/RewardConfig;"),
        require = 1)
    private static RewardConfig gtng$highestTier(StagedRewardConfig.FourStagedRewardConfig config, int index) {
        return config.getStage4();
    }

    /**
     * Fills every slot instead of increasing random writes which can overwrite previously generated loot.
     *
     * @param random native loot RNG
     * @param loot   selected fourth-stage loot pool
     * @param chest  newly generated chest
     * @param count  original random draw count
     */
    @Redirect(
        method = "spawnLootChest",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/WeightedRandomChestContent;generateChestContents(Ljava/util/Random;[Lnet/minecraft/util/WeightedRandomChestContent;Lnet/minecraft/inventory/IInventory;I)V",
            remap = true),
        require = 1)
    private static void gtng$fillChest(Random random, WeightedRandomChestContent[] loot, IInventory chest, int count) {
        LootGamesMaximumRewards.fillChest(random, loot, chest);
    }
}
