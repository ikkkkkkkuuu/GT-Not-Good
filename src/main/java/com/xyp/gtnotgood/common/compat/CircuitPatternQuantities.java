package com.xyp.gtnotgood.common.compat;

import java.util.Map;

/** Compares complete processing-pattern quantities without rounding or overflowing batch sizes. */
public final class CircuitPatternQuantities {

    private CircuitPatternQuantities() {}

    /**
     * Recognizes duplicate recipe registrations independently of recipe object identity or ingredient order.
     * A scaled recipe is not an exact duplicate: one operation must consume and produce identical quantities.
     *
     * @param selected    complete input/output quantities of the selected recipe
     * @param runnable    complete input/output quantities of the recipe returned by GT or previously delivered
     * @param sameCircuit whether both recipes require the same circuit, including both requiring none
     * @return whether the recipes describe the same operation
     */
    public static <K> boolean sameRecipe(Map<K, Long> selected, Map<K, Long> runnable, boolean sameCircuit) {
        return sameCircuit && batches(selected, runnable) == 1;
    }

    /**
     * Requires exactly the same ingredients and a single positive integer multiplier for every quantity.
     * Inputs and outputs must have distinct keys, so an incorrect output yield cannot match a recipe.
     *
     * @param recipe  quantities for one machine operation
     * @param pattern quantities encoded in the processing pattern
     * @return operation count, or zero for missing, extra, fractional or inconsistent quantities
     */
    public static <K> long batches(Map<K, Long> recipe, Map<K, Long> pattern) {
        if (recipe.isEmpty() || !recipe.keySet()
            .equals(pattern.keySet())) return 0;
        return requestedOutputBatches(recipe, pattern);
    }

    /**
     * Matches only outputs advertised to AE, allowing entire unwanted byproducts to be omitted from the pattern.
     * Every advertised output must still have the exact yield for the same positive integer operation count.
     * Callers must separately verify all consumed inputs against that count. Full recipe identity comparisons
     * must continue using {@link #batches}, so omitted outputs cannot hide a different runnable recipe.
     *
     * @param recipe  complete deterministic outputs for one machine operation
     * @param pattern nonempty subset of outputs advertised by the processing pattern
     * @return operation count, or zero for unknown, fractional or inconsistent advertised outputs
     */
    public static <K> long requestedOutputBatches(Map<K, Long> recipe, Map<K, Long> pattern) {
        if (pattern.isEmpty()) return 0;
        long batches = 0;
        for (Map.Entry<K, Long> entry : pattern.entrySet()) {
            Long unit = recipe.get(entry.getKey());
            if (unit == null) return 0;
            long total = entry.getValue();
            if (unit <= 0 || total <= 0 || total % unit != 0) return 0;
            long count = total / unit;
            if (batches != 0 && count != batches) return 0;
            batches = count;
        }
        return batches;
    }
}
