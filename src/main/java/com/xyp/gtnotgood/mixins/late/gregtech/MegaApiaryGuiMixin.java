package com.xyp.gtnotgood.mixins.late.gregtech;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.common.compat.ApiaryStorageRevision;

import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import kubatech.api.helpers.GTHelper;
import kubatech.gui.modularui2.MTEMegaIndustrialApiaryGui;
import kubatech.tileentity.gregtech.multiblock.MTEMegaIndustrialApiary;

@Mixin(value = MTEMegaIndustrialApiaryGui.class, remap = false)
public abstract class MegaApiaryGuiMixin extends MTEMultiBlockBaseGui<MTEMegaIndustrialApiary> {

    @Unique
    private List<GTHelper.StackableItemSlot> aggregatedBees;

    @Unique
    private long aggregatedBeeRevision;

    @Unique
    private int aggregatedBeeCount;

    protected MegaApiaryGuiMixin(MTEMegaIndustrialApiary multiblock) {
        super(multiblock);
    }

    @Shadow
    protected abstract List<GTHelper.StackableItemSlot> buildAggregatedBeeList();

    @Inject(method = "getFullAggregatedBeeList", at = @At("HEAD"), cancellable = true, require = 1)
    private void reuseAggregatedBees(CallbackInfoReturnable<List<GTHelper.StackableItemSlot>> cir) {
        long revision = ((ApiaryStorageRevision) multiblock).getBeeStorageRevision();
        int count = multiblock.mStorage.size();
        if (aggregatedBees == null || aggregatedBeeRevision != revision || aggregatedBeeCount != count) {
            aggregatedBees = buildAggregatedBeeList();
            aggregatedBeeRevision = revision;
            aggregatedBeeCount = count;
        }
        cir.setReturnValue(aggregatedBees);
    }

    @Inject(method = "invalidateBeeListCache", at = @At("HEAD"), require = 1)
    private void invalidateAggregatedBees(CallbackInfo ci) {
        aggregatedBees = null;
    }
}
