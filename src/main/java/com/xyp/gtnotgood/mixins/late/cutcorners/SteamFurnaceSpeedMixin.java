package com.xyp.gtnotgood.mixins.late.cutcorners;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.xyp.gtnotgood.config.Config;

import gregtech.common.tileentities.machines.steam.MTESteamFurnaceBronze;
import gregtech.common.tileentities.machines.steam.MTESteamFurnaceSteel;

/** Single-block steam furnaces bypass the furnace recipe backend. */
@Mixin(value = { MTESteamFurnaceBronze.class, MTESteamFurnaceSteel.class }, remap = false)
public abstract class SteamFurnaceSpeedMixin {

    @ModifyConstant(
        method = "checkRecipe",
        constant = { @Constant(intValue = 256), @Constant(intValue = 128) },
        require = 1,
        allow = 1)
    private int gtnotgood$modifySteamFurnaceDuration(int duration) {
        Config.ensureLoaded();
        return Config.getModifiedRecipeDuration(duration);
    }
}
