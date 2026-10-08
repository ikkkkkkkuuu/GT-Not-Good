package com.xyp.gtnotgood.mixins.late.gregtech;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.xyp.gtnotgood.common.blocks.stockio.StockIOGeneratorBridge;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicGenerator;

/** Native local fuel burns first; an adjacent interface supplies only an otherwise idle burn tick. */
@Mixin(value = MTEBasicGenerator.class, remap = false)
public abstract class BasicGeneratorStockIOMixin {

    @WrapMethod(method = "onPostTick")
    private void gtng$stockIOFuel(IGregTechTileEntity base, long tick, Operation<Void> original) {
        StockIOGeneratorBridge.onPostTick((MTEBasicGenerator) (Object) this, base, tick,
            () -> original.call(base, tick));
    }
}
