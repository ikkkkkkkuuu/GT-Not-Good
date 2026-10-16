package com.xyp.gtnotgood.client.journeymap;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import com.xyp.gtnotgood.client.journeymap.JourneyMapLanding.Result;
import com.xyp.gtnotgood.client.journeymap.JourneyMapLanding.Status;

public class JourneyMapLandingTest {

    @Test
    public void unloadedTerrainStaysUnknownWithoutReadingBlocks() {
        Column column = new Column(256, 70);
        column.loaded = false;
        assertResult(Status.Unknown, 70, JourneyMapLanding.resolve(column, 70, false));
        assertEquals(0, column.reads);
        assertResult(Status.Unknown, 254, JourneyMapLanding.resolve(null, 999, false));
        assertResult(Status.Unknown, 1, JourneyMapLanding.resolve(null, -12, true));
    }

    @Test
    public void surfaceIgnoresThePlayersHeight() {
        Column column = new Column(256, 65).floor(20).floor(64);
        assertResult(Status.Safe, 65, JourneyMapLanding.resolve(column, 21, false));
    }

    @Test
    public void surfaceCorrectsHeightMapInBothDirections() {
        Column raised = new Column(256, 64).floor(69);
        assertResult(Status.Safe, 70, JourneyMapLanding.resolve(raised, 50, false));
        Column lowered = new Column(256, 70).floor(63);
        assertResult(Status.Safe, 64, JourneyMapLanding.resolve(lowered, 50, false));
    }

    @Test
    public void caveKeepsPreferredLevelAndUsesTheNearestReachableFloor() {
        Column exact = new Column(256, 81).floor(24).floor(80);
        assertResult(Status.Safe, 25, JourneyMapLanding.resolve(exact, 25, true));
        Column nearest = new Column(256, 81).floor(20).floor(27).floor(80);
        assertResult(Status.Safe, 28, JourneyMapLanding.resolve(nearest, 25, true));
    }

    @Test
    public void caveDoesNotSubstituteAnUnrelatedSurfaceForMissingSpace() {
        Column column = new Column(256, 81).floor(80);
        assertResult(Status.Blocked, 25, JourneyMapLanding.resolve(column, 25, true));
        Column edge = new Column(256, 81).floor(40).floor(80);
        assertResult(Status.Safe, 41, JourneyMapLanding.resolve(edge, 25, true));
    }

    @Test
    public void hazardousGroundAndObstructedBodiesAreRejected() {
        Column hazardous = new Column(256, 65).hazard(64);
        assertResult(Status.Blocked, 65, JourneyMapLanding.resolve(hazardous, 65, false));
        Column feet = new Column(256, 65).floor(64).obstruction(65);
        assertResult(Status.Blocked, 65, JourneyMapLanding.resolve(feet, 65, false));
        Column head = new Column(256, 65).floor(64).obstruction(66);
        assertResult(Status.Blocked, 65, JourneyMapLanding.resolve(head, 65, false));
    }

    @Test
    public void unsupportedAirDoesNotBecomeATeleportDestination() {
        assertResult(Status.Blocked, 70, JourneyMapLanding.resolve(new Column(256, 70), 70, false));
    }

    @Test
    public void worldLimitsReserveTwoBodyBlocksAndARealFloor() {
        Column low = new Column(128, 1).floor(0);
        assertResult(Status.Safe, 1, JourneyMapLanding.resolve(low, -200, true));
        Column high = new Column(128, 127).floor(125);
        assertResult(Status.Safe, 126, JourneyMapLanding.resolve(high, 400, true));
        Column ceiling = new Column(128, 128).floor(126);
        assertResult(Status.Blocked, 126, JourneyMapLanding.resolve(ceiling, 400, false));
        assertResult(Status.Blocked, 70, JourneyMapLanding.resolve(null, 30_000_000, 0, 70, false));
        assertResult(Status.Blocked, 70, JourneyMapLanding.resolve(null, 0, -30_000_001, 70, false));
    }

    private static void assertResult(Status status, int y, Result result) {
        assertEquals(status, result.status);
        assertEquals(y, result.y);
    }

    private static final class Column implements JourneyMapLanding.Column {

        private final int height;
        private final int surface;
        private final boolean[] supports = new boolean[256];
        private final boolean[] clear = new boolean[256];
        private boolean loaded = true;
        private int reads;

        private Column(int height, int surface) {
            this.height = height;
            this.surface = surface;
            Arrays.fill(clear, true);
        }

        private Column floor(int y) {
            supports[y] = true;
            clear[y] = false;
            return this;
        }

        private Column hazard(int y) {
            clear[y] = false;
            return this;
        }

        private Column obstruction(int y) {
            clear[y] = false;
            return this;
        }

        @Override
        public int height() {
            return height;
        }

        @Override
        public int surfaceY() {
            return surface;
        }

        @Override
        public boolean loaded() {
            return loaded;
        }

        @Override
        public boolean supports(int y) {
            reads++;
            return supports[y];
        }

        @Override
        public boolean clear(int y) {
            reads++;
            return clear[y];
        }
    }
}
