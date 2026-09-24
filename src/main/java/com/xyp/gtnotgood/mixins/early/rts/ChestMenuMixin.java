package com.xyp.gtnotgood.mixins.early.rts;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.rtsbuilding.rtsbuilding.compat.remote.RtsRemoteMenuCompat;

/** Retains the real chest container while honoring the server's marked RTS remote window. */
@Mixin(ContainerChest.class)
abstract class ChestMenuMixin {

    @Inject(method = "canInteractWith", at = @At("HEAD"), cancellable = true)
    private void rtsbuilding$forceRemoteStillValid(EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (RtsRemoteMenuCompat.shouldForceStillValid((Container) (Object) this, player)) {
            cir.setReturnValue(true);
        }
    }
}
