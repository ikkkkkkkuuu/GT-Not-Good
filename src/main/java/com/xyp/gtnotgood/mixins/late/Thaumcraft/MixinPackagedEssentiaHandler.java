package com.xyp.gtnotgood.mixins.late.Thaumcraft;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.common.packaged.DirectEssentiaSupply;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.events.EssentiaHandler;

/** Overrides only explicitly tagged Provider jobs; ordinary TC4 source discovery remains unchanged. */
@Mixin(value = EssentiaHandler.class, remap = false)
public abstract class MixinPackagedEssentiaHandler {

    @Inject(
        method = "drainEssentia(Lnet/minecraft/tileentity/TileEntity;Lthaumcraft/api/aspects/Aspect;Lnet/minecraftforge/common/util/ForgeDirection;I)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 1)
    private static void gtnotgood$directEssentia(TileEntity tile, Aspect aspect, ForgeDirection side, int range,
        CallbackInfoReturnable<Boolean> callback) {
        Boolean result = DirectEssentiaSupply.drain(tile, aspect);
        if (result != null) callback.setReturnValue(result);
    }
}
