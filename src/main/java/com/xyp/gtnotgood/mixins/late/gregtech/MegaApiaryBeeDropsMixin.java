package com.xyp.gtnotgood.mixins.late.gregtech;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xyp.gtnotgood.utils.machine.ApiaryDropScan;

import gregtech.api.util.GTUtility.ItemId;
import kubatech.tileentity.gregtech.multiblock.MTEMegaIndustrialApiary.BeeSimulator;

@Mixin(value = BeeSimulator.class, remap = false)
public abstract class MegaApiaryBeeDropsMixin {

    @Shadow
    List<?> drops;

    @Shadow
    List<?> specialDrops;

    @Unique
    private Set<ItemId> gtng$dropIds;

    @Inject(method = "generate", at = @At("RETURN"))
    private void gtng$cacheGeneratedDropIds(World world, float tier, CallbackInfoReturnable<Boolean> cir) {
        gtng$cacheDropIds();
    }

    @Inject(method = "<init>(Lnet/minecraft/nbt/NBTTagCompound;)V", at = @At("RETURN"))
    private void gtng$cacheLoadedDropIds(NBTTagCompound tag, CallbackInfo ci) {
        gtng$cacheDropIds();
    }

    @Unique
    private void gtng$cacheDropIds() {
        Set<ItemId> ids = new LinkedHashSet<>();
        for (Object drop : drops) ids.add(((MegaApiaryDropAccessor) drop).gtng$getDropId());
        for (Object drop : specialDrops) ids.add(((MegaApiaryDropAccessor) drop).gtng$getDropId());
        gtng$dropIds = ids;
    }

    @WrapOperation(
        method = "getDrops",
        at = @At(value = "INVOKE", target = "Ljava/util/HashMap;entrySet()Ljava/util/Set;"),
        require = 1)
    private Set<Map.Entry<ItemId, Double>> gtng$scanChangedDrops(HashMap<ItemId, Double> progress,
        Operation<Set<Map.Entry<ItemId, Double>>> original) {
        if (gtng$dropIds == null) gtng$cacheDropIds();
        return ApiaryDropScan.entries(progress, gtng$dropIds, original.call(progress));
    }
}
