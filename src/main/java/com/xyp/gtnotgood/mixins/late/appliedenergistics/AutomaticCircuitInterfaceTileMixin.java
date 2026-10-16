package com.xyp.gtnotgood.mixins.late.appliedenergistics;

import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.helpers.DualityInterface;
import appeng.tile.misc.TileInterface;
import appeng.util.Platform;

@Mixin(value = TileInterface.class, remap = false)
public abstract class AutomaticCircuitInterfaceTileMixin {

    @Shadow
    public abstract DualityInterface getInterfaceDuality();

    @Inject(method = "setSide", at = @At("RETURN"))
    private void gtng$refreshDirectedPatterns(ForgeDirection axis, CallbackInfo ci) {
        if (Platform.isServer()) getInterfaceDuality().updateCraftingList();
    }
}
