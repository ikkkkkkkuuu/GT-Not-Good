package com.xyp.gtnotgood.mixins.late.gregtech.arcfurnace;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.common.items.electrode.IronElectrode;

import kubatech.tileentity.gregtech.hatch.MTEHatchElectrode;

@Mixin(value = MTEHatchElectrode.class, remap = false)
public abstract class ElectrodeHatchMixin {

    @Inject(method = "isItemValidForSlot", at = @At("HEAD"), cancellable = true)
    private void acceptIronElectrode(int index, ItemStack stack, CallbackInfoReturnable<Boolean> result) {
        if (stack != null && stack.getItem() instanceof IronElectrode) {
            result.setReturnValue(index == 0);
        }
    }
}
