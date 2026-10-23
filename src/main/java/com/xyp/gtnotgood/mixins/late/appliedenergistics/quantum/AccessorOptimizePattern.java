// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtnotgood.mixins.late.appliedenergistics.quantum;

import java.util.HashSet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEStack;

@Mixin(targets = "appeng.container.implementations.ContainerOptimizePatterns$Pattern", remap = false)
public interface AccessorOptimizePattern {

    @Accessor("patternDetails")
    HashSet<ICraftingPatternDetails> gtng$getPatternDetails();

    @Accessor("requestedCrafts")
    long gtng$getRequestedCrafts();

    @Invoker("addCraftingTask")
    void gtng$addCraftingTask(ICraftingPatternDetails pattern, long totalCrafts);

    @Invoker("getCraftAmountForItem")
    long gtng$getCraftAmountForItem(IAEStack<?> stack);

    @Invoker("getPattern")
    ICraftingPatternDetails gtng$getPattern();

    @Invoker("getMaxBitMultiplier")
    int gtng$getMaxBitMultiplier();
}
