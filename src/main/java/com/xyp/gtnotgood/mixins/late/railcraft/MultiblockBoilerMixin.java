package com.xyp.gtnotgood.mixins.late.railcraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.config.Config;

import mods.railcraft.common.blocks.RailcraftTileEntity;
import mods.railcraft.common.blocks.machine.beta.TileBoilerFirebox;
import mods.railcraft.common.util.steam.SteamBoiler;

/**
 * Limits boiler boosts to multiblock fireboxes because locomotives and hobbyist engines share SteamBoiler.
 * Steam is multiplied after the native water calculation, preserving fuel use, cooling and dry-boiler behavior.
 */
@Mixin(value = SteamBoiler.class, remap = false)
public abstract class MultiblockBoilerMixin {

    @Shadow
    private RailcraftTileEntity tile;
    @Shadow
    private double heat;
    @Shadow
    private double maxHeat;
    @Shadow
    protected boolean isBurning;

    @Inject(method = "increaseHeat", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void gtnotgood$instantHeat(int numTanks, CallbackInfo ci) {
        if (gtnotgood$heatMultiblock()) ci.cancel();
    }

    @Inject(method = "convertSteam", at = @At("HEAD"), require = 1, allow = 1)
    private void gtnotgood$heatBeforeFirstConversion(int numTanks, CallbackInfoReturnable<Integer> cir) {
        gtnotgood$heatMultiblock();
    }

    private boolean gtnotgood$heatMultiblock() {
        if (!(tile instanceof TileBoilerFirebox) || !isBurning) return false;
        Config.ensureLoaded();
        if (!Config.railcraftBoilerInstantHeat) return false;
        heat = maxHeat;
        return true;
    }

    @ModifyArg(
        method = "convertSteam",
        at = @At(
            value = "INVOKE",
            target = "Lmods/railcraft/common/fluids/Fluids;get(I)Lnet/minecraftforge/fluids/FluidStack;"),
        index = 0,
        require = 1,
        allow = 1)
    private int gtnotgood$multiplySteam(int amount) {
        if (!(tile instanceof TileBoilerFirebox)) return amount;
        Config.ensureLoaded();
        return scaleSteamOutput(amount, Config.railcraftBoilerSteamMultiplier);
    }

    /**
     * Saturates large steam amounts so extreme settings cannot overflow a FluidStack.
     *
     * @param amount     native steam amount after water availability is checked
     * @param multiplier configured steam multiplier
     * @return boosted amount, bounded by the FluidStack integer limit
     */
    private static int scaleSteamOutput(int amount, float multiplier) {
        if (amount <= 0 || !Float.isFinite(multiplier) || multiplier < 1F) return amount;
        return (int) Math.min(Integer.MAX_VALUE, amount * (double) multiplier);
    }
}
