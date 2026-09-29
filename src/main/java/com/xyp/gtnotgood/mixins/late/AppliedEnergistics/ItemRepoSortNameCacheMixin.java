package com.xyp.gtnotgood.mixins.late.AppliedEnergistics;

import java.util.ArrayList;
import java.util.Comparator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xyp.gtnotgood.ae2thing.quickterminal.client.ItemSortNameCache;

import appeng.api.storage.data.IAEStack;
import appeng.client.me.ItemRepo;

@Mixin(value = ItemRepo.class, remap = false)
public abstract class ItemRepoSortNameCacheMixin {

    @WrapOperation(
        method = "updateView",
        at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;sort(Ljava/util/Comparator;)V"),
        require = 1)
    private void cacheNamesDuringSort(ArrayList<IAEStack<?>> view, Comparator<? super IAEStack<?>> comparator,
        Operation<Void> original) {
        ItemSortNameCache.duringSort(() -> original.call(view, comparator));
    }
}
