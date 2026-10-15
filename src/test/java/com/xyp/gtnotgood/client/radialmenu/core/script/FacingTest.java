package com.xyp.gtnotgood.client.radialmenu.core.script;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * The yaw a script reads turned into the word it asks about.
 *
 * <p>
 * Worth pinning down because the mapping is counter-intuitive twice over: Minecraft's yaw starts at south rather than
 * north, and it is unbounded, so a player who has spun around a few times carries a number in the hundreds.
 */
public class FacingTest {

    @Test
    public void theFourDirectionsAreVanillasOwn() {
        assertEquals("south", Facing.of(0.0));
        assertEquals("west", Facing.of(90.0));
        assertEquals("north", Facing.of(180.0));
        assertEquals("east", Facing.of(270.0));
    }

    @Test
    public void aQuarterOfTheCompassCountsAsThatDirection() {
        // Each direction owns 45 degrees either side of itself, so standing still and drifting a little does not
        // change the answer a script gets.
        assertEquals("south", Facing.of(44.0));
        assertEquals("west", Facing.of(46.0));
        assertEquals("west", Facing.of(134.0));
        assertEquals("north", Facing.of(136.0));
        assertEquals("south", Facing.of(316.0));
    }

    @Test
    public void spinningAroundDoesNotChangeWhereYouFace() {
        assertEquals("north", Facing.of(180.0 + 360.0 * 3));
        assertEquals("north", Facing.of(180.0 - 360.0 * 5));
        assertEquals("east", Facing.of(-90.0));
        assertEquals("south", Facing.of(-720.0));
    }

    @Test
    public void theNormalisedYawIsAlwaysOnTheCompass() {
        assertEquals(0.0, Facing.normalizeYaw(360.0), 1e-9);
        assertEquals(350.0, Facing.normalizeYaw(-10.0), 1e-9);
        assertEquals(10.0, Facing.normalizeYaw(370.0), 1e-9);
        assertEquals(180.0, Facing.normalizeYaw(-900.0), 1e-9);
    }
}
