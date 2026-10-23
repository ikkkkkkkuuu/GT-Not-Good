// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2. If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package com.xyp.gtnotgood.common.crafting.quantum.crafting;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.xyp.gtnotgood.common.crafting.quantum.api.crafting.IPatternDetails;
import com.xyp.gtnotgood.common.crafting.quantum.api.stacks.AEKey;
import com.xyp.gtnotgood.common.crafting.quantum.api.stacks.GenericStack;
import com.xyp.gtnotgood.common.crafting.quantum.api.stacks.KeyCounter;

import it.unimi.dsi.fastutil.objects.Object2LongMap;

@Desugar
public record CraftingPlan(GenericStack finalOutput, long bytes, boolean simulation, KeyCounter usedItems,
    KeyCounter emittedItems, KeyCounter missingItems, Object2LongMap<IPatternDetails> patternTimes,
    @Nullable Map<IPatternDetails, Map<AEKey, KeyCounter>> variantBudgets) {

    public CraftingPlan withMissing(KeyCounter missing) {
        return new CraftingPlan(this.finalOutput, this.bytes, this.simulation, this.usedItems, this.emittedItems,
            missing, this.patternTimes, this.variantBudgets);
    }
}
