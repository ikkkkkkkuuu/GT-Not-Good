// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtnotgood.common.crafting.quantum.api.networking.crafting;

import java.util.Collection;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.xyp.gtnotgood.common.crafting.quantum.api.crafting.IPatternDetails;
import com.xyp.gtnotgood.common.crafting.quantum.api.stacks.AEKey;
import com.xyp.gtnotgood.common.crafting.quantum.crafting.graph.CraftingGraph;

public interface ICraftingService {

    Collection<IPatternDetails> getCraftingFor(AEKey whatToCraft);

    @Nullable
    AEKey getFuzzyCraftable(AEKey whatToCraft, Predicate<AEKey> filter);

    boolean canEmitFor(AEKey what);

    default long getRecursiveIngredientReserveAmount() {
        return 1;
    }

    default long getGraphCacheRevision() {
        return Long.MIN_VALUE;
    }

    @Nullable
    default CraftingGraph takeCachedGraph(AEKey output) {
        return null;
    }

    @Nullable
    default CraftingGraph peekCachedGraph(AEKey output) {
        return null;
    }

    default void putCachedGraph(AEKey output, CraftingGraph graph) {}
}
