package com.xyp.gtnotgood.mixins.late.AppliedEnergistics;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.google.common.collect.Ordering;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEStack;
import appeng.me.cache.CraftingGridCache;

/** Keeps alternative recipes when AE's output index would otherwise merge equal-priority patterns. */
@Mixin(value = CraftingGridCache.class, remap = false)
public abstract class CraftingPatternAlternativesMixin {

    @WrapOperation(
        method = "setPatternsFromCraftingMethods",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;"),
        require = 1)
    private Object gtng$keepAlternativePatterns(Map<IAEStack<?>, Set<ICraftingPatternDetails>> patterns, Object output,
        Function<IAEStack<?>, Set<ICraftingPatternDetails>> factory, Operation<Object> original) {
        // craftingMethods already groups equal recipes. Priority alone is not recipe identity: dropping a tied
        // chemical-bath recipe can leave only a plate -> molten material -> plate cycle in the planner.
        Function<IAEStack<?>, Set<ICraftingPatternDetails>> alternatives = key -> new TreeSet<>(
            Comparator.comparingInt(ICraftingPatternDetails::getPriority)
                .reversed()
                .thenComparing(Ordering.arbitrary()));
        return original.call(patterns, output, alternatives);
    }
}
