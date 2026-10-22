// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.machines.processing;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import com.xyp.gtnotgood.common.machines.hatch.modular.ExecutionCoreHatch.CoreType;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.VoltageIndex;

public final class HeartGenerationRules {

    public static final BigInteger FUEL_ENERGY = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(81920));
    public static final long MAINTENANCE_EUT = 64L * GTValues.V[VoltageIndex.LuV];
    public static final int BASE_DURATION = 1200;

    private HeartGenerationRules() {}

    public static int duration(CoreType core, int speed, int overclock) {
        long multiplier = (long) Math.max(1, speed) * Math.max(1, overclock);
        int baseDuration = switch (core) {
            case Normal -> BASE_DURATION;
            case Advanced -> BASE_DURATION / 4;
            case Perfect -> 1;
        };
        return (int) Math.max(1, (baseDuration + multiplier - 1) / multiplier);
    }

    public static long maintenance(double multiplier) {
        if (!Double.isFinite(multiplier) || multiplier <= 0 || multiplier > 1)
            throw new IllegalArgumentException("Invalid maintenance multiplier");
        return BigDecimal.valueOf(MAINTENANCE_EUT).multiply(BigDecimal.valueOf(multiplier))
            .setScale(0, RoundingMode.CEILING).longValueExact();
    }

    public static BigInteger energy(int parallels) {
        if (parallels < 1) throw new IllegalArgumentException("Invalid fuel count");
        return FUEL_ENERGY.multiply(BigInteger.valueOf(parallels));
    }

    public static BigInteger cumulative(BigInteger energy, int progress, int duration) {
        if (energy.signum() < 0 || duration < 1 || progress < 0 || progress > duration)
            throw new IllegalArgumentException("Invalid generation progress");
        return energy.multiply(BigInteger.valueOf(progress)).divide(BigInteger.valueOf(duration));
    }
}
