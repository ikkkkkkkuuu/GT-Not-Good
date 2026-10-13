// SPDX-License-Identifier: LGPL-3.0-only

package com.xyp.gtnotgood.common.wirelessfrequency;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.xyp.gtnotgood.common.wirelessfrequency.PhysicalClusterTracker.Cluster;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessChannelManager.Address;
import com.xyp.gtnotgood.common.wirelessfrequency.WirelessChannelManager.Entrance;

import appeng.api.networking.IGridNode;

public class WirelessEntranceIndex {

    private final PhysicalClusterTracker<IGridNode, Address> tracker;
    private final Map<Cluster<IGridNode, Address>, List<Entrance>> byCluster = new IdentityHashMap<>();
    private final Map<IGridNode, List<Entrance>> byNode = new IdentityHashMap<>();
    private List<Entrance> entrances;
    private List<Cluster<IGridNode, Address>> clusters;

    WirelessEntranceIndex(PhysicalClusterTracker<IGridNode, Address> tracker) {
        this.tracker = tracker;
    }

    List<Entrance> forCluster(Cluster<IGridNode, Address> cluster) {
        refresh();
        return byCluster.getOrDefault(cluster, WirelessCollections.listOf());
    }

    List<Entrance> atNode(IGridNode node) {
        refresh();
        return byNode.getOrDefault(node, WirelessCollections.listOf());
    }

    private void refresh() {
        List<Entrance> currentEntrances = WirelessChannelManager.entrances();
        List<Cluster<IGridNode, Address>> currentClusters = tracker.clusters();
        if (entrances == currentEntrances && clusters == currentClusters) return;
        byCluster.clear();
        byNode.clear();
        for (Entrance entrance : currentEntrances) {
            byNode.computeIfAbsent(entrance.targetNode(), ignored -> new ArrayList<>()).add(entrance);
            Cluster<IGridNode, Address> cluster = tracker.clusterOf(entrance.targetNode());
            if (cluster != null) byCluster.computeIfAbsent(cluster, ignored -> new ArrayList<>()).add(entrance);
        }
        byCluster.replaceAll((cluster, entries) -> WirelessCollections.copyList(entries));
        byNode.replaceAll((node, entries) -> WirelessCollections.copyList(entries));
        entrances = currentEntrances;
        clusters = currentClusters;
    }
}
