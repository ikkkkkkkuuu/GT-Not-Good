package com.xyp.gtnotgood.ae2thing.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WirelessRangeTest {

    @Test
    public void distantAccessPointsCannotWrapInsideRange() {
        assertFalse(WirelessRange.contains(0, 64, 0, 50000, 64, 0, 32));
        assertFalse(WirelessRange.contains(0, 64, 0, -50000, 64, 0, 32));
        assertFalse(WirelessRange.contains(0, 64, 0, 32768, 64, 32768, 32));
    }

    @Test
    public void subtractionAndThreeAxisSumRemainSafeAtCoordinateExtremes() {
        assertFalse(WirelessRange.contains(Integer.MIN_VALUE, 0, 0, Integer.MAX_VALUE, 0, 0, 32));
        assertFalse(WirelessRange.contains(-30000000, 255, -30000000, 30000000, 0, 30000000, 32));
        assertTrue(WirelessRange.contains(30000000, 0, -30000000, 30000000, 0, -30000000, 0));
    }

    @Test
    public void keepsInclusiveSphericalRangeBoundary() {
        assertTrue(WirelessRange.contains(10, 20, 30, 13, 24, 30, 5));
        assertFalse(WirelessRange.contains(10, 20, 30, 13, 24, 31, 5));
        assertTrue(WirelessRange.contains(10, 20, 30, 10, 20, 30, 0));
        assertFalse(WirelessRange.contains(10, 20, 30, 11, 20, 30, 0));
        assertFalse(WirelessRange.contains(10, 20, 30, 10, 20, 30, -1));
    }
}
