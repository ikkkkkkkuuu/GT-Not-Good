package com.xyp.gtnotgood.mixins.late.gregtech;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.xyp.gtnotgood.utils.machine.ApiaryDropScan;

import gregtech.api.recipe.check.CheckRecipeResult;
import kubatech.tileentity.gregtech.multiblock.MTEMegaIndustrialApiary;

@Mixin(value = MTEMegaIndustrialApiary.class, remap = false)
public abstract class MegaApiaryDropScanScopeMixin {

    @WrapMethod(method = "checkProcessing")
    private CheckRecipeResult gtng$scopeDropScans(Operation<CheckRecipeResult> original) {
        MTEMegaIndustrialApiary machine = (MTEMegaIndustrialApiary) (Object) this;
        try (ApiaryDropScan.Scope scope = ApiaryDropScan.begin(machine.dropProgress)) {
            return original.call();
        }
    }
}
