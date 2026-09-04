package com.xyp.gtnotgood.mixins.late.CutCorners;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.xyp.gtnotgood.config.Config;

import gregtech.api.metatileentity.implementations.MTEBasicMachine;

/**
 * Lets GregTech single-block machines auto-output all available fluid instead of one bucket per transfer.
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
