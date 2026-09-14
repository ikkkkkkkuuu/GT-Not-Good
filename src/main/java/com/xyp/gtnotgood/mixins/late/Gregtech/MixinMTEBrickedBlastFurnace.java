package com.xyp.gtnotgood.mixins.late.Gregtech;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.utils.Utils;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.tileentities.machines.multi.MTEBrickedBlastFurnace;

/**
 * Enables sided automation through the brick blast furnace controller while keeping input and output slots separate.
 * Existing cover restrictions and stack compatibility checks remain enforced by GregTech's inventory interface.
 */
@Mixin(value = MTEBrickedBlastFurnace.class, remap = false)
public abstract class MixinMTEBrickedBlastFurnace {

    /** Replaces the upstream manual-only tooltip to describe the enabled controller automation. */
    @ModifyConstant(
        method = "getTooltip",
        constant = @Constant(stringValue = "All input/output is done manually through the controller"),
        require = 1)
    private String gtng$automationTooltip(String original) {
        Config.ensureLoaded();
        if (!Config.enableBrickedBlastFurnaceAutomation) return original;
        // #tr gtng.bbf.automation
        // # Supports automated item input/output through the controller
        // # zh_CN 支持通过控制器自动输入和输出物品
        return Utils.tr("gtng.bbf.automation");
    }

    /** Allows insertion into the three recipe input slots from any side of the controller. */
    @Inject(method = "allowPutStack", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtng$allowInput(IGregTechTileEntity tile, int index, ForgeDirection side, ItemStack stack,
        CallbackInfoReturnable<Boolean> cir) {
        if (!Config.enableBrickedBlastFurnaceAutomation) return;
        cir.setReturnValue(index >= 0 && index < MTEBrickedBlastFurnace.INPUT_SLOTS);
    }

    /** Allows extraction only from product slots, preventing automation from removing recipe ingredients. */
    @Inject(method = "allowPullStack", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtng$allowOutput(IGregTechTileEntity tile, int index, ForgeDirection side, ItemStack stack,
        CallbackInfoReturnable<Boolean> cir) {
        if (!Config.enableBrickedBlastFurnaceAutomation) return;
        cir.setReturnValue(
            index >= MTEBrickedBlastFurnace.INPUT_SLOTS
                && index < MTEBrickedBlastFurnace.INPUT_SLOTS + MTEBrickedBlastFurnace.OUTPUT_SLOTS);
    }
}
