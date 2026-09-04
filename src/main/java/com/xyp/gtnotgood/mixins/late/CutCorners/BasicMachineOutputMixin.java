package com.xyp.gtnotgood.mixins.late.CutCorners;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.xyp.gtnotgood.config.Config;

import gregtech.api.metatileentity.implementations.MTEBasicMachine;

/**
 * 单方块机器(MTEBasicMachine)自动输出流体不再受每次 1000mB(1 桶)上限——仿 GTNH-CutCorners。
 * <p>
 * 原版 {@code onPostTick} 里自动输出流体走 {@code drain(1000, false)}，每 20 tick 最多输出 1000mB。
 * 配方大幅提速后产出速率远超这个上限会导致内部储罐堵塞。这里 {@link ModifyConstant} 把该 1000
 * 改为 {@link Integer#MAX_VALUE}，一次抽干内部储罐全部流体。
 * <p>
 * {@code onPostTick} 方法体内 1000 只出现这一处(已核实)，故 ModifyConstant 精确、不误伤。
 * 由 {@link Config#recipeSpeedFullFluidOutput} 控制，关闭时原样返回 1000。
 */
@Mixin(value = MTEBasicMachine.class, remap = false)
public class BasicMachineOutputMixin {

    @ModifyConstant(method = "onPostTick", constant = @Constant(intValue = 1000), remap = false)
    private int gtnotgood$modifyAutoOutputFluidAmount(int constant) {
        if (Config.recipeSpeedFullFluidOutput) {
            return Integer.MAX_VALUE;
        }
        return constant;
    }
}
