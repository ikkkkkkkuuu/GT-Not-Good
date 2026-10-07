package com.xyp.gtnotgood.mixins.late.appliedenergistics;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.xyp.gtnotgood.ae2thing.quickterminal.client.ItemSortNameCache;

import appeng.api.storage.data.IAEStack;
import appeng.util.ItemSorters;

@Mixin(value = ItemSorters.class, remap = false)
public abstract class ItemSortersNameCacheMixin {

    @Inject(method = "getSortName", at = @At("HEAD"), cancellable = true)
    private static void readCachedName(IAEStack<?> stack, CallbackInfoReturnable<String> cir) {
        String name = ItemSortNameCache.get(stack);
        if (name != null) cir.setReturnValue(name);
    }

    @Inject(method = "getSortName", at = @At("RETURN"))
    private static void cacheName(IAEStack<?> stack, CallbackInfoReturnable<String> cir) {
        ItemSortNameCache.put(stack, cir.getReturnValue());
    }
}
