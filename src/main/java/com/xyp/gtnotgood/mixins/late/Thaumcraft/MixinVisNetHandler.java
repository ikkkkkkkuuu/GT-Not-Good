package com.xyp.gtnotgood.mixins.late.Thaumcraft;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.config.Config;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.visnet.VisNetHandler;

/** Makes Thaumcraft vis drainage succeed without consuming node storage when configured. */
@Mixin(value = VisNetHandler.class, remap = false)
public class MixinVisNetHandler {

    @Inject(method = "drainVis", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private static void gtnotgood$infiniteVis(World world, int x, int y, int z, Aspect aspect, int amount,
        CallbackInfoReturnable<Integer> cir) {
        if (Config.tcInfiniteVis) {
            cir.setReturnValue(amount);
        }
    }
}
