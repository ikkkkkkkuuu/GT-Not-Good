// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure PR #627 at 651396c75d108e6ff5399a44c56c12214ad27d3d.

package com.xyp.gtnotgood.common.wirelessfrequency;

import java.util.List;

import com.github.bsideup.jabel.Desugar;

public class WirelessCardSelection {

    @Desugar
    record Candidate<T> (T card, boolean held, boolean owned, boolean bound, boolean automatic) {}

    @Desugar
    record Selection<T> (T card, boolean ambiguous) {}

    private WirelessCardSelection() {}

    static <T> Selection<T> select(List<Candidate<T>> cards, boolean connecting) {
        T selected = null;
        int count = 0;
        for (Candidate<T> candidate : cards) {
            if (!candidate.owned() || connecting && (!candidate.bound() || !candidate.automatic())) continue;
            if (candidate.held()) return new Selection<>(candidate.card(), false);
            selected = candidate.card();
            count++;
        }
        return new Selection<>(count == 1 ? selected : null, count > 1);
    }
}
