package com.xyp.gtnotgood.common.advancedio;

/** Overflow-safe quantity decisions shared by item and millibucket transfers. */
public final class StockPolicy {

    private StockPolicy() {}

    public static long exportAmount(long current, long target, long budget) {
        return current >= target ? 0 : Math.min(target - current, budget);
    }

    public static long importAmount(long current, long target, boolean listed, boolean regulate, long budget) {
        if (!listed) return Math.min(current, budget);
        return !regulate || current <= target ? 0 : Math.min(current - target, budget);
    }
}
