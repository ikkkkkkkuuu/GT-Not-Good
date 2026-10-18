package com.xyp.gtnotgood.mixins.late.gregtech;

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xyp.gtnotgood.common.compat.ApiaryStorageRevision;

import kubatech.tileentity.gregtech.multiblock.MTEMegaIndustrialApiary;

@Mixin(value = MTEMegaIndustrialApiary.class, remap = false)
public abstract class MegaApiaryStorageMixin implements ApiaryStorageRevision {

    @Unique
    private long beeStorageRevision;

    @Unique
    private final Map<ItemStack, String> beeDisplayNames = new IdentityHashMap<>();

    @Unique
    private long beeNameTranslationRevision;

    @Unique
    private int namedBeeCount;

    @Override
    public long getBeeStorageRevision() {
        return beeStorageRevision;
    }

    @Inject(method = "onStorageContentChanged", at = @At("HEAD"), require = 1)
    private void invalidateBeeStorage(boolean ignoreFlowerCheck, CallbackInfo ci) {
        beeStorageRevision++;
        beeDisplayNames.clear();
    }

    @Inject(method = "getInfoData", at = @At("HEAD"), require = 1)
    private void refreshNameTranslations(CallbackInfoReturnable<String[]> cir) {
        long revision = StatCollector.getLastTranslationUpdateTimeInMilliseconds();
        int count = ((MTEMegaIndustrialApiary) (Object) this).mStorage.size();
        if (beeNameTranslationRevision != revision || namedBeeCount != count) {
            beeDisplayNames.clear();
            beeNameTranslationRevision = revision;
            namedBeeCount = count;
        }
    }

    @WrapOperation(
        method = "getInfoData",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;getDisplayName()Ljava/lang/String;",
            remap = true),
        require = 1)
    private String cacheQueenName(ItemStack queen, Operation<String> original) {
        return beeDisplayNames.computeIfAbsent(queen, stack -> original.call(stack));
    }
}
