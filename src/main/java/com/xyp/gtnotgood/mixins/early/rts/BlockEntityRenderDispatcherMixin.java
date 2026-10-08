package com.xyp.gtnotgood.mixins.early.rts;

import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.rtsbuilding.rtsbuilding.client.screen.culling.RtsCullingClientState;
import com.rtsbuilding.rtsbuilding.platform.math.BlockPos;

/**
 * 客户端范围剔除：隐藏盒内箱子、机器等方块实体渲染。
 */
@Mixin(TileEntityRendererDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {

    @Inject(method = "renderTileEntity(Lnet/minecraft/tileentity/TileEntity;F)V", at = @At("HEAD"), cancellable = true)
    private void rtsbuilding$skipCulledBlockEntity(TileEntity tileEntity, float partialTicks, CallbackInfo ci) {
        if (
            tileEntity != null && RtsCullingClientState
                .shouldCull(new BlockPos(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord))
        ) {
            ci.cancel();
        }
    }
}
