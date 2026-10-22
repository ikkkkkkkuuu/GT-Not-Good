// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.recipe.machine;

import java.math.BigInteger;
import java.util.Objects;

import com.github.bsideup.jabel.Desugar;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.VoltageIndex;

public final class LightHunterRecipeRules {

    public static final int BASE_THREADS = 128;
    public static final int MIN_DURATION = 20;
    public static final int STATION_DURATION = 800;
    public static final long STATION_EUT = GTValues.V[VoltageIndex.UXV];
    public static final int MAX_ASTRAL_ARRAYS = 64;
    public static final int COMPRESSED_ARRAY_EQUIVALENT = 1024;
    public static final int THREAD_MULTIPLIER = 2;
    public static final int THREADS_PER_ARRAY = 64 * THREAD_MULTIPLIER;
    public static final int MAX_THREAD_ARRAYS = 64;
    public static final long NORMAL_PARALLEL_QUOTA = (long) Integer.MAX_VALUE * BASE_THREADS;

    private LightHunterRecipeRules() {}

    public static int additionalThreads(int arrayCount) {
        return Math.max(0, Math.min(MAX_THREAD_ARRAYS, arrayCount)) * THREADS_PER_ARRAY;
    }

    public static int totalThreads(int extraThreads) {
        return (int) Math.min(Integer.MAX_VALUE, (long) BASE_THREADS + Math.max(0, extraThreads));
    }

    public static long parallelQuota(int extraThreads) {
        return (long) Integer.MAX_VALUE * totalThreads(extraThreads);
    }

    /** Retains unused quota when fewer than one parallel remains per unsatisfied recipe. */
    public static long[] fairAllocation(long[] wants, long quota) {
        Objects.requireNonNull(wants, "wants");
        if (quota < 0) throw new IllegalArgumentException("Negative parallel quota");
        long[] allocated = new long[wants.length];
        if (wants.length == 0) return allocated;
        long share = quota / wants.length;
        long remaining = quota;
        int[] active = new int[wants.length];
        int activeCount = 0;
        for (int index = 0; index < wants.length; index++) {
            if (wants[index] < 0) throw new IllegalArgumentException("Negative parallel demand");
            allocated[index] = Math.min(wants[index], share);
            remaining -= allocated[index];
            if (wants[index] > allocated[index]) active[activeCount++] = index;
        }
        while (remaining > 0 && activeCount > 0) {
            share = remaining / activeCount;
            if (share == 0) break;
            int nextActiveCount = 0;
            for (int position = 0; position < activeCount; position++) {
                int index = active[position];
                long amount = Math.min(wants[index] - allocated[index], share);
                allocated[index] += amount;
                remaining -= amount;
                if (wants[index] > allocated[index]) active[nextActiveCount++] = index;
            }
            activeCount = nextActiveCount;
        }
        return allocated;
    }

    public static NormalRates normalRates(BigInteger totalEnergy, long maxEUt, int minimumDuration) {
        Objects.requireNonNull(totalEnergy, "totalEnergy");
        if (totalEnergy.signum() < 0 || maxEUt <= 0 || minimumDuration < 1)
            throw new IllegalArgumentException("Invalid batch energy or duration");
        BigInteger power = BigInteger.valueOf(maxEUt);
        if (totalEnergy.compareTo(power.multiply(BigInteger.valueOf(minimumDuration))) <= 0)
            return new NormalRates(totalEnergy.divide(BigInteger.valueOf(minimumDuration)).longValueExact(),
                minimumDuration);
        BigInteger[] duration = totalEnergy.divideAndRemainder(power);
        if (duration[1].shiftLeft(1).compareTo(power) >= 0) duration[0] = duration[0].add(BigInteger.ONE);
        return new NormalRates(maxEUt, duration[0].intValueExact());
    }

    @Desugar
    public record NormalRates(long eut, int duration) {}
}
