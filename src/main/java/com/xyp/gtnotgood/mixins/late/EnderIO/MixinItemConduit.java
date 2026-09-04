package com.xyp.gtnotgood.mixins.late.EnderIO;

import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import crazypants.enderio.conduit.AbstractConduit;
import crazypants.enderio.conduit.item.IItemConduit;
import crazypants.enderio.conduit.item.ItemConduit;

/**
 * 物品管道 Mixin 类
 * <p>
 * 用于修改 {@link ItemConduit} 物品的提取速度和传输间隔，实现超高速物品传输
 */
@Mixin(value = ItemConduit.class, remap = false)
public abstract class MixinItemConduit extends AbstractConduit implements IItemConduit {

    /**
     * 修改每次最大提取物品数量
     * <p>
     * 将物品管道的最大提取数量设置为 Integer 最大值，实现瞬间大量物品提取
     *
     * @param dir 提取方向
     * @param cir 回调信息返回对象，用于修改方法返回值
     */
    @Inject(method = "getMaximumExtracted", at = @At("HEAD"), cancellable = true)
    public void getMaximumExtractedFast(ForgeDirection dir, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(2147483647);
    }

    /**
     * 修改每个物品的传输 tick 间隔
     * <p>
     * 将物品传输间隔设置为极小值，实现超高速物品传输
     *
     * @param dir 传输方向
     * @param cir 回调信息返回对象，用于修改方法返回值
     */
    @Inject(method = "getTickTimePerItem", at = @At("HEAD"), cancellable = true)
    public void getTickTimePerItemFast(ForgeDirection dir, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(0.000001f);
    }
}
