package com.xyp.gtnotgood.mixins.late.gregtech;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtnotgood.config.MainConfig;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.tileentities.machines.multi.compressor.MTEHIPCompressor;

@Mixin(value = MTEHIPCompressor.class, remap = false)
public abstract class HipCompressorHeatMixin {

    @Shadow
    private boolean overheated;

    @Inject(method = "onPostTick", at = @At("HEAD"), require = 1, allow = 1)
    private void gtnotgood$clearOverheated(IGregTechTileEntity tile, long tick, CallbackInfo ci) {
        if (tile.isClientSide()) return;
        MainConfig.ensureLoaded();
        if (MainConfig.hipCompressorNoOverheat) overheated = false;
    }

    @Redirect(
        method = { "onPostTick", "loadNBTData" },
        at = @At(
            value = "FIELD",
            target = "Lgregtech/common/tileentities/machines/multi/compressor/MTEHIPCompressor;overheated:Z",
            opcode = Opcodes.PUTFIELD),
        require = 2,
        allow = 2)
    private void gtnotgood$preventOverheating(MTEHIPCompressor compressor, boolean value) {
        MainConfig.ensureLoaded();
        overheated = !MainConfig.hipCompressorNoOverheat && value;
    }
}
