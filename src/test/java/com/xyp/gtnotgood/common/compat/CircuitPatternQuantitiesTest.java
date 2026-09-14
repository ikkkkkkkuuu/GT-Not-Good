package com.xyp.gtnotgood.common.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/** Covers ambiguous-input recipes, fluid units and scaled processing patterns without launching Minecraft. */
public class CircuitPatternQuantitiesTest {

    @Test
    public void duplicateSteelPlateRegistrationsAreAcceptedForLookupAndRefill() {
        Map<String, Long> selected = quantities("in:steel_ingot", 1, "out:steel_plate", 1);
        Map<String, Long> gtLookup = quantities("out:steel_plate", 1, "in:steel_ingot", 1);
        assertNotSame(selected, gtLookup);
        assertTrue(CircuitPatternQuantities.sameRecipe(selected, gtLookup, true));
        assertTrue(CircuitPatternQuantities.sameRecipe(gtLookup, selected, true));
        assertTrue(
            CircuitRefillPolicy
                .canAccept(true, true, true, CircuitPatternQuantities.sameRecipe(gtLookup, selected, true), true));
    }

    @Test
    public void duplicateDetectionStillRejectsDifferentCircuitOutputAndYield() {
        Map<String, Long> selected = quantities("in:steel_ingot", 1, "out:steel_plate", 1);
        assertFalse(CircuitPatternQuantities.sameRecipe(selected, new HashMap<>(selected), false));
        assertFalse(
            CircuitPatternQuantities.sameRecipe(selected, quantities("in:steel_ingot", 1, "out:steel_rod", 1), true));
        assertFalse(
            CircuitPatternQuantities.sameRecipe(selected, quantities("in:steel_ingot", 1, "out:steel_plate", 2), true));
        assertFalse(
            CircuitPatternQuantities.sameRecipe(selected, quantities("in:steel_ingot", 2, "out:steel_plate", 2), true));
        assertFalse(CircuitPatternQuantities.sameRecipe(selected, new HashMap<>(), true));
    }

    @Test
    public void duplicateDetectionIncludesFluidAmountsAndIngredientIdentity() {
        Map<String, Long> selected = quantities("in:item:nbtA", 1, "in:water", 1000, "out:product", 1);
        assertTrue(CircuitPatternQuantities.sameRecipe(selected, new HashMap<>(selected), true));
        assertFalse(
            CircuitPatternQuantities
                .sameRecipe(selected, quantities("in:item:nbtB", 1, "in:water", 1000, "out:product", 1), true));
        assertFalse(
            CircuitPatternQuantities
                .sameRecipe(selected, quantities("in:item:nbtA", 1, "in:water", 2000, "out:product", 1), true));
    }

    @Test
    public void sameMaterialsWithDifferentOutputsDoNotMatch() {
        Map<String, Long> recipe = quantities("in:metal", 1, "out:plate", 1);
        assertEquals(0, CircuitPatternQuantities.batches(recipe, quantities("in:metal", 1, "out:wire", 1)));
    }

    @Test
    public void itemAndFluidQuantitiesMustScaleTogether() {
        Map<String, Long> recipe = quantities("in:dust", 2, "in:water", 1000, "out:product", 3);
        assertEquals(
            4,
            CircuitPatternQuantities.batches(recipe, quantities("in:dust", 8, "in:water", 4000, "out:product", 12)));
        assertEquals(
            0,
            CircuitPatternQuantities.batches(recipe, quantities("in:dust", 8, "in:water", 1000, "out:product", 12)));
        assertEquals(
            0,
            CircuitPatternQuantities.batches(recipe, quantities("in:dust", 8, "in:water", 4000, "out:product", 3)));
    }

    @Test
    public void missingByproductsAndExtraIngredientsDoNotMatch() {
        Map<String, Long> recipe = quantities("in:ore", 1, "out:metal", 1, "out:slag", 1);
        assertEquals(0, CircuitPatternQuantities.batches(recipe, quantities("in:ore", 1, "out:metal", 1)));
        Map<String, Long> extra = new HashMap<>(recipe);
        extra.put("in:circuit", 1L);
        assertEquals(0, CircuitPatternQuantities.batches(recipe, extra));
    }

    @Test
    public void fractionalZeroAndOverflowSizedBatchesAreHandledWithoutMultiplication() {
        assertEquals(0, CircuitPatternQuantities.batches(quantities("in:ore", 2), quantities("in:ore", 3)));
        assertEquals(0, CircuitPatternQuantities.batches(quantities("in:ore", 1), quantities("in:ore", 0)));
        assertEquals(0, CircuitPatternQuantities.batches(new HashMap<>(), new HashMap<>()));
        assertEquals(
            Long.MAX_VALUE,
            CircuitPatternQuantities.batches(quantities("in:ore", 1), quantities("in:ore", Long.MAX_VALUE)));
    }

    private static Map<String, Long> quantities(Object... entries) {
        Map<String, Long> result = new HashMap<>();
        for (int i = 0; i < entries.length; i += 2)
            result.put((String) entries[i], ((Number) entries[i + 1]).longValue());
        return result;
    }
}
