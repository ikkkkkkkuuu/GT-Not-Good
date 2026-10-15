package com.xyp.gtnotgood.client.radialmenu.core.geometry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Placement of several inline rings on one level.
 *
 * <p>
 * The property worth pinning down is not where any one arc lands - that depends on its neighbours - but that no two
 * ever overlap and that none of them changes places with another. An overlap draws two menus over each other; a swap
 * draws a menu outside somebody else's entry, and both look like the mod lost track of the tree.
 */
public class ArcLayoutTest {

    private static final double EPSILON = 1e-6;

    @Test
    public void aLoneArcSitsExactlyWhereItAsked() {
        ArcLayout.Arc[] arcs = ArcLayout.place(new double[] { 90.0 }, new double[] { 60.0 });

        assertEquals(60.0, arcs[0].span, EPSILON);
        assertEquals(90.0, arcs[0].center(), EPSILON);
    }

    @Test
    public void arcsThatDoNotTouchAreLeftAlone() {
        ArcLayout.Arc[] arcs = ArcLayout.place(new double[] { 0.0, 180.0 }, new double[] { 40.0, 40.0 });

        assertEquals(0.0, arcs[0].center(), EPSILON);
        assertEquals(180.0, arcs[1].center(), EPSILON);
    }

    @Test
    public void overlappingNeighboursGiveWayEqually() {
        // Two 60 degree arcs 30 apart overlap by 30, so each moves 15 away from the other.
        ArcLayout.Arc[] arcs = ArcLayout.place(new double[] { 90.0, 120.0 }, new double[] { 60.0, 60.0 });

        assertEquals(75.0, arcs[0].center(), EPSILON);
        assertEquals(135.0, arcs[1].center(), EPSILON);
        assertNoOverlap(arcs);
    }

    @Test
    public void aCrowdStaysInOrderAndStopsOverlapping() {
        double[] centers = { 10.0, 20.0, 30.0, 40.0, 350.0 };
        double[] spans = { 50.0, 50.0, 50.0, 50.0, 50.0 };

        ArcLayout.Arc[] arcs = ArcLayout.place(centers, spans);

        assertNoOverlap(arcs);
        // Round the circle from the one that started at 350, the order it was given is the order it kept.
        assertCyclicOrder(arcs, 4, 0, 1, 2, 3);
    }

    @Test
    public void arcsAcrossTheTopOfTheWheelDoNotOverlapEither() {
        ArcLayout.Arc[] arcs = ArcLayout.place(new double[] { 350.0, 10.0 }, new double[] { 40.0, 40.0 });

        assertNoOverlap(arcs);
    }

    @Test
    public void aRingThatIsTooFullIsSharedOutRatherThanOverlapped() {
        // Five arcs of 120 want 600 degrees between them; they end at 72 each and fill the circle exactly.
        double[] centers = { 0.0, 72.0, 144.0, 216.0, 288.0 };
        double[] spans = { 120.0, 120.0, 120.0, 120.0, 120.0 };

        ArcLayout.Arc[] arcs = ArcLayout.place(centers, spans);

        double total = 0.0;
        for (ArcLayout.Arc arc : arcs) {
            assertEquals(72.0, arc.span, EPSILON);
            total += arc.span;
        }
        assertEquals(360.0, total, EPSILON);
        assertNoOverlap(arcs);
    }

    /**
     * That the arcs are met in the given order walking clockwise.
     *
     * <p>
     * Checked as a cycle rather than by comparing the angles outright: an arc pushed back past the top of the wheel
     * reads as 340 next to a neighbour at 10, and a plain comparison would call that a swap when it is only the
     * place the numbers happen to wrap.
     */
    private static void assertCyclicOrder(ArcLayout.Arc[] arcs, int... expected) {
        for (int i = 0; i < expected.length; i++) {
            ArcLayout.Arc from = arcs[expected[i]];
            ArcLayout.Arc to = arcs[expected[(i + 1) % expected.length]];
            double step = RadialGeometry.normalizeDegrees(to.center() - from.center());
            assertTrue("arc " + expected[i] + " is not followed by " + expected[(i + 1) % expected.length],
                step > 0.0 && step < 180.0);
        }
    }

    /** Every pair, by walking the circle in steps small enough that no arc can be stepped over. */
    private static void assertNoOverlap(ArcLayout.Arc[] arcs) {
        for (int i = 0; i < arcs.length; i++) {
            for (int j = i + 1; j < arcs.length; j++) {
                double separation = RadialGeometry.normalizeDegrees(arcs[j].center() - arcs[i].center());
                double apart = Math.min(separation, 360.0 - separation);
                double required = (arcs[i].span + arcs[j].span) / 2.0;
                assertTrue("arcs " + i + " and " + j + " overlap: " + apart + " apart, " + required + " required",
                    apart >= required - 1e-6);
            }
        }
    }
}
