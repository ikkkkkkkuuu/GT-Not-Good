package com.xyp.gtnotgood.common.compat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Regression cases for continuous feeding, circuit switches and missing delivery history after reload. */
public class CircuitRefillPolicyTest {

    @Test
    public void sameRecipeCanRefillDuringProcessingWithOrWithoutQueuedInputs() {
        assertTrue(CircuitRefillPolicy.canAccept(true, true, true, true, true));
        assertTrue(CircuitRefillPolicy.canAccept(true, false, true, true, true));
        assertTrue(CircuitRefillPolicy.canAccept(false, true, true, true, true));
    }

    @Test
    public void circuitChangesWaitForBothProcessingAndBufferedInputs() {
        assertFalse(CircuitRefillPolicy.canAccept(true, false, false, true, true));
        assertFalse(CircuitRefillPolicy.canAccept(false, true, false, true, true));
        assertFalse(CircuitRefillPolicy.canAccept(true, true, false, true, true));
        assertTrue(CircuitRefillPolicy.canAccept(false, false, false, false, true));
    }

    @Test
    public void sameCircuitDoesNotPermitMixingDifferentRecipes() {
        assertFalse(CircuitRefillPolicy.canAccept(true, false, true, false, true));
        assertFalse(CircuitRefillPolicy.canAccept(false, true, true, true, false));
        assertFalse(CircuitRefillPolicy.canAccept(true, true, true, true, false));
    }

    @Test
    public void reloadWaitsForUnknownRunningBatchButCanRecoverIdleBufferedRecipe() {
        assertFalse(CircuitRefillPolicy.canAccept(true, false, true, false, true));
        assertFalse(CircuitRefillPolicy.canAccept(true, true, true, false, true));
        assertTrue(CircuitRefillPolicy.canAccept(false, true, true, false, true));
        assertTrue(CircuitRefillPolicy.canAccept(false, false, true, false, true));
    }
}
