package com.xyp.gtnotgood.common.compat;

import java.util.Map;

/** Compares complete processing-pattern quantities without rounding or overflowing batch sizes. */
public final class CircuitPatternQuantities {

    private CircuitPatternQuantities() {}

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
        long batches = 0;
        for (Map.Entry<K, Long> entry : recipe.entrySet()) {
            long unit = entry.getValue();
            long total = pattern.get(entry.getKey());
            if (unit <= 0 || total <= 0 || total % unit != 0) return 0;
            long count = total / unit;
            if (batches != 0 && count != batches) return 0;
            batches = count;
        }
        return batches;
    }
}
