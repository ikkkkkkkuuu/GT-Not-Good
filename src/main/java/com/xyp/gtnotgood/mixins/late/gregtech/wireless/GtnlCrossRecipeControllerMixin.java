package com.xyp.gtnotgood.mixins.late.gregtech.wireless;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnotgood.common.wireless.WirelessControllerAccess;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

@Pseudo
@Mixin(targets = "com.science.gtnl.common.machine.multiMachineBase.MultiMachineBase", remap = false)
public abstract class GtnlCrossRecipeControllerMixin {

    @Inject(
        method = "runMachine(Lgregtech/api/interfaces/tileentity/IGregTechTileEntity;J)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0)
    private void gtng$run(IGregTechTileEntity tile, long tick, CallbackInfo ci) {
        if (
            ((WirelessControllerAccess) this).gtng$getWirelessScheduler().tick((MTEMultiBlockBase) (Object) this, tile,
                tick)
        ) ci.cancel();
    }
}
