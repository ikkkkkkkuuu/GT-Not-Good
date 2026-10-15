package com.xyp.gtnotgood.client.radialmenu.core.animation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The opening animation's timing.
 *
 * <p>
 * Worth pinning down because the part that can go wrong is invisible in a screenshot: a stagger where the last
 * sector never quite arrives, or one that finishes in a different order than it started.
 */
public class RevealTimingTest {

    private static final double DURATION = 0.2;

    private static double at(RevealAnimation animation, int slot, double elapsed) {
        return RevealTiming.slotProgress(animation, slot, 8, elapsed, DURATION);
    }

    @Test
    public void withoutAnAnimationEverythingIsAlreadyThere() {
        assertEquals(1.0, at(RevealAnimation.NONE, 3, 0.0), 0.0);
        assertEquals("a duration of nothing is the same as no animation", 1.0,
            RevealTiming.slotProgress(RevealAnimation.ZOOM, 3, 8, 0.0, 0.0), 0.0);
    }

    @Test
    public void anUnstaggeredAnimationMovesEverySectorTogether() {
        assertEquals(at(RevealAnimation.ZOOM, 0, 0.1), at(RevealAnimation.ZOOM, 5, 0.1), 1e-9);
        assertEquals(0.5, at(RevealAnimation.ZOOM, 0, 0.1), 1e-9);
    }

    @Test
    public void aStaggerHandsOffFromOneSectorToTheNext() {
        // Halfway through, the first sector is well along and the last has only just started.
        double first = at(RevealAnimation.STAGGER_CW, 0, 0.1);
        double last = at(RevealAnimation.STAGGER_CW, 7, 0.1);

        assertTrue("the first sector leads", first > last);
        assertEquals("the last one is only beginning", 0.0, last, 1e-9);
    }

    @Test
    public void everySectorArrivesTogetherHoweverItStarted() {
        // The stagger spends part of the time queueing, not extra time: a wheel is finished when its duration is up,
        // whichever way it was revealed.
        for (RevealAnimation animation : RevealAnimation.values()) {
            for (int slot = 0; slot < 8; slot++) {
                assertEquals(animation + " slot " + slot, 1.0,
                    RevealTiming.slotProgress(animation, slot, 8, DURATION, DURATION), 0.0);
            }
        }
    }

    @Test
    public void bothWaysAtOnceIsSymmetric() {
        // Sectors either side of the top are the same distance along, and the bottom is last.
        assertEquals(at(RevealAnimation.STAGGER_BOTH, 1, 0.1), at(RevealAnimation.STAGGER_BOTH, 7, 0.1), 1e-9);
        assertEquals(at(RevealAnimation.STAGGER_BOTH, 3, 0.1), at(RevealAnimation.STAGGER_BOTH, 5, 0.1), 1e-9);
        assertTrue(at(RevealAnimation.STAGGER_BOTH, 0, 0.1) > at(RevealAnimation.STAGGER_BOTH, 4, 0.1));
    }

    @Test
    public void anticlockwiseIsTheMirrorOfClockwise() {
        assertEquals(at(RevealAnimation.STAGGER_CW, 1, 0.1), at(RevealAnimation.STAGGER_CCW, 7, 0.1), 1e-9);
        assertEquals(at(RevealAnimation.STAGGER_CW, 0, 0.1), at(RevealAnimation.STAGGER_CCW, 0, 0.1), 1e-9);
    }

    @Test
    public void easingStartsFastAndSettles() {
        assertEquals(0.0, RevealTiming.easeOutQuint(0.0), 1e-9);
        assertEquals(1.0, RevealTiming.easeOutQuint(1.0), 1e-9);
        assertTrue("half the time is most of the way there", RevealTiming.easeOutQuint(0.5) > 0.9);
        assertEquals("clamped rather than overshooting", 1.0, RevealTiming.easeOutQuint(1.4), 1e-9);
    }

    @Test
    public void approachMovesBothWaysAndStops() {
        assertEquals(0.3, RevealTiming.approach(0.0, 1.0, 0.3), 1e-9);
        assertEquals("never past the target", 1.0, RevealTiming.approach(0.9, 1.0, 0.3), 1e-9);
        assertEquals(0.7, RevealTiming.approach(1.0, 0.0, 0.3), 1e-9);
        assertEquals(0.0, RevealTiming.approach(0.2, 0.0, 0.3), 1e-9);
    }
}
