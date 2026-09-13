package com.xyp.gtnotgood.common.flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * GTNH ledger adapter for Flux Networks' ordered transfer cycle. Plugs settle first, then points
 * consume by surge mode and descending user priority, across loaded dimensions. The ordered list
 * is rebuilt only when nodes load/unload or settings change; no world/chunk scan is performed.
 */
public final class FluxTransferScheduler {

    private static final Set<TileFluxConnector> NODES = Collections.newSetFromMap(new WeakHashMap<>());
    private static List<TileFluxConnector> ordered = Collections.emptyList();
    private static boolean dirty;

    static void add(TileFluxConnector tile) {
        NODES.add(tile);
        dirty = true;
    }

    static void remove(TileFluxConnector tile) {
        NODES.remove(tile);
        dirty = true;
    }

    static void changed() {
        dirty = true;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (dirty) {
            ordered = new ArrayList<>(NODES);
            ordered.sort(
                Comparator.comparing(TileFluxConnector::isPlug)
                    .reversed()
                    .thenComparing(
                        Comparator.comparingInt(TileFluxConnector::effectivePriority)
                            .reversed()));
            dirty = false;
        }
        for (TileFluxConnector tile : ordered) {
            if (!tile.isInvalid() && tile.getWorldObj() != null && !tile.getWorldObj().isRemote) tile.updateEntity();
        }
    }
}
