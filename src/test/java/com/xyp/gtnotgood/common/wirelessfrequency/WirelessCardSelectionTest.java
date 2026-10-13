package com.xyp.gtnotgood.common.wirelessfrequency;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardSelection.Candidate;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessCardSelection.Selection;

public class WirelessCardSelectionTest {

    @Test
    public void automaticConnectionRequiresOwnedBoundEnabledCard() {
        Object eligible = new Object();
        Selection<Object> selection = WirelessCardSelection
            .select(Arrays.asList(new Candidate<>(new Object(), true, false, true, true),
                new Candidate<>(new Object(), true, true, false, true),
                new Candidate<>(new Object(), true, true, true, false),
                new Candidate<>(eligible, false, true, true, true)), true);

        assertSame(eligible, selection.card());
        assertFalse(selection.ambiguous());
    }

    @Test
    public void heldCardResolvesMultipleEligibleCards() {
        Object held = new Object();
        Object other = new Object();
        Selection<Object> ambiguous = WirelessCardSelection.select(Arrays.asList(
            new Candidate<>(held, false, true, true, true), new Candidate<>(other, false, true, true, true)), true);
        assertNull(ambiguous.card());
        assertTrue(ambiguous.ambiguous());

        Selection<Object> selected = WirelessCardSelection.select(Arrays.asList(
            new Candidate<>(other, false, true, true, true), new Candidate<>(held, true, true, true, true)), true);
        assertSame(held, selected.card());
        assertFalse(selected.ambiguous());
    }

    @Test
    public void manualActionCanSelectOwnedUnboundCard() {
        Object owned = new Object();
        Selection<Object> selection = WirelessCardSelection
            .select(Arrays.asList(new Candidate<>(new Object(), true, false, true, true),
                new Candidate<>(owned, false, true, false, false)), false);

        assertSame(owned, selection.card());
        assertFalse(selection.ambiguous());
    }
}
