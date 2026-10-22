package com.xyp.gtnotgood.common.machines.processing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.UUID;

import org.junit.Test;

import com.xyp.gtnotgood.common.machines.hatch.modular.ExecutionCoreHatch.CoreType;
import com.xyp.gtnotgood.common.wireless.WirelessOutputs;

public class HeartGenerationRulesTest {

    @Test
    public void maintenanceUsesExactlySixtyFourLuvAmps() {
        assertEquals(2097152L, HeartGenerationRules.MAINTENANCE_EUT);
        assertEquals(2097152L, HeartGenerationRules.maintenance(1));
        assertEquals(524288L, HeartGenerationRules.maintenance(0.25));
        assertEquals(1992295L, HeartGenerationRules.maintenance(0.95));
    }

    @Test
    public void coresAndModulesOnlyCompressTheReleaseWindow() {
        assertEquals(1200, HeartGenerationRules.BASE_DURATION);
        assertEquals(1200, HeartGenerationRules.duration(CoreType.Normal, 1, 1));
        assertEquals(300, HeartGenerationRules.duration(CoreType.Advanced, 1, 1));
        assertEquals(1, HeartGenerationRules.duration(CoreType.Perfect, 1, 1));
        assertEquals(600, HeartGenerationRules.duration(CoreType.Normal, 2, 1));
        assertEquals(300, HeartGenerationRules.duration(CoreType.Normal, 1, 4));
        assertEquals(150, HeartGenerationRules.duration(CoreType.Normal, 1, 8));
        assertEquals(75, HeartGenerationRules.duration(CoreType.Normal, 1, 16));
        assertEquals(150, HeartGenerationRules.duration(CoreType.Normal, 2, 4));
        assertEquals(38, HeartGenerationRules.duration(CoreType.Normal, 2, 16));
        assertEquals(150, HeartGenerationRules.duration(CoreType.Advanced, 2, 1));
        assertEquals(75, HeartGenerationRules.duration(CoreType.Advanced, 1, 4));
        assertEquals(38, HeartGenerationRules.duration(CoreType.Advanced, 1, 8));
        assertEquals(19, HeartGenerationRules.duration(CoreType.Advanced, 1, 16));
        assertEquals(38, HeartGenerationRules.duration(CoreType.Advanced, 2, 4));
        assertEquals(10, HeartGenerationRules.duration(CoreType.Advanced, 2, 16));
        assertEquals(1, HeartGenerationRules.duration(CoreType.Normal, 256, 8));
    }

    @Test
    public void everyCoreSpeedAndOverclockCombinationConservesFuelEnergy() {
        BigInteger energy = HeartGenerationRules.energy(7);
        for (CoreType core : CoreType.values())
            for (int speed : new int[] { 1, 2, 3, 256 }) for (int overclock : new int[] { 1, 4, 8, 16 }) {
                int duration = HeartGenerationRules.duration(core, speed, overclock);
                GenerationWork work = new GenerationWork("controller", core, new UUID(1, 2), duration, 7, energy,
                    new WirelessOutputs(null, null));
                BigInteger credited = BigInteger.ZERO;
                for (int tick = 0; tick < duration; tick++) {
                    BigInteger next = work.nextEnergy();
                    assertTrue(next.signum() >= 0);
                    credited = credited.add(next);
                    work.advance();
                    assertEquals(energy.subtract(credited), work.remainingEnergy());
                }
                assertTrue(work.finished());
                assertEquals(energy, credited);
                assertEquals(BigInteger.ZERO, work.nextEnergy());
            }
    }

    @Test
    public void enormousModuleMultipliersNeverOverflowOrProduceSubtickDurations() {
        for (CoreType core : CoreType.values()) {
            assertEquals(1, HeartGenerationRules.duration(core, Integer.MAX_VALUE, Integer.MAX_VALUE));
            assertEquals(1, HeartGenerationRules.duration(core, Integer.MAX_VALUE, 1));
            assertEquals(1, HeartGenerationRules.duration(core, 1, Integer.MAX_VALUE));
        }
    }

    @Test
    public void enormousFuelBatchesNeverPassThroughLong() {
        BigInteger output = HeartGenerationRules.energy(Integer.MAX_VALUE);
        assertEquals(HeartGenerationRules.FUEL_ENERGY.multiply(BigInteger.valueOf(Integer.MAX_VALUE)), output);
        assertTrue(output.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0);
    }

    @Test
    public void reloadingCannotReissueAlreadyReleasedEnergy() {
        int duration = HeartGenerationRules.duration(CoreType.Normal, 1, 1);
        GenerationWork work = new GenerationWork("controller", CoreType.Normal, new UUID(1, 2), duration, 1,
            HeartGenerationRules.FUEL_ENERGY, new WirelessOutputs(null, null));
        BigInteger credited = BigInteger.ZERO;
        for (int tick = 0; tick < 137; tick++) {
            credited = credited.add(work.nextEnergy());
            work.advance();
        }
        GenerationWork restored = GenerationWork.load(work.save());
        assertEquals(137, restored.progress());
        assertEquals(HeartGenerationRules.FUEL_ENERGY.subtract(credited), restored.remainingEnergy());
        while (!restored.finished()) {
            credited = credited.add(restored.nextEnergy());
            restored.advance();
        }
        assertEquals(HeartGenerationRules.FUEL_ENERGY, credited);
        assertEquals(BigInteger.ZERO, restored.nextEnergy());
        assertEquals(BigInteger.ZERO, restored.remainingEnergy());
    }

    @Test
    public void longReleaseWindowsRetainEveryFractionalRemainder() {
        GenerationWork work = new GenerationWork("controller", CoreType.Normal, new UUID(1, 2),
            HeartGenerationRules.duration(CoreType.Normal, 1, 1), 1, BigInteger.valueOf(101),
            new WirelessOutputs(null, null));
        BigInteger credited = BigInteger.ZERO;
        while (!work.finished()) {
            credited = credited.add(work.nextEnergy());
            work.advance();
        }
        assertEquals(BigInteger.valueOf(101), credited);
        assertEquals(BigInteger.ZERO, work.remainingEnergy());
    }
}
