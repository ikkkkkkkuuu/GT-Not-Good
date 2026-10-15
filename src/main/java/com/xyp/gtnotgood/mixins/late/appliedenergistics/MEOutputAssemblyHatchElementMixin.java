package com.xyp.gtnotgood.mixins.late.appliedenergistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.common.machines.hatch.me.MEOutputAssembly;

import gregtech.api.enums.HatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;

@Mixin(value = HatchElement.class, remap = false)
public abstract class MEOutputAssemblyHatchElementMixin {

    @Inject(method = "mteClasses", at = @At("RETURN"), cancellable = true)
    private void gtnotgood$includeAssemblyHatch(
        CallbackInfoReturnable<List<? extends Class<? extends IMetaTileEntity>>> cir) {
        if ((Object) this != HatchElement.OutputHatch) return;
        List<Class<? extends IMetaTileEntity>> classes = new ArrayList<>(cir.getReturnValue());
        classes.add(MEOutputAssembly.class);
        cir.setReturnValue(Collections.unmodifiableList(classes));
    }
}
