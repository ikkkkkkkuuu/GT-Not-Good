// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtnotgood.common.crafting.quantum.api.crafting;

import java.util.List;

import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import com.xyp.gtnotgood.common.crafting.quantum.api.stacks.AEKey;
import com.xyp.gtnotgood.common.crafting.quantum.api.stacks.GenericStack;

public interface IPatternDetails {

    IInput[] getInputs();

    default boolean canSubstitute() {
        return false;
    }

    default GenericStack getPrimaryOutput() {
        return getOutputs().get(0);
    }

    List<GenericStack> getOutputs();

    interface IInput {

        GenericStack[] possibleInputs();

        long getMultiplier();

        boolean isValid(AEKey input, World level);

        @Nullable
        AEKey getRemainingKey(AEKey template);
    }
}
