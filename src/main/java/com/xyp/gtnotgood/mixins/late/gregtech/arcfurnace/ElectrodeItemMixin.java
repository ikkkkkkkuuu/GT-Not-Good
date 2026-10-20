package com.xyp.gtnotgood.mixins.late.gregtech.arcfurnace;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.common.items.electrode.IronElectrode;

import kubatech.loaders.ArcFurnaceElectrode;
import kubatech.loaders.item.arcfurnace.ElectrodeItem;

@Mixin(value = ElectrodeItem.class, remap = false)
public abstract class ElectrodeItemMixin {

    @Inject(method = "getElectrodeFromStack", at = @At("HEAD"), cancellable = true)
    private void getIronElectrode(ItemStack stack, CallbackInfoReturnable<ArcFurnaceElectrode> result) {
        if (stack != null && stack.getItem() instanceof IronElectrode) {
            result.setReturnValue(IronElectrode.getDefinition());
        }
    }

    @Inject(method = "damageElectrode", at = @At("HEAD"), cancellable = true)
    private void damageIronElectrode(ItemStack stack, int damage, CallbackInfoReturnable<Boolean> result) {
        if (stack != null && stack.getItem() instanceof IronElectrode) {
            result.setReturnValue(IronElectrode.damage(stack, damage));
        }
    }
}
