package com.xyp.gtnotgood.mixins.late.AppliedEnergistics;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

/**
 * Reuses tree ranks across inventory refreshes without caching the comparator's name, enchantment or count tie-breaks.
 * Tree mutations invalidate all ranks, including wildcard matches and previously unknown item variants.
 */
@Mixin(targets = "appeng.util.InvTweakSortingModule$InvTweaksItemTree", remap = false)
public abstract class InvTweaksOrderCacheMixin {

    @Unique
    private Map<String, Int2IntOpenHashMap> itemOrderCache;

    @Inject(method = "getItemOrder", at = @At("HEAD"), cancellable = true)
    private void readCachedOrder(String id, int damage, CallbackInfoReturnable<Integer> cir) {
        if (itemOrderCache == null || id == null) return;
        Int2IntOpenHashMap orders = itemOrderCache.get(id);
        if (orders != null && orders.containsKey(damage)) cir.setReturnValue(orders.get(damage));
    }

    @Inject(method = "getItemOrder", at = @At("RETURN"))
    private void cacheOrder(String id, int damage, CallbackInfoReturnable<Integer> cir) {
        if (id == null) return;
        if (itemOrderCache == null) itemOrderCache = new HashMap<>();
        itemOrderCache.computeIfAbsent(id, ignored -> new Int2IntOpenHashMap())
            .put(damage, cir.getReturnValueI());
    }

    @Inject(method = { "reset", "addItem" }, at = @At("HEAD"))
    private void invalidateOrders(CallbackInfo ci) {
        if (itemOrderCache != null) itemOrderCache.clear();
    }
}
