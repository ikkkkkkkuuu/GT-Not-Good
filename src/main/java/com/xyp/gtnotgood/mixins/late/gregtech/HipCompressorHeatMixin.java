package com.xyp.gtnotgood.mixins.late.gregtech;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import gregtech.common.tileentities.machines.multi.compressor.MTEHIPCompressor;

@Mixin(value = MTEHIPCompressor.class, remap = false)
public abstract class HipCompressorHeatMixin {

    @Shadow
    private boolean overheated;

    @Redirect(
        method = { "onPostTick", "loadNBTData", "onValueUpdate" },
        at = @At(
            value = "FIELD",
            target = "Lgregtech/common/tileentities/machines/multi/compressor/MTEHIPCompressor;overheated:Z",
            opcode = Opcodes.PUTFIELD),
        require = 3,
        allow = 3)
    private void gtnotgood$preventOverheating(MTEHIPCompressor compressor, boolean value) {
        overheated = false;
    }
}
