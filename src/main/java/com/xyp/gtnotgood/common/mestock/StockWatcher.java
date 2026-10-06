package com.xyp.gtnotgood.common.mestock;

import appeng.api.networking.storage.IStackWatcher;
import appeng.api.storage.data.IAEStack;

/** Registers only configured, enabled identities. No wildcard inventory listener or full storage scan. */
final class StockWatcher {

    private final StockHost host;
    private IStackWatcher watcher;

    StockWatcher(StockHost host) {
        this.host = host;
    }

    void bind(IStackWatcher watcher) {
        this.watcher = watcher;
        rebuild();
    }

    void rebuild() {
        if (watcher == null) return;
        watcher.clear();
        StockConfig config = host.stockConfig();
        for (int i = 0; i < host.stockSlots(); i++) {
            IAEStack<?> key = config.key(i);
            if (key != null && config.enabled(i)) watcher.add(key);
        }
    }
}
