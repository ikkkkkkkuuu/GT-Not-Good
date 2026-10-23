// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtnotgood.common.crafting.quantum.crafting.execution;

import com.github.bsideup.jabel.Desugar;
import com.xyp.gtnotgood.common.crafting.quantum.api.crafting.IPatternDetails;
import com.xyp.gtnotgood.common.crafting.quantum.api.stacks.AEKey;

/**
 * Represents a single "unit" of input for a slot in a crafting/processing pattern. How many units of input are required
 * for a pattern is returned by {@link IPatternDetails.IInput#getMultiplier()}.
 */
@Desugar
public record InputTemplate(AEKey key, long amount) {}
