// SPDX-License-Identifier: LGPL-3.0-only
// GTNG port modifications (c) 2026 GTNG contributors.
// Upstream authors and exact source mappings: META-INF/ae2lt-port/CODE_PORT_NOTES.md
package com.xyp.gtnotgood.common.packaged;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Bounded network-to-server handoff for 1.7.10; prevents world mutations on Netty threads. */
public final class PackagedServerActions {

    private static final ConcurrentLinkedQueue<Runnable> QUEUE = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger SIZE = new AtomicInteger();

    public static void enqueue(Runnable action) {
        if (SIZE.incrementAndGet() > 256) {
            SIZE.decrementAndGet();
            return;
        }
        QUEUE.add(action);
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        for (int i = 0; i < 64; i++) {
            Runnable action = QUEUE.poll();
            if (action == null) break;
            SIZE.decrementAndGet();
            action.run();
        }
    }
}
