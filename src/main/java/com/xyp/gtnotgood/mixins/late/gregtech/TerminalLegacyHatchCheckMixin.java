package com.xyp.gtnotgood.mixins.late.gregtech;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.common.items.multiblockterminal.TerminalOperations;

// GT5 5.09.54.205: the check-only ofHatchAdder(IGTHatchAdder, int, Block, int) element.
@Mixin(targets = "gregtech.api.util.GTStructureUtility$5", remap = false)
public abstract class TerminalLegacyHatchCheckMixin {

    @Inject(
        method = "check(Ljava/lang/Object;Lnet/minecraft/world/World;III)Z",
        at = @At("RETURN"),
        require = 1,
        allow = 1)
    private void gtnotgood$captureAcceptedHatch(Object context, World world, int x, int y, int z,
        CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) TerminalOperations.captureAcceptedHatch(context, world, x, y, z);
    }
}
