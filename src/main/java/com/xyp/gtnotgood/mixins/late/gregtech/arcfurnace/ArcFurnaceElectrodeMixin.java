package com.xyp.gtnotgood.mixins.late.gregtech.arcfurnace;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.common.items.electrode.IronElectrode;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import kubatech.loaders.ArcFurnaceElectrode;

@Mixin(value = ArcFurnaceElectrode.class, remap = false)
public abstract class ArcFurnaceElectrodeMixin {

    @Inject(method = "getById", at = @At("HEAD"), cancellable = true)
    private static void getIronElectrode(int id, CallbackInfoReturnable<ArcFurnaceElectrode> result) {
        if (id == IronElectrode.ELECTRODE_ID && IronElectrode.getDefinition() != null) {
            result.setReturnValue(IronElectrode.getDefinition());
        }
    }

    @Inject(method = "getStartupAmperage", at = @At("HEAD"), cancellable = true)
    private void getIronStartupAmperage(CallbackInfoReturnable<Long> result) {
        if ((Object) this == IronElectrode.getDefinition()) {
            result.setReturnValue(1L);
        }
    }

    @Inject(method = "getElectrodeItem", at = @At("HEAD"), cancellable = true)
    private void getIronElectrodeItem(int amount, CallbackInfoReturnable<ItemStack> result) {
        if ((Object) this == IronElectrode.getDefinition()) {
            result.setReturnValue(GTNGItemList.IronElectrode.get(amount));
        }
    }
}
