package com.xyp.gtnotgood.mixins.early.interaction;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(NetHandlerPlayServer.class)
public abstract class BlockReachPlacementMixin {

    @Shadow
    public EntityPlayerMP playerEntity;

    @ModifyArg(
        method = "processPlayerBlockPlacement(Lnet/minecraft/network/play/client/C08PacketPlayerBlockPlacement;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayerMP;getDistanceSq(DDD)D"),
        index = 1,
        require = 1,
        expect = 1,
        allow = 1)
    private double gtnotgood$placementEyeHeight(double blockCenterY) {
        return blockCenterY - playerEntity.getEyeHeight();
    }
}
