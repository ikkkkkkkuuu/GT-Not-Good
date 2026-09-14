package com.xyp.gtnotgood.common.compat;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/** Covers ambiguous-input recipes, fluid units and scaled processing patterns without launching Minecraft. */
public class CircuitPatternQuantitiesTest {

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
