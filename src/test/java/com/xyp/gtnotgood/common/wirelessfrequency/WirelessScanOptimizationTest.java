package com.xyp.gtnotgood.common.wirelessfrequency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;

import com.xyp.gtnotgood.common.wirelessfrequency.PhysicalClusterTracker.Topology;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessClusterManager.ScanSchedule;

public class WirelessScanOptimizationTest {

    @Test
    public void unchangedTopologyWaitsThirtySecondsBetweenFallbackScans() {
        ScanSchedule schedule = new ScanSchedule();
        assertTrue(schedule.pending());
        schedule.beginScan();
        assertFalse(schedule.pending());
        for (int tick = 1; tick < 600; tick++) assertFalse(schedule.tick());
        assertTrue(schedule.tick());
    }

    @Test
    public void topologyChangesBypassTheFallbackDelay() {
        ScanSchedule schedule = new ScanSchedule();
        schedule.beginScan();
        schedule.invalidate();
        assertTrue(schedule.pending());
        assertTrue(schedule.tick());
        schedule.beginScan();
        assertFalse(schedule.pending());
        assertFalse(schedule.tick());
    }

    @Test
    public void unreadySavedNodesRetryWithinOneSecond() {
        ScanSchedule schedule = new ScanSchedule();
        schedule.beginScan();
        schedule.retrySoon();
        for (int tick = 1; tick < 20; tick++) assertFalse(schedule.tick());
        assertTrue(schedule.tick());
    }

    @Test
    public void repeatedRetriesKeepTheEarlierDeadline() {
        ScanSchedule schedule = new ScanSchedule();
        schedule.beginScan();
        schedule.retrySoon();
        for (int tick = 0; tick < 5; tick++) assertFalse(schedule.tick());
        schedule.retrySoon();
        for (int tick = 1; tick < 15; tick++) assertFalse(schedule.tick());
        assertTrue(schedule.tick());
    }

    @Test
    public void unrelatedRemovalDoesNotReportAChangedBinding() {
        Object node = new Object();
        PhysicalClusterTracker<Object, String> tracker = new PhysicalClusterTracker<>(10);
        tracker.refresh(singleNode(node), Collections.singleton(node), ignored -> Collections.singleton("source"),
            source -> source);
        assertFalse(tracker.forget(ignored -> false));
        assertTrue(tracker.forget(removed -> removed == node));
        assertFalse(tracker.forget(removed -> removed == node));
    }

    @Test
    public void refreshedMembershipKeepsThePreviousSnapshotStable() {
        Object node = new Object();
        PhysicalClusterTracker<Object, String> tracker = new PhysicalClusterTracker<>(10);
        tracker.refresh(singleNode(node), Collections.singleton(node), ignored -> Collections.singleton("source"),
            source -> source);
        var previous = tracker.clusterOf(node);
        tracker.refresh(singleNode(node), Collections.singleton(node), ignored -> Collections.emptySet(),
            source -> source);
        assertNotSame(previous, tracker.clusterOf(node));
        assertEquals(Collections.singleton(node), previous.nodes());
        assertEquals(Collections.singleton("source"), previous.frequencies());
    }

    private static Topology<Object> singleNode(Object expected) {
        return new Topology<Object>() {

            @Override
            public boolean isLive(Object node) {
                return node == expected;
            }

            @Override
            public Iterable<Object> neighbours(Object node) {
                return Collections.emptyList();
            }

            @Override
            public boolean blocksWireless(Object node) {
                return false;
            }
        };
    }
}
