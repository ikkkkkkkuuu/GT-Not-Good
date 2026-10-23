// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure at 7b17611117a19b7cc2a921e7213b0578f1513504.
package com.xyp.gtnotgood.common.crafting.quantum.api.stacks;

import java.util.Objects;

import com.github.bsideup.jabel.Desugar;

@Desugar
public record GenericStack(AEKey what, long amount) {

    public GenericStack {
        Objects.requireNonNull(what, "what");
    }
}
